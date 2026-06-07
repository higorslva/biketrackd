package com.biketrackd.app.data

data class OfflineMapInfo(
    val name: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val tileCount: Int,
    val centerLat: Double,
    val centerLon: Double,
    val radiusKm: Int,
    val minZoom: Int,
    val maxZoom: Int,
    val createdAt: Long,
) {
    val fileSizeDisplay: String
        get() = when {
            fileSizeBytes < 1_000_000 -> "${fileSizeBytes / 1_000} KB"
            fileSizeBytes < 1_000_000_000 -> "${String.format("%.1f", fileSizeBytes / 1_000_000f)} MB"
            else -> "${String.format("%.1f", fileSizeBytes / 1_000_000_000f)} GB"
        }

    val tileCountDisplay: String
        get() = when {
            tileCount < 1_000 -> "$tileCount"
            tileCount < 1_000_000 -> "${tileCount / 1_000}k"
            else -> "${String.format("%.1f", tileCount / 1_000_000f)}M"
        }
}
