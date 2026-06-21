package com.biketrackd.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.maplibre.android.geometry.LatLng
import java.net.URL

object GraphHopperClient {

    private const val BASE_URL = "https://graphhopper.com/api/1/route"

    suspend fun getRoute(
        apiKey: String,
        originLat: Double,
        originLon: Double,
        destLat: Double,
        destLon: Double,
    ): Result<RouteInfo> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL?point=${originLat},${originLon}&point=${destLat},${destLon}" +
                "&profile=bike&locale=pt-BR&instructions=false&points_encoded=false&key=$apiKey")

            val response = url.openConnection().let { conn ->
                conn.connect()
                conn.getInputStream().bufferedReader().readText()
            }

            val json = JSONObject(response)
            if (json.has("message")) {
                return@withContext Result.failure(
                    Exception(json.getString("message")),
                )
            }

            val path = json.getJSONArray("paths").getJSONObject(0)
            val distance = path.getDouble("distance")
            val time = path.getDouble("time") / 1000.0
            val points = path.getJSONObject("points")
            val coordinates = points.getJSONArray("coordinates")

            val latLngs = mutableListOf<LatLng>()
            for (i in 0 until coordinates.length()) {
                val coord = coordinates.getJSONArray(i)
                latLngs.add(LatLng(coord.getDouble(1), coord.getDouble(0)))
            }

            Result.success(RouteInfo(distance, time, latLngs))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
