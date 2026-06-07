package com.biketrackd.app.weather

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

object WeatherRepository {

    private val _weather = MutableStateFlow<WeatherData?>(null)
    val weather: StateFlow<WeatherData?> = _weather.asStateFlow()

    private var lastFetchTime = 0L
    private var lastFetchKm = 0f

    suspend fun refresh(latitude: Double, longitude: Double, totalKm: Float) {
        val now = System.currentTimeMillis()
        val timeSince = now - lastFetchTime
        val kmSince = totalKm - lastFetchKm

        if (timeSince < 30 * 60 * 1000L && kmSince < 5f) return

        val result = withContext(Dispatchers.IO) {
            WeatherClient.fetch(latitude, longitude)
        }

        if (result != null) {
            lastFetchTime = now
            lastFetchKm = totalKm
            _weather.value = result
        }
    }
}
