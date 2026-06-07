package com.biketrackd.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Slider
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val sessionCount by dao.getSessionCountFlow().collectAsState(initial = 0)
    val totalDist by dao.getTotalDistanceFlow().collectAsState(initial = 0f)
    val totalDur by dao.getTotalDurationFlow().collectAsState(initial = 0L)
    val overallAvg by dao.getOverallAvgSpeedFlow().collectAsState(initial = 0f)

    val bestDist by dao.getBestDistanceFlow().collectAsState(initial = 0f)
    val bestDuration by dao.getBestDurationFlow().collectAsState(initial = 0L)
    val bestAvg by dao.getBestAvgSpeedFlow().collectAsState(initial = 0f)
    val weeklyDist by dao.getWeeklyDistances().collectAsState(initial = emptyList())
    val monthlyDist by dao.getMonthlyDistances().collectAsState(initial = emptyList())
    val timestamps by dao.getAllTimestamps().collectAsState(initial = emptyList())

    var showWeekly by remember { mutableStateOf(true) }

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
                        LocationRepository.addToTotalOdometer(state.totalDistanceMeters)
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

        // === Stats section ===
        item { Spacer(modifier = Modifier.height(24.dp)) }

        item {
            Text(
                text = "Estatísticas Gerais",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }

        item {
            val totalKm = totalDist / 1000f
            val hours = totalDur / 3600
            val minutes = (totalDur % 3600) / 60
            val durStr = if (hours > 0) "${hours}h ${minutes}min" else "${minutes}min"
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    StatRow("Total Pedalado", "${String.format("%.1f", totalKm)} km")
                    Spacer(modifier = Modifier.height(6.dp))
                    StatRow("Sessões", "$sessionCount")
                    Spacer(modifier = Modifier.height(6.dp))
                    StatRow("Tempo Total", durStr)
                    Spacer(modifier = Modifier.height(6.dp))
                    StatRow("Média Geral", "${String.format("%.1f", overallAvg)} km/h")
                }
            }
        }

        // === Records section ===
        item { Spacer(modifier = Modifier.height(24.dp)) }

        item {
            Text(
                text = "Recordes Pessoais",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }

        item {
            val bestKm = bestDist / 1000f
            val bestHours = bestDuration / 3600
            val bestMinutes = (bestDuration % 3600) / 60
            val bestDurStr = if (bestHours > 0) "${bestHours}h ${bestMinutes}min" else "${bestMinutes}min"
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    StatRow("🔥 Maior Distância", "${String.format("%.1f", bestKm)} km")
                    Spacer(modifier = Modifier.height(6.dp))
                    StatRow("⏱  Mais Tempo", bestDurStr)
                    Spacer(modifier = Modifier.height(6.dp))
                    StatRow("📈 Melhor Média", "${String.format("%.1f", bestAvg)} km/h")
                }
            }
        }

        // === Weekly/Monthly chart section ===
        item { Spacer(modifier = Modifier.height(24.dp)) }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (showWeekly) "Distância por Semana" else "Distância por Mês",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (showWeekly) "Mensal" else "Semanal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { showWeekly = !showWeekly },
                )
            }
        }

        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }

        item {
            val data: List<Pair<String, Float>> = if (showWeekly)
                weeklyDist.reversed().map { it.week to it.dist }
            else
                monthlyDist.reversed().map { it.month to it.dist }
            val maxDist = data.maxOfOrNull { it.second } ?: 1f

            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val barCount = data.size
                            if (barCount == 0) return@Canvas
                            val gap = 8.dp.toPx()
                            val barWidth = (size.width - gap * (barCount + 1)) / barCount
                            val chartHeight = size.height - 4.dp.toPx()

                            data.forEachIndexed { i, (_, dist) ->
                                val barHeight = if (maxDist > 0) (dist / maxDist) * chartHeight else 0f
                                val x = gap + i * (barWidth + gap)
                                val y = size.height - barHeight

                                drawRoundRect(
                                    color = Color(0xFF4CAF50),
                                    topLeft = Offset(x, y),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = CornerRadius(4.dp.toPx()),
                                )
                            }
                        }
                    }

                    if (data.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            data.forEach { (label, _) ->
                                val display = if (showWeekly)
                                    "S" + label.substringAfter("-")
                                else
                                    label.substringAfter("-")
                                Text(
                                    text = display,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }

        // === Streak section ===
        item { Spacer(modifier = Modifier.height(24.dp)) }

        item {
            Text(
                text = "Sequência",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }

        item {
            val streak = remember(timestamps) { calcStreak(timestamps) }
            val lastDate = if (timestamps.isNotEmpty()) {
                val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
                sdf.format(Date(timestamps.first()))
            } else "---"

            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🔥 $streak ${if (streak == 1) "dia" else "dias"} consecutivos",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Última: $lastDate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // === Config section ===
        item { Spacer(modifier = Modifier.height(24.dp)) }

        item {
            Text(
                text = "Configurações",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }

        item {
            val currentMax by LocationRepository.maxSpeedArc.collectAsState()

            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Velocidade Máxima",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "${String.format("%.0f", currentMax)} km/h",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = currentMax,
                        onValueChange = { LocationRepository.setMaxSpeedArc(it) },
                        valueRange = 20f..100f,
                        steps = 15,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("20", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("100", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
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

        // === About section ===
        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "BikeTrackd v0.3",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ciclocomputador offline de código aberto",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "github.com/higorslva/biketrackd",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
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
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
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

private fun calcStreak(timestamps: List<Long>): Int {
    if (timestamps.isEmpty()) return 0
    val days = timestamps.map { millisToDayNumber(it) }.distinct().sortedDescending()
    var streak = 1
    for (i in 1 until days.size) {
        if (days[i - 1] - days[i] == 1L) streak++
        else break
    }
    return streak
}

private fun millisToDayNumber(millis: Long): Long {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = millis }
    return cal.get(java.util.Calendar.YEAR).toLong() * 366 +
        cal.get(java.util.Calendar.DAY_OF_YEAR)
}
