package com.biketrackd.app.data

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import java.io.File

object OfflineMapManager {

    private const val MAPS_DIR = "offline_maps"

    private fun getMapsDir(context: Context): File {
        val dir = File(context.filesDir, MAPS_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun listMaps(context: Context): List<OfflineMapInfo> {
        val dir = getMapsDir(context)
        return dir.listFiles { f -> f.extension == "mbtiles" }
            ?.mapNotNull { file -> readMetadata(context, file) }
            ?.sortedByDescending { it.createdAt }
            ?: emptyList()
    }

    fun getMapFile(context: Context, fileName: String): File? {
        val file = File(getMapsDir(context), fileName)
        return if (file.exists()) file else null
    }

    fun deleteMap(context: Context, fileName: String): Boolean {
        val file = File(getMapsDir(context), fileName)
        return if (file.exists()) file.delete() else false
    }

    fun getMapForLocation(context: Context, lat: Double, lon: Double): OfflineMapInfo? {
        return listMaps(context).firstOrNull { info ->
            val d = haversine(lat, lon, info.centerLat, info.centerLon)
            d <= info.radiusKm
        }
    }

    private fun readMetadata(context: Context, file: File): OfflineMapInfo? {
        return try {
            SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                val meta = mutableMapOf<String, String>()
                val cursor: Cursor = db.rawQuery("SELECT name, value FROM metadata", null)
                cursor.use { c ->
                    while (c.moveToNext()) {
                        meta[c.getString(0)] = c.getString(1)
                    }
                }

                val tileCount: Int
                val countCursor = db.rawQuery("SELECT COUNT(*) FROM tiles", null)
                countCursor.use { c ->
                    tileCount = if (c.moveToFirst()) c.getInt(0) else 0
                }

                OfflineMapInfo(
                    name = meta["name"] ?: file.nameWithoutExtension,
                    fileName = file.name,
                    fileSizeBytes = file.length(),
                    tileCount = tileCount,
                    centerLat = meta["centerLat"]?.toDoubleOrNull() ?: 0.0,
                    centerLon = meta["centerLon"]?.toDoubleOrNull() ?: 0.0,
                    radiusKm = meta["radiusKm"]?.toIntOrNull() ?: 0,
                    minZoom = meta["minZoom"]?.toIntOrNull() ?: 0,
                    maxZoom = meta["maxZoom"]?.toIntOrNull() ?: 0,
                    createdAt = meta["createdAt"]?.toLongOrNull() ?: file.lastModified(),
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2).pow(2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2).pow(2)
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }

    private fun Double.pow(exp: Int): Double = Math.pow(this, exp.toDouble())
}
