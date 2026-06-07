package com.biketrackd.app.weather

data class WeatherData(
    val temperature: Float,
    val windspeed: Float,
    val weatherCode: Int,
    val isDay: Boolean
) {
    fun temperatureDisplay(): String = String.format("%.0f°C", temperature)

    fun conditionText(): String = when (weatherCode) {
        0 -> "Limpo"
        in 1..3 -> "Nublado"
        in 45..48 -> "Neblina"
        in 51..55 -> "Chuvisco"
        in 61..65 -> "Chuva"
        in 71..77 -> "Neve"
        in 80..82 -> "Aguaceiro"
        in 95..99 -> "Tempestade"
        else -> ""
    }
}
