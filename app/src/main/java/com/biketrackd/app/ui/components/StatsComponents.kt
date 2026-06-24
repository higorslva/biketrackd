package com.biketrackd.app.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.biketrackd.app.R
import com.biketrackd.app.data.GpxExporter
import com.biketrackd.app.data.PedalSession
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SessionCard(session: PedalSession, onDelete: () -> Unit = {}) {
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
                    text = stringResource(R.string.label_session_card, avg, max, minutes),
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
                        Toast.makeText(context, context.getString(R.string.toast_export_error), Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = stringResource(R.string.desc_export_gpx),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.desc_delete),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String, subtitle: String? = null) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
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
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

fun calcStreak(timestamps: List<Long>): Int {
    if (timestamps.isEmpty()) return 0
    val days = timestamps.map { millisToDayNumber(it) }.distinct().sortedDescending()
    var streak = 1
    for (i in 1 until days.size) {
        if (days[i - 1] - days[i] == 1L) streak++
        else break
    }
    return streak
}

fun millisToDayNumber(millis: Long): Long {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = millis }
    return cal.get(java.util.Calendar.YEAR).toLong() * 366 +
        cal.get(java.util.Calendar.DAY_OF_YEAR)
}

fun dateLabel(millis: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return sdf.format(Date(millis))
}

fun exportSessionsAsJson(sessions: List<PedalSession>): String {
    val arr = JSONArray()
    for (s in sessions) {
        val obj = JSONObject()
        obj.put("date", dateLabel(s.timestamp))
        obj.put("distance_km", String.format("%.2f", s.totalDistance / 1000f))
        obj.put("avg_speed_kmh", String.format("%.1f", s.avgSpeed))
        obj.put("max_speed_kmh", String.format("%.0f", s.maxSpeed))
        obj.put("duration_min", s.durationSeconds / 60)
        obj.put("has_trail", !s.trailData.isNullOrBlank())
        arr.put(obj)
    }
    return arr.toString(2)
}

fun shareJson(context: android.content.Context, json: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_TEXT, json)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.btn_export_all)))
}
