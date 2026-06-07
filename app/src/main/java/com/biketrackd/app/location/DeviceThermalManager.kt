package com.biketrackd.app.location

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.content.getSystemService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThermalLevel { NORMAL, WARM, MODERATE, HOT, CRITICAL, UNKNOWN }

object DeviceThermalManager {

    private var powerManager: PowerManager? = null

    private val _thermalLevel = MutableStateFlow(ThermalLevel.UNKNOWN)
    val thermalLevel: StateFlow<ThermalLevel> = _thermalLevel.asStateFlow()

    private val _batteryTempCelsius = MutableStateFlow(-1f)
    val batteryTempCelsius: StateFlow<Float> = _batteryTempCelsius.asStateFlow()

    private val thermalListener = PowerManager.OnThermalStatusChangedListener { status ->
        _thermalLevel.value = when (status) {
            PowerManager.THERMAL_STATUS_NONE -> ThermalLevel.NORMAL
            PowerManager.THERMAL_STATUS_LIGHT -> ThermalLevel.WARM
            PowerManager.THERMAL_STATUS_MODERATE -> ThermalLevel.MODERATE
            PowerManager.THERMAL_STATUS_SEVERE -> ThermalLevel.HOT
            PowerManager.THERMAL_STATUS_CRITICAL -> ThermalLevel.CRITICAL
            PowerManager.THERMAL_STATUS_EMERGENCY -> ThermalLevel.CRITICAL
            PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalLevel.CRITICAL
            else -> ThermalLevel.UNKNOWN
        }
    }

    fun init(context: Context) {
        powerManager = context.getSystemService()
    }

    fun start() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            powerManager?.addThermalStatusListener(thermalListener)
        }
    }

    fun stop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            powerManager?.removeThermalStatusListener(thermalListener)
        }
    }

    fun onBatteryChanged(intent: Intent) {
        @Suppress("DEPRECATION")
        val temp = intent.getIntExtra(
            android.os.BatteryManager.EXTRA_TEMPERATURE, -1
        )

        if (temp > 0) {
            _batteryTempCelsius.value = temp / 10f
        }

        // Fallback for API < 30: estimate thermal level from battery temp
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R && temp > 0) {
            val celsius = temp / 10f
            _thermalLevel.value = when {
                celsius < 38f -> ThermalLevel.NORMAL
                celsius < 42f -> ThermalLevel.WARM
                celsius < 48f -> ThermalLevel.MODERATE
                else -> ThermalLevel.HOT
            }
        }
    }
}
