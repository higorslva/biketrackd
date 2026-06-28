package com.biketrackd.app.ui.screens

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.biketrackd.app.R
import com.biketrackd.app.data.DownloadProgress
import com.biketrackd.app.data.GraphHopperClient
import com.biketrackd.app.data.GraphHopperPreferences
import com.biketrackd.app.data.MapOfflineManager
import com.biketrackd.app.data.RouteInfo
import com.biketrackd.app.data.UnitFormatter
import com.biketrackd.app.data.UnitPreferences
import com.biketrackd.app.location.LocationRepository
import com.biketrackd.app.ui.components.DownloadDialog
import com.biketrackd.app.ui.components.MiniSpeedometerContent
import com.biketrackd.app.ui.theme.Green500
import kotlinx.coroutines.delay
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
fun GpsScreen(showMiniSpeedometer: Boolean = false, burnInDimmed: Boolean = false) {
    val state by LocationRepository.state.collectAsState()
    val downloadProgress by MapOfflineManager.progress.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var followMode by remember { mutableStateOf(true) }
    var isFirstFix by remember { mutableStateOf(true) }
    var showDownloadConfirm by remember { mutableStateOf(false) }
    var showNoKeyDialog by remember { mutableStateOf(false) }
    val routeDimAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (burnInDimmed) 0.4f else 1f,
        animationSpec = androidx.compose.animation.core.tween(800),
        label = "routeDim",
    )
    var lastResetCount by remember { mutableStateOf(0) }
    var mapStateRef by remember { mutableStateOf<MapLibreMapState?>(null) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var destination by remember { mutableStateOf<LatLng?>(null) }
    var routeInfo by remember { mutableStateOf<RouteInfo?>(null) }
    var lastRouteOrigin by remember { mutableStateOf<LatLng?>(null) }
    var lastRouteFetchTime by remember { mutableStateOf(0L) }
    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT

    if (state.resetCount != lastResetCount) {
        lastResetCount = state.resetCount
        isFirstFix = true
    }

    val trailPoints by remember { derivedStateOf { LocationRepository.trailPoints } }
    val unitSystem = UnitPreferences.get(context)

    LaunchedEffect(mapStateRef) {
        val ms = mapStateRef ?: return@LaunchedEffect
        val coords = trailPoints.map { Point.fromLngLat(it.second, it.first) }
        val trailLineString = if (coords.size >= 2) LineString.fromLngLats(coords) else null
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

    // Auto-reroute when user deviates from the planned route
    LaunchedEffect(destination != null) {
        if (destination == null || mapStateRef == null) return@LaunchedEffect

        while (true) {
            delay(5_000)
            if (routeInfo == null) continue
            if (!state.hasFix) continue

            val apiKey = GraphHopperPreferences.getApiKey(context)
            if (apiKey.isBlank()) continue

            val currentPos = LatLng(state.latitude, state.longitude)
            val lastOrigin = lastRouteOrigin ?: continue
            if (currentPos.distanceTo(lastOrigin) < 50.0) continue

            val now = System.currentTimeMillis()
            if (now - lastRouteFetchTime < 10_000) continue

            GraphHopperClient.getRoute(
                apiKey = apiKey,
                originLat = state.latitude,
                originLon = state.longitude,
                destLat = destination!!.latitude,
                destLon = destination!!.longitude,
            ).onSuccess { newRoute ->
                routeInfo = newRoute
                lastRouteOrigin = LatLng(state.latitude, state.longitude)
                lastRouteFetchTime = System.currentTimeMillis()
                val coords = newRoute.points.map {
                    Point.fromLngLat(it.longitude, it.latitude)
                }
                val lineString = if (coords.isNotEmpty())
                    LineString.fromLngLats(coords) else null
                if (lineString != null) {
                    mapStateRef?.style?.getSourceAs<GeoJsonSource>("route")
                        ?.setGeoJson(Feature.fromGeometry(lineString))
                    mapStateRef?.style?.getLayerAs<LineLayer>("route-layer")
                        ?.setProperties(
                            PropertyFactory.visibility(Property.VISIBLE),
                        )
                }
            }
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

    val mapContent: @Composable (Modifier) -> Unit = { boxModifier ->
        Box(modifier = boxModifier.graphicsLayer { clip = true }) {
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

                            val cachedStyleFile = java.io.File(ctx.cacheDir, "map_style.json")

                            map.setStyle(
                                if (cachedStyleFile.exists()) "file://${cachedStyleFile.absolutePath}"
                                else MapOfflineManager.OPENFREEMAP_STYLE,
                            ) { style ->
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

                                val destPin = drawableToBitmap(ctx, R.drawable.ic_destination_marker)
                                style.addImage("dest-pin", destPin)
                                style.addSource(GeoJsonSource("destination"))
                                style.addLayer(
                                    SymbolLayer("destination-layer", "destination").apply {
                                        setProperties(
                                            PropertyFactory.iconImage("dest-pin"),
                                            PropertyFactory.iconAnchor(Property.ICON_ANCHOR_CENTER),
                                            PropertyFactory.visibility(Property.NONE),
                                        )
                                    },
                                )

                                style.addSource(GeoJsonSource("route"))
                                style.addLayer(
                                    LineLayer("route-layer", "route").apply {
                                        setProperties(
                                            PropertyFactory.lineColor(android.graphics.Color.parseColor("#FF5722")),
                                            PropertyFactory.lineWidth(6f),
                                            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                                            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                                            PropertyFactory.visibility(Property.NONE),
                                        )
                                    },
                                )

                                mapStateRef = MapLibreMapState(map, style)
                            }

                            if (!cachedStyleFile.exists()) {
                                Thread {
                                    try {
                                        val json = java.net.URL(MapOfflineManager.OPENFREEMAP_STYLE)
                                            .openStream().bufferedReader().use { it.readText() }
                                        cachedStyleFile.writeText(json)
                                    } catch (_: Exception) {
                                    }
                                }.start()
                            }

                            map.addOnMapLongClickListener { latLng ->
                                destination = latLng
                                routeInfo = null
                                mapStateRef?.style?.getSourceAs<GeoJsonSource>("destination")?.setGeoJson(
                                    Feature.fromGeometry(Point.fromLngLat(latLng.longitude, latLng.latitude)),
                                )
                                mapStateRef?.style?.getLayerAs<SymbolLayer>("destination-layer")?.setProperties(
                                    PropertyFactory.visibility(Property.VISIBLE),
                                )
                                mapStateRef?.style?.getLayerAs<LineLayer>("route-layer")?.setProperties(
                                    PropertyFactory.visibility(Property.NONE),
                                )
                                true
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
                                        .zoom(15.0)
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
                    text = "\u21A9 ${UnitFormatter.formatLongDistance(state.distanceToOrigin, unitSystem)}",
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

            if (destination != null) {
                val apiKey = GraphHopperPreferences.getApiKey(context)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 12.dp, start = 12.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(12.dp),
                        )
                        .padding(12.dp)
                        .alpha(routeDimAlpha),
                ) {
                    Text(
                        text = "${String.format("%.5f", destination!!.latitude)}, ${String.format("%.5f", destination!!.longitude)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (routeInfo != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = routeInfo!!.distanceDisplay,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = routeInfo!!.timeDisplay,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (routeInfo != null || destination != null) {
                        Button(
                            onClick = {
                                destination = null
                                routeInfo = null
                                lastRouteOrigin = null
                                lastRouteFetchTime = 0L
                                mapStateRef?.style?.getLayerAs<SymbolLayer>("destination-layer")
                                    ?.setProperties(PropertyFactory.visibility(Property.NONE))
                                mapStateRef?.style?.getLayerAs<LineLayer>("route-layer")
                                    ?.setProperties(PropertyFactory.visibility(Property.NONE))
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ),
                        ) {
                            Text(stringResource(R.string.btn_clear), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                if (routeInfo == null) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                            .alpha(routeDimAlpha),
                    ) {
                        Button(
                            onClick = {
                                if (apiKey.isBlank()) {
                                    showNoKeyDialog = true
                                    return@Button
                                }
                                val dest = destination ?: return@Button
                                scope.launch {
                                    GraphHopperClient.getRoute(
                                        apiKey = apiKey,
                                        originLat = state.latitude,
                                        originLon = state.longitude,
                                        destLat = dest.latitude,
                                        destLon = dest.longitude,
                                    ).onSuccess { route ->
                                        routeInfo = route
                                        lastRouteOrigin = LatLng(state.latitude, state.longitude)
                                        lastRouteFetchTime = System.currentTimeMillis()
                                        val coords = route.points.map {
                                            Point.fromLngLat(it.longitude, it.latitude)
                                        }
                                        val lineString = if (coords.isNotEmpty())
                                            LineString.fromLngLats(coords) else null
                                        if (lineString != null) {
                                            mapStateRef?.style?.getSourceAs<GeoJsonSource>("route")
                                                ?.setGeoJson(Feature.fromGeometry(lineString))
                                            mapStateRef?.style?.getLayerAs<LineLayer>("route-layer")
                                                ?.setProperties(
                                                    PropertyFactory.visibility(Property.VISIBLE),
                                                )
                                        }
                                    }.onFailure {
                                        // silently ignore
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            Text(
                                stringResource(R.string.btn_tracar_rota),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }

            if (state.hasOrigin) {
                Text(
                    text = "\u21A9 ${UnitFormatter.formatLongDistance(state.distanceToOrigin, unitSystem)}",
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
                                else MaterialTheme.colorScheme.onSurface,
                        ),
                    ) {
                        Text(
                            text = stringResource(if (followMode) R.string.btn_following else R.string.btn_follow),
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
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    ) {
                        Text(
                            text = stringResource(if (downloadProgress.status == DownloadProgress.Status.Completed) R.string.btn_saved_offline else R.string.btn_save_offline),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }

    if (showMiniSpeedometer) {
        if (isPortrait) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(0.65f)) {
                    mapContent(Modifier.fillMaxSize())
                }
                MiniSpeedometerContent(Modifier.weight(0.35f))
            }
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(0.65f)) {
                    mapContent(Modifier.fillMaxSize())
                }
                MiniSpeedometerContent(Modifier.weight(0.35f))
            }
        }
    } else {
        mapContent(Modifier.fillMaxSize())
    }

    if (showDownloadConfirm && state.hasFix) {
        AlertDialog(
            onDismissRequest = { showDownloadConfirm = false },
            title = { Text(stringResource(R.string.dialog_download_title)) },
            text = {
                Text(stringResource(R.string.dialog_download_message))
            },
            confirmButton = {
                TextButton(onClick = {
                    showDownloadConfirm = false
                    scope.launch {
                        MapOfflineManager.download(
                            context = context,
                            name = context.getString(R.string.dialog_download_name, state.latitude, state.longitude),
                            centerLat = state.latitude,
                            centerLon = state.longitude,
                            radiusKm = 40,
                        )
                    }
                }) { Text(stringResource(R.string.btn_download)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDownloadConfirm = false
                }) { Text(stringResource(R.string.btn_cancel)) }
            },
        )
    }

    if (showNoKeyDialog) {
        AlertDialog(
            onDismissRequest = { showNoKeyDialog = false },
            title = { Text(stringResource(R.string.dialog_gh_no_key_title)) },
            text = { Text(stringResource(R.string.dialog_gh_no_key_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showNoKeyDialog = false
                    val intent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://graphhopper.com"),
                    )
                    context.startActivity(intent)
                }) { Text(stringResource(R.string.btn_sign_up)) }
            },
            dismissButton = {
                TextButton(onClick = { showNoKeyDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
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
