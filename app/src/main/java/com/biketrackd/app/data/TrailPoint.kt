package com.biketrackd.app.data

import org.json.JSONArray

data class TrailPoint(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val timestamp: Long = 0L,
)

object TrailDataParser {

    fun parse(trailData: String?): List<TrailPoint> {
        if (trailData.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(trailData)
            buildList {
                for (i in 0 until arr.length()) {
                    val point = arr.getJSONArray(i)
                    if (point.length() < 2) continue
                    add(
                        TrailPoint(
                            latitude = point.getDouble(0),
                            longitude = point.getDouble(1),
                            altitude = if (point.length() > 2 && !point.isNull(2)) {
                                point.optDouble(2)
                            } else {
                                null
                            },
                            timestamp = if (point.length() > 3 && !point.isNull(3)) {
                                point.optLong(3)
                            } else {
                                0L
                            },
                        ),
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
