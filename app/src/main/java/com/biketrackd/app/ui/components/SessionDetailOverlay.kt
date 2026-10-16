package com.biketrackd.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.biketrackd.app.R
import com.biketrackd.app.data.MapOfflineManager
import com.biketrackd.app.data.PedalSession
import com.biketrackd.app.data.UnitFormatter
import com.biketrackd.app.data.UnitPreferences
import org.json.JSONArray
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SessionDetailOverlay(
    session: PedalSession,
    bikeName: String?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val unitSystem = UnitPreferences.get(LocalContext.current)
    val dateStr = remember(session.timestamp) {
        SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(session.timestamp))
    }
    val points = remember(session.trailData) { parseTrail(session.trailData) }
    val hasTrail = points.isNotEmpty()

    BackHandler(onBack = onClose)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        val isPortrait = maxHeight > maxWidth

        if (hasTrail) {
            // Fullscreen map, stats and header floating on top (running-app style)
            SessionTrailMap(
                points = points,
                modifier = Modifier.fillMaxSize(),
            )

            DetailHeader(
                session = session,
                dateStr = dateStr,
                onClose = onClose,
                translucent = true,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 8.dp, top = 8.dp, end = 8.dp),
            )

            if (isPortrait) {
                SessionStatsCard(
                    session = session,
                    bikeName = bikeName,
                    unitSystem = unitSystem,
                    translucent = true,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                        .fillMaxWidth(),
                )
            } else {
                SessionStatsCard(
                    session = session,
                    bikeName = bikeName,
                    unitSystem = unitSystem,
                    translucent = true,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp)
                        .width(280.dp),
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                DetailHeader(
                    session = session,
                    dateStr = dateStr,
                    onClose = onClose,
                    translucent = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, top = 8.dp, end = 8.dp),
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(R.string.label_no_trail),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    SessionStatsCard(
                        session = session,
                        bikeName = bikeName,
                        unitSystem = unitSystem,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailHeader(
    session: PedalSession,
    dateStr: String,
    onClose: () -> Unit,
    translucent: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val hasTrail = !session.trailData.isNullOrBlank()
    val chipColor = if (translucent) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            onClick = onClose,
            shape = RoundedCornerShape(12.dp),
            color = chipColor,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.desc_back),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(10.dp),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = chipColor,
        ) {
            Text(
                text = dateStr,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        if (hasTrail) {
            Surface(
                onClick = { shareSessionGpx(context, session) },
                shape = RoundedCornerShape(12.dp),
                color = chipColor,
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = stringResource(R.string.desc_export_gpx),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(10.dp),
                )
            }
        }
    }
}

@Composable
private fun SessionStatsCard(
    session: PedalSession,
    bikeName: String?,
    unitSystem: UnitPreferences.UnitSystem,
    modifier: Modifier = Modifier,
    translucent: Boolean = false,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = if (translucent) 0.85f else 1f,
            ),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            StatRow(
                stringResource(R.string.label_distance),
                UnitFormatter.formatLongDistance(session.totalDistance, unitSystem),
            )
            Spacer(modifier = Modifier.height(6.dp))
            StatRow(
                stringResource(R.string.stat_avg_speed),
                UnitFormatter.formatSpeed(session.avgSpeed, unitSystem),
            )
            Spacer(modifier = Modifier.height(6.dp))
            StatRow(
                stringResource(R.string.stat_max_speed),
                UnitFormatter.formatSpeed(session.maxSpeed, unitSystem),
            )
            Spacer(modifier = Modifier.height(6.dp))
            StatRow(
                stringResource(R.string.stat_duration),
                formatSessionDuration(session.durationSeconds),
            )
            if (bikeName != null) {
                Spacer(modifier = Modifier.height(6.dp))
                StatRow(stringResource(R.string.label_bike), bikeName)
            }
        }
    }
}

private class SessionMapState(
    val map: org.maplibre.android.maps.MapLibreMap,
    val style: Style,
)

@Composable
private fun SessionTrailMap(
    points: List<Pair<Double, Double>>,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var mapStateRef by remember { mutableStateOf<SessionMapState?>(null) }
    var mapSize by remember { mutableStateOf(IntSize.Zero) }

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
            mapStateRef = null
        }
    }

    LaunchedEffect(mapStateRef, mapSize) {
        val ms = mapStateRef ?: return@LaunchedEffect
        if (points.isEmpty() || mapSize.width == 0) return@LaunchedEffect
        try {
            if (points.size >= 2) {
                val bounds = LatLngBounds.Builder()
                points.forEach { bounds.include(LatLng(it.first, it.second)) }
                ms.map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 96))
            } else {
                ms.map.setCameraPosition(
                    CameraPosition.Builder()
                        .target(LatLng(points[0].first, points[0].second))
                        .zoom(15.0)
                        .build(),
                )
            }
        } catch (_: Exception) {
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { mapSize = it },
            factory = { ctx ->
                MapView(ctx).apply {
                    mapViewRef = this
                    val currentState = lifecycleOwner.lifecycle.currentState
                    if (currentState.isAtLeast(Lifecycle.State.CREATED)) onCreate(null)
                    if (currentState.isAtLeast(Lifecycle.State.STARTED)) onStart()
                    if (currentState.isAtLeast(Lifecycle.State.RESUMED)) onResume()

                    getMapAsync { map ->
                        map.uiSettings.apply {
                            isAttributionEnabled = false
                            isLogoEnabled = false
                        }
                        val cachedStyleFile = File(ctx.cacheDir, "map_style.json")
                        map.setStyle(
                            if (cachedStyleFile.exists()) "file://${cachedStyleFile.absolutePath}"
                            else MapOfflineManager.OPENFREEMAP_STYLE,
                        ) { style ->
                            style.addSource(GeoJsonSource("session-trail"))
                            style.addLayer(
                                LineLayer("session-trail-layer", "session-trail").apply {
                                    setProperties(
                                        PropertyFactory.lineColor(
                                            android.graphics.Color.parseColor("#4CAF50"),
                                        ),
                                        PropertyFactory.lineWidth(6f),
                                        PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                                        PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                                    )
                                },
                            )
                            val coords = points.map { Point.fromLngLat(it.second, it.first) }
                            if (coords.size >= 2) {
                                style.getSourceAs<GeoJsonSource>("session-trail")?.setGeoJson(
                                    Feature.fromGeometry(LineString.fromLngLats(coords)),
                                )
                            }
                            mapStateRef = SessionMapState(map, style)
                        }
                    }
                }
            },
        )
    }
}

private fun parseTrail(trailData: String?): List<Pair<Double, Double>> {
    if (trailData.isNullOrBlank()) return emptyList()
    return try {
        val arr = JSONArray(trailData)
        buildList {
            for (i in 0 until arr.length()) {
                val coord = arr.getJSONArray(i)
                add(coord.getDouble(0) to coord.getDouble(1))
            }
        }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun formatSessionDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}min" else "${minutes}min"
}
