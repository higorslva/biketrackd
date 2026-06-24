package com.biketrackd.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "maintenance_part",
    foreignKeys = [ForeignKey(
        entity = Bike::class,
        parentColumns = ["id"],
        childColumns = ["bikeId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("bikeId")],
)
data class MaintenancePart(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bikeId: Long,
    val name: String,
    val componentType: String,
    val lifespanKm: Float,
    val usedKm: Float = 0f,
    val installDate: Long = 0L,
    val notes: String = "",
)
