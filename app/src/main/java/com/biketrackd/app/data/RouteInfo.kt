package com.biketrackd.app.data

import org.maplibre.android.geometry.LatLng

data class RouteInfo(
    val distanceMeters: Double,
    val timeSeconds: Double,
    val points: List<LatLng>,
) {
    val distanceDisplay: String
        get() = if (distanceMeters >= 1000)
            String.format("%.1f km", distanceMeters / 1000)
        else
            String.format("%.0f m", distanceMeters)

    val timeDisplay: String
        get() {
            val totalMinutes = (timeSeconds / 60).toInt()
            val h = totalMinutes / 60
            val m = totalMinutes % 60
            return if (h > 0) "${h}h${m}min" else "${m}min"
        }
}
