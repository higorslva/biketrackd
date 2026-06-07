package com.biketrackd.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.biketrackd.app.data.AppDatabase
import com.biketrackd.app.data.GpxExporter
import com.biketrackd.app.data.OfflineMapInfo
import com.biketrackd.app.data.OfflineMapManager
import com.biketrackd.app.data.PedalSession
import com.biketrackd.app.data.TileDownloader
import com.biketrackd.app.location.LocationRepository
import com.biketrackd.app.ui.components.CityResult
import com.biketrackd.app.ui.components.CitySearchDialog
import com.biketrackd.app.ui.components.DownloadDialog
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = rememberDao(context)

    val sessions by dao.getAllFlow().collectAsState(initial = emptyList())
    val downloadProgress by TileDownloader.progress.collectAsState()

    var refreshTrigger by remember { mutableStateOf(0) }
    var showCitySearch by remember { mutableStateOf(false) }

    val offlineMaps = remember(refreshTrigger) { OfflineMapManager.listMaps(context) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        // === Session controls ===
        item {
            Button(
                onClick = {
                    val state = LocationRepository.state.value
                    if (!state.hasFix || state.elapsedSeconds < 5) {
                        Toast.makeText(context, "Nenhum dado de sessão", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    scope.launch {
                        val avgSpeed = if (state.elapsedSeconds > 0)
                            state.totalDistanceMeters / state.elapsedSeconds * 3.6f else 0f
                        dao.insert(
                            PedalSession(
                                timestamp = System.currentTimeMillis(),
                                totalDistance = state.totalDistanceMeters,
                                maxSpeed = state.maxSpeedKmh,
                                avgSpeed = avgSpeed,
                                durationSeconds = state.elapsedSeconds,
                            )
                        )
                        Toast.makeText(context, "Sessão salva!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Salvar Sessão")
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            Button(
                onClick = {
                    LocationRepository.resetSession()
                    Toast.makeText(context, "Sessão reiniciada", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Text("Reiniciar Sessão")
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        item {
            Text(
                text = "Histórico",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }

        items(sessions, key = { it.id }) { session ->
            SessionCard(session)
        }

        // === Offline maps section ===
        item { Spacer(modifier = Modifier.height(24.dp)) }

        item {
            Text(
                text = "Mapas Offline",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }

        if (offlineMaps.isEmpty()) {
            item {
                Text(
                    text = "Nenhum mapa salvo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        } else {
            items(offlineMaps, key = { it.fileName }) { map ->
                OfflineMapCard(
                    map = map,
                    onDelete = {
                        OfflineMapManager.deleteMap(context, map.fileName)
                        refreshTrigger++
                    },
                )
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            Button(
                onClick = { showCitySearch = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Text("Baixar por cidade")
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    if (showCitySearch) {
        CitySearchDialog(
            onCitySelected = { city ->
                showCitySearch = false
                downloadMapForCity(context, scope, city)
            },
            onDismiss = { showCitySearch = false },
        )
    }

    DownloadDialog(
        progress = downloadProgress,
        onDismiss = {
            TileDownloader.resetProgress()
            refreshTrigger++
        },
    )
}

@Composable
private fun OfflineMapCard(
    map: OfflineMapInfo,
    onDelete: () -> Unit,
) {
    val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(Date(map.createdAt))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = map.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${map.fileSizeDisplay} — ${map.tileCountDisplay} tiles — $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Zoom ${map.minZoom}-${map.maxZoom} · Raio ${map.radiusKm}km",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Apagar",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun SessionCard(session: PedalSession) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(Date(session.timestamp))
    val km = session.totalDistance / 1000f
    val avg = session.avgSpeed
    val max = session.maxSpeed
    val minutes = session.durationSeconds / 60

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$dateStr — ${String.format("%.2f", km)} km",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Média ${String.format("%.1f", avg)} km/h  " +
                        "Max ${String.format("%.0f", max)} km/h  ${minutes} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (!session.trailData.isNullOrBlank()) {
                IconButton(onClick = {
                    try {
                        val arr = JSONArray(session.trailData)
                        val points = mutableListOf<Pair<Double, Double>>()
                        for (i in 0 until arr.length()) {
                            val coord = arr.getJSONArray(i)
                            points.add(coord.getDouble(0) to coord.getDouble(1))
                        }
                        val gpx = GpxExporter.generate(points, session)
                        GpxExporter.share(context, gpx, session)
                    } catch (_: Exception) {
                        Toast.makeText(context, "Erro ao exportar", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Exportar GPX",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberDao(context: android.content.Context) = androidx.compose.runtime.remember {
    AppDatabase.getInstance(context).pedalSessionDao()
}

private fun downloadMapForCity(
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
    city: CityResult,
) {
    scope.launch {
        TileDownloader.download(
            context = context,
            name = city.displayName.split(",").first().trim(),
            centerLat = city.lat,
            centerLon = city.lon,
            radiusKm = 40,
        )
    }
}
