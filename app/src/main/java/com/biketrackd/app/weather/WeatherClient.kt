package com.biketrackd.app.weather

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object WeatherClient {

    fun fetch(latitude: Double, longitude: Double): WeatherData? {
        return try {
            val url = URL(
                "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$latitude&longitude=$longitude" +
                    "&current_weather=true"
            )
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.requestMethod = "GET"

            val body = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val json = JSONObject(body)
            val current = json.getJSONObject("current_weather")

            WeatherData(
                temperature = current.getDouble("temperature").toFloat(),
                windspeed = current.getDouble("windspeed").toFloat(),
                weatherCode = current.getInt("weathercode"),
                isDay = current.getInt("is_day") == 1
            )
        } catch (_: Exception) {
            null
        }
    }
}
