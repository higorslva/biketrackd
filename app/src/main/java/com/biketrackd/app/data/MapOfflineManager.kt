package com.biketrackd.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegionError
import org.maplibre.android.offline.OfflineRegionStatus
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition
import kotlin.coroutines.resume

object MapOfflineManager {

    private val _progress = MutableStateFlow(DownloadProgress())
    val progress: StateFlow<DownloadProgress> = _progress.asStateFlow()

    private const val MIN_ZOOM = 10.0
    private const val MAX_ZOOM = 15.0
    const val OPENFREEMAP_STYLE = "https://tiles.openfreemap.org/styles/liberty"
    private const val STYLE_CACHE_FILE = "map_style.json"

    private const val PREFS_NAME = "maplibre_offline_meta"

    suspend fun download(
        context: Context,
        name: String,
        centerLat: Double,
        centerLon: Double,
        radiusKm: Int,
    ) {
        val appCtx = context.applicationContext
        val latDelta = radiusKm / 111.32
        val lonDelta = radiusKm / (111.32 * Math.cos(Math.toRadians(centerLat)))
        val bounds = LatLngBounds.Builder()
            .include(LatLng(centerLat - latDelta, centerLon - lonDelta))
            .include(LatLng(centerLat + latDelta, centerLon + lonDelta))
            .build()

        val density = appCtx.resources.displayMetrics.density
        val definition = OfflineTilePyramidRegionDefinition(
            OPENFREEMAP_STYLE, bounds, MIN_ZOOM, MAX_ZOOM, density,
        )

        _progress.value = DownloadProgress(status = DownloadProgress.Status.Downloading)

        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                OfflineManager.getInstance(appCtx).createOfflineRegion(
                    definition,
                    byteArrayOf(),
                    object : OfflineManager.CreateOfflineRegionCallback {
                        override fun onCreate(region: OfflineRegion) {
                            val id = region.id
                            saveMetadata(appCtx, id, name, centerLat, centerLon, radiusKm)
                            region.setDownloadState(OfflineRegion.STATE_ACTIVE)
                            region.setObserver(object : OfflineRegion.OfflineRegionObserver {
                                var completed = false
                                override fun onStatusChanged(status: OfflineRegionStatus) {
                                    if (completed) return
                                    completed = status.isComplete
                                    _progress.value = _progress.value.copy(
                                        current = status.completedResourceCount.toInt(),
                                        total = status.requiredResourceCount.toInt(),
                                    )
                                    if (completed) {
                                        _progress.value = DownloadProgress(
                                            status = DownloadProgress.Status.Completed,
                                            current = status.completedResourceCount.toInt(),
                                            total = status.requiredResourceCount.toInt(),
                                        )
                                        cont.resume(Unit)
                                    }
                                }

                                override fun onError(error: OfflineRegionError) {
                                    _progress.value = DownloadProgress(
                                        status = DownloadProgress.Status.Error,
                                    )
                                    cont.resume(Unit)
                                }

                                override fun mapboxTileCountLimitExceeded(limit: Long) {
                                }
                            })
                        }

                        override fun onError(error: String) {
                            _progress.value = DownloadProgress(
                                status = DownloadProgress.Status.Error,
                            )
                            cont.resume(Unit)
                        }
                    },
                )
            }
        }

        withContext(Dispatchers.IO) {
            fetchStyleJson(context)
        }
    }

    fun resetProgress() {
        _progress.value = DownloadProgress()
    }

    private fun saveMetadata(
        context: Context,
        id: Long,
        name: String,
        lat: Double,
        lon: Double,
        radiusKm: Int,
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString("region_${id}_name", name)
            .putString("region_${id}_lat", lat.toString())
            .putString("region_${id}_lon", lon.toString())
            .putInt("region_${id}_radius", radiusKm)
            .putLong("region_${id}_createdAt", System.currentTimeMillis())
            .apply()
    }

    internal fun getMetadata(context: Context, id: Long): OfflineMetadata? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString("region_${id}_name", null) ?: return null
        val lat = prefs.getString("region_${id}_lat", null)?.toDoubleOrNull() ?: return null
        val lon = prefs.getString("region_${id}_lon", null)?.toDoubleOrNull() ?: return null
        val radius = prefs.getInt("region_${id}_radius", 0)
        val createdAt = prefs.getLong("region_${id}_createdAt", 0L)
        return OfflineMetadata(id, name, lat, lon, radius, createdAt)
    }

    internal fun deleteMetadata(context: Context, id: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove("region_${id}_name")
            .remove("region_${id}_lat")
            .remove("region_${id}_lon")
            .remove("region_${id}_radius")
            .remove("region_${id}_createdAt")
            .apply()
    }

    internal data class OfflineMetadata(
        val id: Long,
        val name: String,
        val lat: Double,
        val lon: Double,
        val radiusKm: Int,
        val createdAt: Long,
    )

    fun getCachedStyleJson(context: Context): String? {
        return try {
            val file = java.io.File(context.cacheDir, STYLE_CACHE_FILE)
            if (file.exists()) file.readText() else null
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchStyleJson(context: Context) {
        try {
            val json = java.net.URL(OPENFREEMAP_STYLE).openStream()
                .bufferedReader().use { it.readText() }
            val file = java.io.File(context.cacheDir, STYLE_CACHE_FILE)
            file.writeText(json)
        } catch (_: Exception) {
        }
    }
}
