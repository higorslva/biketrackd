package com.biketrackd.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pedal_history")
data class PedalSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val totalDistance: Float,
    val maxSpeed: Float,
    val avgSpeed: Float,
    val durationSeconds: Long,
    val trailData: String? = null,
)
