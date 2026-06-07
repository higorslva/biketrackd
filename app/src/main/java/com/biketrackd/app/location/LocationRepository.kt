package com.biketrackd.app.location

import android.location.Location
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object LocationRepository {

    private const val EMA_ALPHA = 0.20f
    private const val SPEED_THRESHOLD = 1.5f
    private const val TRAIL_MIN_DISTANCE = 10f

    private val _state = MutableStateFlow(LocationState())
    val state: StateFlow<LocationState> = _state.asStateFlow()

    private val _trailPoints = mutableListOf<Pair<Double, Double>>()
    val trailPoints: List<Pair<Double, Double>> get() = _trailPoints.toList()

    private var previousLocation: Location? = null
    private var sessionStartTime: Long = 0L
    private var smoothedSpeed = 0f

    fun startSession() {
        _trailPoints.clear()
        previousLocation = null
        sessionStartTime = System.currentTimeMillis()
        smoothedSpeed = 0f
        val current = _state.value
        _state.value = current.copy(
            isSessionActive = true,
            elapsedSeconds = 0L,
            totalDistanceMeters = 0f,
            maxSpeedKmh = 0f,
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
        val rawSpeed = location.speed * 3.6f
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

        val bearing = if (rawSpeed > SPEED_THRESHOLD) {
            if (location.hasBearing()) location.bearing
            else if (prevLocation != null) prevLocation.bearingTo(location)
            else current.bearing
        } else {
            current.bearing
        }

        var newState = current.copy(
            speedKmh = displayedSpeed,
            hasFix = true,
            latitude = location.latitude,
            longitude = location.longitude,
            bearing = bearing,
        )

        if (current.isSessionActive) {
            val elapsed = if (sessionStartTime > 0)
                (System.currentTimeMillis() - sessionStartTime) / 1000 else 0L

            if (_trailPoints.isEmpty() || distanceDelta > TRAIL_MIN_DISTANCE) {
                _trailPoints.add(Pair(location.latitude, location.longitude))
            }

            newState = newState.copy(
                maxSpeedKmh = maxOf(current.maxSpeedKmh, rawSpeed),
                elapsedSeconds = elapsed,
                totalDistanceMeters = current.totalDistanceMeters + distanceDelta,
            )
        }

        _state.value = newState
    }

    fun trailToJson(): String {
        val coords = _trailPoints.joinToString(",") { (lat, lon) ->
            "[$lat,$lon]"
        }
        return "[$coords]"
    }

    fun resetSession() {
        _trailPoints.clear()
        _state.value = _state.value.copy(
            isSessionActive = false,
            elapsedSeconds = 0L,
            totalDistanceMeters = 0f,
            maxSpeedKmh = 0f,
            resetCount = _state.value.resetCount + 1,
        )
        previousLocation = null
        sessionStartTime = 0L
        smoothedSpeed = 0f
    }
}
