package com.biketrackd.app.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biketrackd.app.R
import com.biketrackd.app.data.AppDatabase
import com.biketrackd.app.data.PedalSession
import com.biketrackd.app.data.UnitFormatter
import com.biketrackd.app.data.UnitPreferences
import com.biketrackd.app.ui.LocalFontScale
import com.biketrackd.app.ui.components.LineChart
import com.biketrackd.app.ui.components.SessionCard
import com.biketrackd.app.ui.components.StatRow
import com.biketrackd.app.ui.components.calcStreak
import com.biketrackd.app.ui.components.exportSessionsAsJson
import com.biketrackd.app.ui.components.dateLabel
import com.biketrackd.app.ui.components.shareJson
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun StatsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val fontScale = LocalFontScale.current
    val dao = remember { AppDatabase.getInstance(context).pedalSessionDao() }

    val sessions by dao.getAllFlow().collectAsState(initial = emptyList())
    val sessionCount by dao.getSessionCountFlow().collectAsState(initial = 0)
    val totalDist by dao.getTotalDistanceFlow().collectAsState(initial = 0f)
    val totalDur by dao.getTotalDurationFlow().collectAsState(initial = 0L)
    val overallAvg by dao.getOverallAvgSpeedFlow().collectAsState(initial = 0f)

    val bestDist by dao.getBestDistanceFlow().collectAsState(initial = 0f)
    val bestDuration by dao.getBestDurationFlow().collectAsState(initial = 0L)
    val bestAvg by dao.getBestAvgSpeedFlow().collectAsState(initial = 0f)
    val bestMaxSpeed by dao.getBestMaxSpeedFlow().collectAsState(initial = 0f)

    val bestDistSession by dao.getBestDistanceSessionFlow().collectAsState(initial = null)
    val bestDurationSession by dao.getBestDurationSessionFlow().collectAsState(initial = null)
    val bestAvgSession by dao.getBestAvgSpeedSessionFlow().collectAsState(initial = null)
    val bestMaxSpeedSession by dao.getBestMaxSpeedSessionFlow().collectAsState(initial = null)
    val bestSession by dao.getBestSessionFlow().collectAsState(initial = null)

    // Current month range
    val monthStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val monthEnd = remember {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val monthDistance by dao.getRangeDistanceFlow(monthStart, monthEnd).collectAsState(initial = 0f)
    val monthSessionCount by dao.getRangeSessionCountFlow(monthStart, monthEnd).collectAsState(initial = 0)

    val weeklyDist by dao.getWeeklyDistances().collectAsState(initial = emptyList())
    val monthlyDist by dao.getMonthlyDistances().collectAsState(initial = emptyList())
    val timestamps by dao.getAllTimestamps().collectAsState(initial = emptyList())

    val unitSystem by remember { mutableStateOf(UnitPreferences.get(context)) }
    var showWeekly by remember { mutableStateOf(true) }
    var showAvgSpeedChart by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    // Delete state
    var sessionToDelete by remember { mutableStateOf<PedalSession?>(null) }

    if (sessionToDelete != null) {
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text(stringResource(R.string.dialog_delete_title)) },
            text = { Text(stringResource(R.string.dialog_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        sessionToDelete?.let { dao.deleteById(it.id) }
                        sessionToDelete = null
                    }
                }) {
                    Text(stringResource(R.string.btn_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            },
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        // === Stats section ===
        item { Spacer(modifier = Modifier.height(24.dp)) }

        item {
            Text(
                text = stringResource(R.string.section_stats),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }

        item {
            val hours = totalDur / 3600
            val minutes = (totalDur % 3600) / 60
            val durStr = if (hours > 0) "${hours}h ${minutes}min" else "${minutes}min"
            val avgDist = if (sessionCount > 0) totalDist / sessionCount else 0f
            val avgTimeMin = if (sessionCount > 0) (totalDur / sessionCount) / 60 else 0L
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    StatRow(stringResource(R.string.stat_total_ridden), UnitFormatter.formatLongDistance(totalDist, unitSystem))
                    Spacer(modifier = Modifier.height(6.dp))
                    StatRow(stringResource(R.string.stat_sessions), "$sessionCount")
                    Spacer(modifier = Modifier.height(6.dp))
                    StatRow(stringResource(R.string.stat_total_time), durStr)
                    Spacer(modifier = Modifier.height(6.dp))
                    StatRow(stringResource(R.string.stat_avg_speed), UnitFormatter.formatSpeed(overallAvg, unitSystem))
                    Spacer(modifier = Modifier.height(6.dp))
                    StatRow(stringResource(R.string.stat_avg_distance), UnitFormatter.formatLongDistance(avgDist, unitSystem))
                    Spacer(modifier = Modifier.height(6.dp))
                    StatRow(stringResource(R.string.stat_avg_time), "${avgTimeMin}min")
                }
            }
        }

        // === Personal Records section ===
        item { Spacer(modifier = Modifier.height(24.dp)) }

        item {
            Text(
                text = stringResource(R.string.section_records),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }

        item {
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
                    StatRow(stringResource(R.string.stat_best_distance),
                        UnitFormatter.formatLongDistance(bestDist, unitSystem),
                        subtitle = bestDistSession?.let { stringResource(R.string.label_record_on, dateLabel(it.timestamp)) })
                    Spacer(modifier = Modifier.height(6.dp))
                    StatRow(stringResource(R.string.stat_best_time),
                        bestDurStr,
                        subtitle = bestDurationSession?.let { stringResource(R.string.label_record_on, dateLabel(it.timestamp)) })
                    Spacer(modifier = Modifier.height(6.dp))
                    StatRow(stringResource(R.string.stat_best_avg),
                        UnitFormatter.formatSpeed(bestAvg, unitSystem),
                        subtitle = bestAvgSession?.let { stringResource(R.string.label_record_on, dateLabel(it.timestamp)) })
                    Spacer(modifier = Modifier.height(6.dp))
                    StatRow(stringResource(R.string.stat_best_max_speed),
                        UnitFormatter.formatSpeed(bestMaxSpeed, unitSystem),
                        subtitle = bestMaxSpeedSession?.let { stringResource(R.string.label_record_on, dateLabel(it.timestamp)) })
                }
            }
        }

        // === This Month section ===
        item { Spacer(modifier = Modifier.height(24.dp)) }

        item {
            Text(
                text = stringResource(R.string.section_this_month),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    StatRow(stringResource(R.string.stat_total_ridden), UnitFormatter.formatLongDistance(monthDistance, unitSystem))
                    Spacer(modifier = Modifier.height(6.dp))
                    StatRow(stringResource(R.string.stat_sessions), "$monthSessionCount")
                }
            }
        }

        // === Best Session section ===
        item { Spacer(modifier = Modifier.height(24.dp)) }

        item {
            Text(
                text = stringResource(R.string.section_best_session),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }

        item {
            if (bestSession != null) {
                val s = bestSession!!
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val km = s.totalDistance / 1000f
                val mins = s.durationSeconds / 60
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = sdf.format(Date(s.timestamp)),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        StatRow(stringResource(R.string.stat_total_ridden), String.format("%.2f km", km))
                        Spacer(modifier = Modifier.height(4.dp))
                        StatRow(stringResource(R.string.stat_avg_speed), UnitFormatter.formatSpeed(s.avgSpeed, unitSystem))
                        Spacer(modifier = Modifier.height(4.dp))
                        StatRow(stringResource(R.string.stat_max_speed), UnitFormatter.formatSpeed(s.maxSpeed, unitSystem))
                        Spacer(modifier = Modifier.height(4.dp))
                        StatRow(stringResource(R.string.stat_total_time), "${mins}min")
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.toast_no_session),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
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
                    text = stringResource(if (showWeekly) R.string.section_distance_week else R.string.section_distance_month),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(if (showWeekly) R.string.btn_toggle_monthly else R.string.btn_toggle_weekly),
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
                                    fontSize = (9 * fontScale).sp,
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
                text = stringResource(R.string.section_streak),
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
                        text = stringResource(R.string.label_streak, streak,
                            stringResource(if (streak == 1) R.string.label_streak_day else R.string.label_streak_days)),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.label_last_date, lastDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // === Speed per session chart section ===
        item { Spacer(modifier = Modifier.height(24.dp)) }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(if (showAvgSpeedChart) R.string.section_avg_speed_chart else R.string.section_max_speed_chart),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(if (showAvgSpeedChart) R.string.btn_show_max_speed else R.string.btn_show_avg_speed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { showAvgSpeedChart = !showAvgSpeedChart },
                )
            }
        }

        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }

        item {
            val chartSessions = remember(sessions) { sessions.takeLast(15).filter { it.avgSpeed > 0 } }
            val data = remember(chartSessions, showAvgSpeedChart) {
                if (chartSessions.isEmpty()) emptyList()
                else chartSessions.map { s ->
                    dateLabel(s.timestamp) to if (showAvgSpeedChart) s.avgSpeed else s.maxSpeed
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (data.isNotEmpty()) {
                        LineChart(
                            data = data,
                            color = Color(0xFF2196F3),
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.toast_no_session),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                }
            }
        }

        // === History section ===
        item { Spacer(modifier = Modifier.height(24.dp)) }

        item {
            Text(
                text = stringResource(R.string.section_history),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }

        items(sessions, key = { it.id }) { session ->
            SessionCard(session, onDelete = { sessionToDelete = session })
        }

        // === Export All button ===
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val json = exportSessionsAsJson(sessions)
                    shareJson(context, json)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(stringResource(R.string.btn_export_all))
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
