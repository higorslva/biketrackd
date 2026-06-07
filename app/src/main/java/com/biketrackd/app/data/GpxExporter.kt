package com.biketrackd.app.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
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
    <trkseg>"""

    private const val GPX_TRKPT = """      <trkpt lat="%.6f" lon="%.6f"></trkpt>"""

    private const val GPX_FOOTER = """    </trkseg>
  </trk>
</gpx>"""

    fun generate(
        trailPoints: List<Pair<Double, Double>>,
        session: PedalSession,
    ): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        val timeFormat = SimpleDateFormat("dd/MM_HH'h'mm", Locale.getDefault())
        val now = dateFormat.format(Date())
        val name = "Sessao_${timeFormat.format(Date(session.timestamp))}"

        val header = GPX_HEADER.format(now, name)
        val points = trailPoints.joinToString("\n") { (lat, lon) ->
            GPX_TRKPT.format(lat, lon)
        }
        return "$header\n$points\n$GPX_FOOTER"
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

        context.startActivity(Intent.createChooser(intent, "Exportar rota via"))
    }
}
