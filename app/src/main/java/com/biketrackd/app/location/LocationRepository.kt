package com.biketrackd.app.location

import android.content.Context
import android.location.Location
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object LocationRepository {

    private const val EMA_ALPHA = 0.20f
    private const val SPEED_THRESHOLD = 1.5f
    private const val TRAIL_MIN_DISTANCE = 10f
    private const val MIN_MOVING_SPEED = 3.0f
    private const val PREFS_NAME = "biketrackd_prefs"
    private const val KEY_TOTAL_ODOMETER = "total_odometer_meters"
    private const val KEY_MAX_SPEED_ARC = "max_speed_arc"

    private val _state = MutableStateFlow(LocationState())
    val state: StateFlow<LocationState> = _state.asStateFlow()

    private val _maxSpeedArc = MutableStateFlow(80f)
    val maxSpeedArc: StateFlow<Float> = _maxSpeedArc.asStateFlow()

    private val _trailPoints = mutableListOf<Pair<Double, Double>>()
    val trailPoints: List<Pair<Double, Double>> get() = _trailPoints.toList()

    private var previousLocation: Location? = null
    private var previousAltitude: Double = 0.0
    private var previousDistanceForAltitude: Float = 0f
    private var sessionStartTime: Long = 0L
    private var smoothedSpeed = 0f
    private var lastMovingSecond = 0L
    private var contextForPrefs: Context? = null
    private var lastSlowUpdate = 0L
    private var originSet = false

    fun init(context: Context) {
        contextForPrefs = context.applicationContext
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val total = prefs.getFloat(KEY_TOTAL_ODOMETER, 0f)
        _state.value = _state.value.copy(totalOdometerMeters = total)
        _maxSpeedArc.value = prefs.getFloat(KEY_MAX_SPEED_ARC, 80f)
    }

    fun startSession() {
        _trailPoints.clear()
        previousLocation = null
        sessionStartTime = System.currentTimeMillis()
        lastMovingSecond = 0L
        smoothedSpeed = 0f
        val current = _state.value
        originSet = false
        _state.value = current.copy(
            isSessionActive = true,
            elapsedSeconds = 0L,
            movingSeconds = 0L,
            totalDistanceMeters = 0f,
            maxSpeedKmh = 0f,
            rawSpeedKmh = 0f,
            hasOrigin = false,
        )
    }

    fun stopSession(): SessionSummary {
        val current = _state.value
        _state.value = current.copy(isSessionActive = false)
        val avgSpeed = if (current.elapsedSeconds > 0)
            current.totalDistanceMeters / current.elapsedSeconds * 3.6f else 0f
        return SessionSummary(
            totalDistance = current.totalDistanceMeters,
            maxSpeed = current.maxSpeedKmh,
            avgSpeed = avgSpeed,
            durationSeconds = current.elapsedSeconds,
        )
    }

    fun updateLocation(location: Location) {
        val rawSpeed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !location.hasSpeed()) {
            0f
        } else {
            location.speed * 3.6f
        }
        val current = _state.value

        if (!current.hasFix) {
            smoothedSpeed = rawSpeed
        } else {
            smoothedSpeed = EMA_ALPHA * rawSpeed + (1f - EMA_ALPHA) * smoothedSpeed
        }

        val displayedSpeed = if (smoothedSpeed < SPEED_THRESHOLD) 0f else smoothedSpeed

        val prevLocation = previousLocation
        var distanceDelta = 0f
        prevLocation?.let { prev ->
            distanceDelta = location.distanceTo(prev)
        }
        previousLocation = location

        val altitude = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && location.hasAltitude()) {
            location.altitude
        } else {
            location.altitude
        }

        // Calculate slope
        val slope = if (distanceDelta > 5f) {
            val altDelta = altitude - previousAltitude
            (altDelta / distanceDelta * 100f).toFloat()
        } else {
            current.slope
        }
        previousAltitude = altitude
        previousDistanceForAltitude += distanceDelta

        val bearing = if (rawSpeed > SPEED_THRESHOLD) {
            if (location.hasBearing()) location.bearing
            else if (prevLocation != null) prevLocation.bearingTo(location)
            else current.bearing
        } else {
            current.bearing
        }

        val now = System.currentTimeMillis()
        val isSlowTick = now - lastSlowUpdate >= 30_000L

        var newState = current.copy(
            speedKmh = displayedSpeed,
            rawSpeedKmh = rawSpeed,
            hasFix = true,
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = if (isSlowTick) altitude else current.altitude,
            bearing = bearing,
            slope = if (isSlowTick) slope else current.slope,
        )

        if (isSlowTick) lastSlowUpdate = now

        if (current.isSessionActive) {
            val elapsed = if (sessionStartTime > 0)
                (System.currentTimeMillis() - sessionStartTime) / 1000 else 0L

            var movingSecs = current.movingSeconds
            if (displayedSpeed >= MIN_MOVING_SPEED && lastMovingSecond != elapsed) {
                movingSecs++
                lastMovingSecond = elapsed
            }

            if (_trailPoints.isEmpty() || distanceDelta > TRAIL_MIN_DISTANCE) {
                _trailPoints.add(Pair(location.latitude, location.longitude))
            }

            newState = newState.copy(
                maxSpeedKmh = maxOf(current.maxSpeedKmh, rawSpeed),
                elapsedSeconds = elapsed,
                movingSeconds = movingSecs,
                totalDistanceMeters = current.totalDistanceMeters + distanceDelta,
            )
        }

        _state.value = newState

        // Save origin on first fix after session starts
        if (current.isSessionActive && !originSet && current.hasFix) {
            originSet = true
            _state.value = _state.value.copy(
                originLatitude = location.latitude,
                originLongitude = location.longitude,
                hasOrigin = true,
            )
        }

        // Update distance to origin
        if (current.isSessionActive && originSet && current.hasFix) {
            val dist = FloatArray(1)
            android.location.Location.distanceBetween(
                current.originLatitude, current.originLongitude,
                location.latitude, location.longitude,
                dist,
            )
            _state.value = _state.value.copy(distanceToOrigin = dist[0])
        }
    }

    fun addToTotalOdometer(distanceMeters: Float) {
        val current = _state.value
        val newTotal = current.totalOdometerMeters + distanceMeters
        _state.value = current.copy(totalOdometerMeters = newTotal)
        contextForPrefs?.let { ctx ->
            ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putFloat(KEY_TOTAL_ODOMETER, newTotal)
                .apply()
        }
    }

    fun trailToJson(): String {
        val coords = _trailPoints.joinToString(",") { (lat, lon) ->
            "[$lat,$lon]"
        }
        return "[$coords]"
    }

    fun resetSession() {
        _trailPoints.clear()
        originSet = false
        _state.value = _state.value.copy(
            isSessionActive = false,
            elapsedSeconds = 0L,
            movingSeconds = 0L,
            totalDistanceMeters = 0f,
            maxSpeedKmh = 0f,
            rawSpeedKmh = 0f,
            resetCount = _state.value.resetCount + 1,
            hasOrigin = false,
            distanceToOrigin = 0f,
        )
        previousLocation = null
        sessionStartTime = 0L
        smoothedSpeed = 0f
    }

    fun setMaxSpeedArc(value: Float) {
        _maxSpeedArc.value = value
        contextForPrefs?.let { ctx ->
            ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putFloat(KEY_MAX_SPEED_ARC, value)
                .apply()
        }
    }
}
