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
                    "&current=temperature_2m,weather_code,wind_speed_10m"
            )
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.requestMethod = "GET"

            val body = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val json = JSONObject(body)
            val current = json.getJSONObject("current")

            WeatherData(
                temperature = current.getDouble("temperature_2m").toFloat(),
                windspeed = current.getDouble("wind_speed_10m").toFloat(),
                weatherCode = current.getInt("weather_code"),
                isDay = true
            )
        } catch (_: Exception) {
            null
        }
    }
}
