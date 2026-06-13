package com.biketrackd.app.data

import com.biketrackd.app.data.UnitPreferences.UnitSystem

object UnitFormatter {
    fun speedKmhToUnit(kmh: Float, system: UnitSystem): Float =
        if (system == UnitSystem.IMPERIAL) kmh * 0.621371f else kmh

    fun speedUnit(system: UnitSystem): String =
        if (system == UnitSystem.IMPERIAL) "mph" else "km/h"

    fun formatSpeed(kmh: Float, system: UnitSystem): String =
        "${speedKmhToUnit(kmh, system).toInt()} ${speedUnit(system)}"

    fun metersToUnit(m: Float, system: UnitSystem): Float =
        if (system == UnitSystem.IMPERIAL) m * 3.28084f else m

    fun distanceUnit(system: UnitSystem): String =
        if (system == UnitSystem.IMPERIAL) "ft" else "m"

    fun formatAltitude(m: Float, system: UnitSystem): String =
        "${metersToUnit(m, system).toInt()} ${distanceUnit(system)}"

    fun formatLongDistance(m: Float, system: UnitSystem): String {
        val value = if (system == UnitSystem.IMPERIAL) m * 0.000621371f else m / 1000f
        return if (value >= 1f) {
            String.format("%.2f %s", value, if (system == UnitSystem.IMPERIAL) "mi" else "km")
        } else {
            val short = if (system == UnitSystem.IMPERIAL) m * 3.28084f else m
            "${short.toInt()} ${if (system == UnitSystem.IMPERIAL) "ft" else "m"}"
        }
    }

    fun formatTotalDistance(odometerM: Float, system: UnitSystem): String {
        val totalKm = odometerM / 1000f
        val totalValue = if (system == UnitSystem.IMPERIAL) totalKm * 0.621371f else totalKm
        val unit = if (system == UnitSystem.IMPERIAL) "mi" else "km"
        return if (totalValue >= 1f) {
            String.format("%.1f %s", totalValue, unit)
        } else {
            val short = if (system == UnitSystem.IMPERIAL) odometerM * 3.28084f else odometerM
            "${short.toInt()} ${if (system == UnitSystem.IMPERIAL) "ft" else "m"}"
        }
    }

    fun formatCelsius(c: Int, system: UnitSystem): String {
        val value = if (system == UnitSystem.IMPERIAL) (c * 9 / 5) + 32 else c
        val unit = if (system == UnitSystem.IMPERIAL) "\u00B0F" else "\u00B0C"
        return "$value$unit"
    }
}
