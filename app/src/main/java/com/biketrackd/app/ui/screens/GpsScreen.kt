package com.biketrackd.app.ui.screens

import android.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.biketrackd.app.R
import com.biketrackd.app.data.DownloadProgress
import com.biketrackd.app.data.TileDownloader
import com.biketrackd.app.location.LocationRepository
import com.biketrackd.app.ui.components.DownloadDialog
import com.biketrackd.app.ui.theme.Green500
import com.biketrackd.app.ui.theme.TextPrimary
import kotlinx.coroutines.launch
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay

private val openTopoSource = XYTileSource(
    "OpenTopoMap", 0, 18, 256, ".png",
    arrayOf("https://tile.opentopomap.org/"),
)

@Composable
fun GpsScreen() {
    val state by LocationRepository.state.collectAsState()
    val downloadProgress by TileDownloader.progress.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var followMode by remember { mutableStateOf(true) }
    var isFirstFix by remember { mutableStateOf(true) }
    var showDownloadConfirm by remember { mutableStateOf(false) }
    var lastResetCount by remember { mutableStateOf(0) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    if (state.resetCount != lastResetCount) {
        lastResetCount = state.resetCount
        isFirstFix = true
    }

    val trailPoints = LocationRepository.trailPoints

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(openTopoSource)
                    setMultiTouchControls(true)
                    controller.setZoom(17.0)

                    val polyline = Polyline().apply {
                        outlinePaint.color = Color.parseColor("#4CAF50")
                        outlinePaint.strokeWidth = 8f
                        outlinePaint.isAntiAlias = true
                    }

                    val marker = Marker(this).apply {
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = "Você"
                        setIcon(ctx.getDrawable(R.drawable.ic_location_pin))
                    }

                    overlays.add(polyline)
                    overlays.add(marker)

                    val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                            followMode = false
                            return false
                        }
                        override fun longPressHelper(p: GeoPoint?): Boolean = false
                    })
                    overlays.add(1, eventsOverlay)

                    RotationGestureOverlay(this).apply {
                        isEnabled = true
                        overlays.add(this)
                    }

                    setTag(SimpleMapState(polyline, marker))

                    mapViewRef = this
                }
            },
            update = { mapView ->
                val mapState = mapView.tag as? SimpleMapState ?: return@AndroidView
                val polyline = mapState.polyline
                val marker = mapState.marker

                polyline.setPoints(ArrayList(trailPoints.map { GeoPoint(it.first, it.second) }))

                if (state.hasFix) {
                    val currentPoint = GeoPoint(state.latitude, state.longitude)
                    marker.position = currentPoint

                    if (followMode) {
                        if (isFirstFix) {
                            mapView.controller.setCenter(currentPoint)
                            isFirstFix = false
                        } else {
                            mapView.controller.animateTo(currentPoint)
                        }
                        if (state.bearing > 0f) {
                            mapView.setMapOrientation(state.bearing, true)
                        }
                    }
                }

                mapView.invalidate()
            },
        )

        Button(
            onClick = {
                followMode = !followMode
                if (followMode && state.hasFix) {
                    isFirstFix = true
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (followMode) Green500
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                contentColor = if (followMode) MaterialTheme.colorScheme.onPrimary
                    else TextPrimary,
            ),
        ) {
            Text(
                text = if (followMode) "SEGUINDO" else "SEGUIR",
                style = MaterialTheme.typography.labelLarge,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp),
        ) {
            if (!followMode && state.hasFix) {
                SmallFloatingActionButton(
                    onClick = {
                        followMode = true
                        isFirstFix = true
                        mapViewRef?.setMapOrientation(0f)
                    },
                    shape = RoundedCornerShape(12.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                    contentColor = TextPrimary,
                ) {
                    Text("⟲", style = MaterialTheme.typography.titleMedium)
                }
                androidx.compose.foundation.layout.Spacer(
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            if (state.hasFix && downloadProgress.status != DownloadProgress.Status.Downloading) {
                Button(
                    onClick = { showDownloadConfirm = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                        contentColor = TextPrimary,
                    ),
                ) {
                    Text(
                        text = if (downloadProgress.status == DownloadProgress.Status.Completed)
                            "✓ Salvo" else "Salvar offline",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }

    if (showDownloadConfirm && state.hasFix) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDownloadConfirm = false },
            title = { Text("Baixar mapa offline") },
            text = {
                Text(
                    "Baixar tiles de zoom 10 a 14 num raio de 40km " +
                        "ao redor da sua posição atual?\n\n" +
                        "Recomendado: use Wi-Fi. O download pode levar " +
                        "alguns minutos e consumir ~40 MB."
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showDownloadConfirm = false
                    scope.launch {
                        TileDownloader.download(
                            context = context,
                            name = "Mapa ${String.format("%.4f", state.latitude)}, ${String.format("%.4f", state.longitude)}",
                            centerLat = state.latitude,
                            centerLon = state.longitude,
                            radiusKm = 40,
                        )
                    }
                }) { Text("Baixar") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showDownloadConfirm = false
                }) { Text("Cancelar") }
            },
        )
    }

    DownloadDialog(
        progress = downloadProgress,
        onDismiss = {
            TileDownloader.resetProgress()
        },
    )
}

private class SimpleMapState(
    val polyline: Polyline,
    val marker: Marker,
)
