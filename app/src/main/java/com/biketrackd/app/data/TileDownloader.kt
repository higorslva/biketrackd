package com.biketrackd.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class DownloadProgress(
    val status: Status = Status.Idle,
    val zoom: Int = 0,
    val current: Int = 0,
    val total: Int = 0,
    val fileName: String = "",
) {
    val fraction: Float get() = if (total > 0) current.toFloat() / total else 0f
    val isActive: Boolean get() = status == Status.Downloading

    enum class Status { Idle, Downloading, Completed, Error }
}

object TileDownloader {

    private val _progress = MutableStateFlow(DownloadProgress())
    val progress: StateFlow<DownloadProgress> = _progress.asStateFlow()

    private const val MIN_ZOOM = 10
    private const val MAX_ZOOM = 14
    private const val TILE_SERVER = "https://tile.openstreetmap.org"
    private const val USER_AGENT = "GPS-OSS/1.0 (offline maps)"
    private const val RATE_LIMIT_MS = 50L
    private const val MAX_RETRIES = 2

    suspend fun download(
        context: Context,
        name: String,
        centerLat: Double,
        centerLon: Double,
        radiusKm: Int,
    ) {
        withContext(Dispatchers.IO) {
            val mapsDir = File(context.filesDir, "offline_maps")
            if (!mapsDir.exists()) mapsDir.mkdirs()

            val fileName = "map_${System.currentTimeMillis()}.mbtiles"
            val file = File(mapsDir, fileName)

            try {
                val bbox = BoundingBox(centerLat, centerLon, radiusKm)
                var totalTiles = 0
                val tilesByZoom = mutableMapOf<Int, List<TileCoord>>()

                for (z in MIN_ZOOM..MAX_ZOOM) {
                    val tiles = bbox.tilesForZoom(z)
                    tilesByZoom[z] = tiles
                    totalTiles += tiles.size
                }

                _progress.value = DownloadProgress(
                    status = DownloadProgress.Status.Downloading,
                    total = totalTiles,
                    fileName = fileName,
                )

                var downloaded = 0
                SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
                    db.execSQL(
                        """CREATE TABLE IF NOT EXISTS tiles (
                            zoom_level INTEGER NOT NULL,
                            tile_column INTEGER NOT NULL,
                            tile_row INTEGER NOT NULL,
                            tile_data BLOB NOT NULL,
                            PRIMARY KEY (zoom_level, tile_column, tile_row)
                        )"""
                    )
                    db.execSQL(
                        """CREATE TABLE IF NOT EXISTS metadata (
                            name TEXT PRIMARY KEY,
                            value TEXT
                        )"""
                    )

                    db.beginTransaction()
                    try {
                        for ((zoom, tiles) in tilesByZoom.toSortedMap()) {
                            for ((x, y) in tiles) {
                                val tmsY = tmsY(zoom, y)
                                val data = downloadTile(zoom, x, tmsY)
                                if (data != null) {
                                    db.execSQL(
                                        "INSERT OR IGNORE INTO tiles VALUES (?, ?, ?, ?)",
                                        arrayOf(zoom, x, y, data),
                                    )
                                }
                                downloaded++
                                _progress.value = _progress.value.copy(
                                    zoom = zoom,
                                    current = downloaded,
                                )
                            }
                        }
                        db.setTransactionSuccessful()
                    } finally {
                        db.endTransaction()
                    }

                    val insertStmt = db.compileStatement(
                        "INSERT OR REPLACE INTO metadata VALUES (?, ?)"
                    )
                    fun putMeta(name: String, value: String) {
                        insertStmt.bindString(1, name)
                        insertStmt.bindString(2, value)
                        insertStmt.executeInsert()
                    }
                    putMeta("name", name)
                    putMeta("format", "png")
                    putMeta("type", "baselayer")
                    putMeta("version", "1.3")
                    putMeta("centerLat", centerLat.toString())
                    putMeta("centerLon", centerLon.toString())
                    putMeta("radiusKm", radiusKm.toString())
                    putMeta("minZoom", MIN_ZOOM.toString())
                    putMeta("maxZoom", MAX_ZOOM.toString())
                    putMeta("createdAt", System.currentTimeMillis().toString())
                }

                _progress.value = DownloadProgress(
                    status = DownloadProgress.Status.Completed,
                    total = downloaded,
                    current = downloaded,
                    fileName = fileName,
                )
            } catch (e: Exception) {
                file.delete()
                _progress.value = DownloadProgress(
                    status = DownloadProgress.Status.Error,
                    fileName = fileName,
                )
            }
        }
    }

    fun resetProgress() {
        _progress.value = DownloadProgress()
    }

    private fun downloadTile(zoom: Int, x: Int, y: Int): ByteArray? {
        for (attempt in 0..MAX_RETRIES) {
            try {
                Thread.sleep(RATE_LIMIT_MS)
                val url = URL("$TILE_SERVER/$zoom/$x/$y.png")
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("User-Agent", USER_AGENT)
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.connect()

                if (conn.responseCode == 200) {
                    return conn.inputStream.readBytes()
                }
                conn.disconnect()
            } catch (_: Exception) {
                // retry
            }
        }
        return null
    }

    private fun tmsY(zoom: Int, y: Int): Int {
        return (1 shl zoom) - 1 - y
    }

    private data class TileCoord(val x: Int, val y: Int)

    private class BoundingBox(
        centerLat: Double,
        centerLon: Double,
        radiusKm: Int,
    ) {
        val minLat: Double
        val maxLat: Double
        val minLon: Double
        val maxLon: Double

        init {
            val latDelta = radiusKm / 111.32
            val lonDelta = radiusKm / (111.32 * Math.cos(Math.toRadians(centerLat)))
            minLat = centerLat - latDelta
            maxLat = centerLat + latDelta
            minLon = centerLon - lonDelta
            maxLon = centerLon + lonDelta
        }

        fun tilesForZoom(zoom: Int): List<TileCoord> {
            val minX = lonToTile(minLon, zoom)
            val maxX = lonToTile(maxLon, zoom)
            val minY = latToTile(maxLat, zoom)
            val maxY = latToTile(minLat, zoom)
            val result = mutableListOf<TileCoord>()
            for (x in minX..maxX) {
                for (y in minY..maxY) {
                    result.add(TileCoord(x, y))
                }
            }
            return result
        }

        private fun lonToTile(lon: Double, zoom: Int): Int {
            val n = 1 shl zoom
            return ((lon + 180.0) / 360.0 * n).toInt().coerceIn(0, n - 1)
        }

        private fun latToTile(lat: Double, zoom: Int): Int {
            val n = 1 shl zoom
            val latRad = Math.toRadians(lat)
            return (n * (1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0)
                .toInt().coerceIn(0, n - 1)
        }
    }
}
