package com.biketrackd.app.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.biketrackd.app.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GpxExporter {

    private const val GPX_HEADER = """<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1"
    creator="GPS-OSS"
    xmlns="http://www.topografix.com/GPX/1/1"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd">
  <metadata>
    <time>%s</time>
  </metadata>
  <trk>
    <name>%s</name>
    <desc>%s</desc>
    <trkseg>"""

    private const val GPX_TRKPT = """      <trkpt lat="%.6f" lon="%.6f">%s</trkpt>"""

    private const val GPX_FOOTER = """    </trkseg>
  </trk>
</gpx>"""

    fun generate(
        trailPoints: List<TrailPoint>,
        session: PedalSession,
    ): String {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        val timeFormat = SimpleDateFormat("dd/MM_HH'h'mm", Locale.getDefault())
        val sessionStart = session.timestamp - session.durationSeconds * 1000
        val startTime = isoFormat.format(Date(sessionStart))
        val name = "Sessao_${timeFormat.format(Date(session.timestamp))}"
        val desc = buildDesc(session)

        val header = GPX_HEADER.format(startTime, name, desc)
        val points = trailPoints.joinToString("\n") { point ->
            val inner = buildString {
                point.altitude?.takeUnless { it.isNaN() }?.let { altitude ->
                    append("<ele>")
                    append(String.format(Locale.US, "%.1f", altitude))
                    append("</ele>")
                }
                if (point.timestamp > 0L) {
                    if (isNotEmpty()) append("\n        ")
                    append("<time>")
                    append(isoFormat.format(Date(point.timestamp)))
                    append("</time>")
                }
            }
            String.format(Locale.US, GPX_TRKPT, point.latitude, point.longitude, inner)
        }
        return "$header\n$points\n$GPX_FOOTER"
    }

    private fun buildDesc(session: PedalSession): String {
        val parts = mutableListOf<String>()
        parts.add("Distance: " + String.format(Locale.US, "%.2f km", session.totalDistance / 1000f))
        parts.add("Avg speed: " + String.format(Locale.US, "%.2f km/h", session.avgSpeed))
        parts.add("Max speed: " + String.format(Locale.US, "%.2f km/h", session.maxSpeed))
        session.elevationGain?.let { parts.add("Elevation gain: ${Math.round(it)} m") }
        session.elevationLoss?.let { parts.add("Elevation loss: ${Math.round(it)} m") }
        session.movingSeconds?.let { parts.add("Moving time: ${formatHms(it)}") }
        parts.add("Elapsed time: ${formatHms(session.durationSeconds)}")
        return parts.joinToString(" | ")
    }

    private fun formatHms(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
    }

    fun share(context: Context, gpx: String, session: PedalSession) {
        val dir = File(context.cacheDir, "gpx")
        dir.mkdirs()

        val timeFormat = SimpleDateFormat("dd-MM_HH'h'mm", Locale.getDefault())
        val name = "Sessao_${timeFormat.format(Date(session.timestamp))}.gpx"
        val file = File(dir, name)
        file.writeText(gpx)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.gpxprovider",
            file,
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, context.getString(R.string.title_share_route)))
    }
}
