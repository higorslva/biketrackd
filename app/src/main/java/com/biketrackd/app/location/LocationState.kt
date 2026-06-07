package com.biketrackd.app.location

data class LocationState(
    val speedKmh: Float = 0f,
    val rawSpeedKmh: Float = 0f,
    val maxSpeedKmh: Float = 0f,
    val hasFix: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0,
    val bearing: Float = 0f,
    val slope: Float = 0f,
    val elapsedSeconds: Long = 0L,
    val totalDistanceMeters: Float = 0f,
    val totalOdometerMeters: Float = 0f,
    val movingSeconds: Long = 0L,
    val resetCount: Int = 0,
    val isSessionActive: Boolean = false,
    val originLatitude: Double = 0.0,
    val originLongitude: Double = 0.0,
    val hasOrigin: Boolean = false,
    val distanceToOrigin: Float = 0f,
)

data class SessionSummary(
    val totalDistance: Float,
    val maxSpeed: Float,
    val avgSpeed: Float,
    val durationSeconds: Long,
)
