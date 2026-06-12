package com.biketrackd.app.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.biketrackd.app.R
import com.biketrackd.app.data.DownloadProgress
import com.biketrackd.app.data.MapOfflineManager
import com.biketrackd.app.location.LocationRepository
import com.biketrackd.app.ui.components.DownloadDialog
import com.biketrackd.app.ui.theme.Green500
import com.biketrackd.app.ui.theme.TextPrimary
import kotlinx.coroutines.launch
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

@Composable
fun GpsScreen() {
    val state by LocationRepository.state.collectAsState()
    val downloadProgress by MapOfflineManager.progress.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var followMode by remember { mutableStateOf(true) }
    var isFirstFix by remember { mutableStateOf(true) }
    var showDownloadConfirm by remember { mutableStateOf(false) }
    var lastResetCount by remember { mutableStateOf(0) }
    var mapStateRef by remember { mutableStateOf<MapLibreMapState?>(null) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    if (state.resetCount != lastResetCount) {
        lastResetCount = state.resetCount
        isFirstFix = true
    }

    val trailPoints = LocationRepository.trailPoints

    LaunchedEffect(mapStateRef) {
        val ms = mapStateRef ?: return@LaunchedEffect
        val coords = trailPoints.map { Point.fromLngLat(it.second, it.first) }
        val trailLineString = if (coords.isNotEmpty()) LineString.fromLngLats(coords) else null
        if (trailLineString != null) {
            ms.style.getSourceAs<GeoJsonSource>("trail")?.setGeoJson(
                Feature.fromGeometry(trailLineString),
            )
        } else {
            ms.style.getSourceAs<GeoJsonSource>("trail")?.setGeoJson(
                org.maplibre.geojson.FeatureCollection.fromFeatures(emptyList()),
            )
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapViewRef?.onCreate(null)
                Lifecycle.Event.ON_START -> mapViewRef?.onStart()
                Lifecycle.Event.ON_RESUME -> mapViewRef?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapViewRef?.onPause()
                Lifecycle.Event.ON_STOP -> mapViewRef?.onStop()
                Lifecycle.Event.ON_DESTROY -> {
                    mapViewRef?.onDestroy()
                    mapViewRef = null
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapViewRef?.onDestroy()
            mapViewRef = null
        }
    }

    Box(modifier = Modifier.fillMaxSize().graphicsLayer { clip = true }) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    mapViewRef = this

                    // catch up on lifecycle events
                    val currentState = lifecycleOwner.lifecycle.currentState
                    if (currentState.isAtLeast(Lifecycle.State.CREATED)) onCreate(null)
                    if (currentState.isAtLeast(Lifecycle.State.STARTED)) onStart()
                    if (currentState.isAtLeast(Lifecycle.State.RESUMED)) onResume()

                    getMapAsync { map ->
                        map.uiSettings.apply {
                            isAttributionEnabled = false
                            isLogoEnabled = false
                        }

                        map.setStyle(MapOfflineManager.OPENFREEMAP_STYLE) { style ->
                            style.addSource(GeoJsonSource("trail"))
                            style.addLayer(
                                LineLayer("trail-layer", "trail").apply {
                                    setProperties(
                                        PropertyFactory.lineColor(android.graphics.Color.parseColor("#4CAF50")),
                                        PropertyFactory.lineWidth(8f),
                                        PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                                        PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                                    )
                                },
                            )

                            val pin = drawableToBitmap(ctx, R.drawable.ic_location_pin)
                            style.addImage("user-pin", pin)
                            style.addSource(GeoJsonSource("user"))
                            style.addLayer(
                                SymbolLayer("user-layer", "user").apply {
                                    setProperties(
                                        PropertyFactory.iconImage("user-pin"),
                                        PropertyFactory.iconAnchor(Property.ICON_ANCHOR_CENTER),
                                    )
                                },
                            )

                            val originPin = drawableToBitmap(ctx, R.drawable.ic_origin_marker)
                            style.addImage("origin-pin", originPin)
                            style.addSource(GeoJsonSource("origin"))
                            style.addLayer(
                                SymbolLayer("origin-layer", "origin").apply {
                                    setProperties(
                                        PropertyFactory.iconImage("origin-pin"),
                                        PropertyFactory.iconAnchor(Property.ICON_ANCHOR_CENTER),
                                    )
                                },
                            )

                            mapStateRef = MapLibreMapState(map, style)
                        }

                        map.addOnMapClickListener { _ ->
                            followMode = false
                            true
                        }
                    }
                }
            },
            update = { _ ->
                val ms = mapStateRef ?: return@AndroidView
                val style = ms.style

                val trailLineString = run {
                    val coords = trailPoints.map { Point.fromLngLat(it.second, it.first) }
                    if (coords.isNotEmpty()) LineString.fromLngLats(coords) else null
                }
                if (trailLineString != null) {
                    style.getSourceAs<GeoJsonSource>("trail")?.setGeoJson(
                        Feature.fromGeometry(trailLineString),
                    )
                } else {
                    style.getSourceAs<GeoJsonSource>("trail")?.setGeoJson(
                        org.maplibre.geojson.FeatureCollection.fromFeatures(emptyList()),
                    )
                }

                if (state.hasFix) {
                    val userPoint = Point.fromLngLat(state.longitude, state.latitude)
                    style.getSourceAs<GeoJsonSource>("user")?.setGeoJson(
                        Feature.fromGeometry(userPoint),
                    )

                    if (followMode) {
                        if (isFirstFix) {
                            ms.map.setCameraPosition(
                                CameraPosition.Builder()
                                    .target(LatLng(state.latitude, state.longitude))
                                    .zoom(17.0)
                                    .build(),
                            )
                            isFirstFix = false
                        } else {
                            val currentZoom = ms.map.cameraPosition.zoom
                            val builder = CameraPosition.Builder()
                                .target(LatLng(state.latitude, state.longitude))
                                .zoom(currentZoom)
                            if (state.rawSpeedKmh >= 3f && state.bearing > 0f) {
                                builder.bearing(state.bearing.toDouble())
                            }
                            ms.map.animateCamera(
                                CameraUpdateFactory.newCameraPosition(builder.build()),
                            )
                        }
                    }
                }

                style.getLayerAs<SymbolLayer>("origin-layer")?.setProperties(
                    PropertyFactory.visibility(
                        if (state.hasOrigin) Property.VISIBLE else Property.NONE,
                    ),
                )
                if (state.hasOrigin) {
                    style.getSourceAs<GeoJsonSource>("origin")?.setGeoJson(
                        Feature.fromGeometry(
                            Point.fromLngLat(state.originLongitude, state.originLatitude),
                        ),
                    )
                }
            },
        )

        if (state.hasOrigin) {
            Text(
                text = "\u21A9 ${String.format("%.2f", state.distanceToOrigin / 1000f)} km",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 12.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp),
        ) {
            if (state.hasFix) {
                Button(
                    onClick = {
                        followMode = !followMode
                        if (followMode) isFirstFix = true
                    },
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
            }
            if (state.hasFix && downloadProgress.status != DownloadProgress.Status.Downloading) {
                Spacer(modifier = Modifier.padding(bottom = 8.dp))
                Button(
                    onClick = { showDownloadConfirm = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                        contentColor = TextPrimary,
                    ),
                ) {
                    Text(
                        text = if (downloadProgress.status == DownloadProgress.Status.Completed)
                            "\u2713 Salvo" else "Salvar offline",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }

    if (showDownloadConfirm && state.hasFix) {
        AlertDialog(
            onDismissRequest = { showDownloadConfirm = false },
            title = { Text("Baixar mapa offline") },
            text = {
                Text(
                    "Baixar tiles vectoriais de zoom 10 a 15 num raio de 40km " +
                        "ao redor da sua posição atual?\n\n" +
                        "Recomendado: use Wi-Fi. Mapas vectoriais são 5-10x menores que raster.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDownloadConfirm = false
                    scope.launch {
                        MapOfflineManager.download(
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
                TextButton(onClick = {
                    showDownloadConfirm = false
                }) { Text("Cancelar") }
            },
        )
    }

    DownloadDialog(
        progress = downloadProgress,
        onDismiss = {
            MapOfflineManager.resetProgress()
        },
    )
}

private class MapLibreMapState(
    val map: org.maplibre.android.maps.MapLibreMap,
    val style: Style,
)

private fun drawableToBitmap(context: Context, drawableRes: Int): Bitmap {
    val drawable = ContextCompat.getDrawable(context, drawableRes)!!
    val bitmap = Bitmap.createBitmap(
        drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888,
    )
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
