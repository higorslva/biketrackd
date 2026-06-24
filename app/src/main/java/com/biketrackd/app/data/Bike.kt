package com.biketrackd.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bike")
data class Bike(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val model: String = "",
    val type: String = "",
    val acquisitionDate: Long = 0L,
    val notes: String = "",
    val isDefault: Boolean = false,
)
