package com.biketrackd.app.location

data class LocationState(
    val speedKmh: Float = 0f,
    val maxSpeedKmh: Float = 0f,
    val hasFix: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val bearing: Float = 0f,
    val elapsedSeconds: Long = 0L,
    val totalDistanceMeters: Float = 0f,
    val resetCount: Int = 0,
    val isSessionActive: Boolean = false,
)

data class SessionSummary(
    val totalDistance: Float,
    val maxSpeed: Float,
    val avgSpeed: Float,
    val durationSeconds: Long,
)
