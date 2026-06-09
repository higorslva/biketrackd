package com.biketrackd.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegionStatus
import kotlin.coroutines.resume

object OfflineMapManager {

    suspend fun listMaps(context: Context): List<OfflineMapInfo> {
        val appCtx = context.applicationContext
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                OfflineManager.getInstance(appCtx).listOfflineRegions(
                    object : OfflineManager.ListOfflineRegionsCallback {
                        override fun onList(offlineRegions: Array<OfflineRegion>?) {
                            val regions = offlineRegions ?: emptyArray()
                            if (regions.isEmpty()) {
                                cont.resume(emptyList())
                                return
                            }
                            val results = mutableListOf<OfflineMapInfo>()
                            var pending = regions.size
                            for (region in regions) {
                                val meta = MapOfflineManager.getMetadata(appCtx, region.id)
                                if (meta != null) {
                                    region.getStatus(object : OfflineRegion.OfflineRegionStatusCallback {
                                        override fun onStatus(status: OfflineRegionStatus?) {
                                            if (status != null) {
                                                synchronized(results) {
                                                    results.add(
                                                        OfflineMapInfo(
                                                            id = region.id,
                                                            name = meta.name,
                                                            fileName = region.id.toString(),
                                                            fileSizeBytes = status.completedResourceSize,
                                                            tileCount = status.completedResourceCount.toInt(),
                                                            centerLat = meta.lat,
                                                            centerLon = meta.lon,
                                                            radiusKm = meta.radiusKm,
                                                            minZoom = region.definition.minZoom.toInt(),
                                                            maxZoom = region.definition.maxZoom.toInt(),
                                                            createdAt = meta.createdAt,
                                                        )
                                                    )
                                                }
                                            }
                                            pending--
                                            if (pending == 0) {
                                                cont.resume(results.toList())
                                            }
                                        }

                                        override fun onError(error: String?) {
                                            pending--
                                            if (pending == 0) {
                                                cont.resume(results.toList())
                                            }
                                        }
                                    })
                                } else {
                                    pending--
                                    if (pending == 0) {
                                        cont.resume(results.toList())
                                    }
                                }
                            }
                        }

                        override fun onError(error: String) {
                            cont.resume(emptyList())
                        }
                    },
                )
            }
        }
    }

    suspend fun deleteMap(context: Context, id: Long) {
        val appCtx = context.applicationContext
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                OfflineManager.getInstance(appCtx).listOfflineRegions(
                    object : OfflineManager.ListOfflineRegionsCallback {
                        override fun onList(offlineRegions: Array<OfflineRegion>?) {
                            val regions = offlineRegions ?: emptyArray()
                            val target = regions.find { it.id == id }
                            if (target != null) {
                                target.delete(object : OfflineRegion.OfflineRegionDeleteCallback {
                                    override fun onDelete() {
                                        MapOfflineManager.deleteMetadata(appCtx, id)
                                        cont.resume(Unit)
                                    }

                                    override fun onError(error: String) {
                                        MapOfflineManager.deleteMetadata(appCtx, id)
                                        cont.resume(Unit)
                                    }
                                })
                            } else {
                                MapOfflineManager.deleteMetadata(appCtx, id)
                                cont.resume(Unit)
                            }
                        }

                        override fun onError(error: String) {
                            MapOfflineManager.deleteMetadata(appCtx, id)
                            cont.resume(Unit)
                        }
                    },
                )
            }
        }
    }
}
