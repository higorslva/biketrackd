package com.biketrackd.app.location

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.content.getSystemService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.reflect.Proxy

enum class ThermalLevel { NORMAL, WARM, MODERATE, HOT, CRITICAL, UNKNOWN }

object DeviceThermalManager {

    private var powerManager: PowerManager? = null

    private val _thermalLevel = MutableStateFlow(ThermalLevel.UNKNOWN)
    val thermalLevel: StateFlow<ThermalLevel> = _thermalLevel.asStateFlow()

    private val _batteryTempCelsius = MutableStateFlow(-1f)
    val batteryTempCelsius: StateFlow<Float> = _batteryTempCelsius.asStateFlow()

    private var thermalListener: Any? = null
    private var thermalListenerRegistered = false

    fun init(context: Context) {
        powerManager = context.getSystemService()
    }

    fun start() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !thermalListenerRegistered) {
            registerThermalListenerReflectively()
        }
    }

    private fun registerThermalListenerReflectively() {
        try {
            val listenerClass = Class.forName("android.os.PowerManager\$OnThermalStatusChangedListener")
            val proxy = Proxy.newProxyInstance(
                listenerClass.classLoader,
                arrayOf(listenerClass)
            ) { _, method, args ->
                if (method.name == "onThermalStatusChanged" && args != null) {
                    val status = args[0] as Int
                    _thermalLevel.value = when (status) {
                        PowerManager.THERMAL_STATUS_NONE -> ThermalLevel.NORMAL
                        PowerManager.THERMAL_STATUS_LIGHT -> ThermalLevel.WARM
                        PowerManager.THERMAL_STATUS_MODERATE -> ThermalLevel.MODERATE
                        PowerManager.THERMAL_STATUS_SEVERE -> ThermalLevel.HOT
                        PowerManager.THERMAL_STATUS_CRITICAL,
                        PowerManager.THERMAL_STATUS_EMERGENCY,
                        PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalLevel.CRITICAL
                        else -> ThermalLevel.UNKNOWN
                    }
                }
                null
            }
            val pm = powerManager ?: return
            pm.javaClass.getMethod("addThermalStatusListener", listenerClass).invoke(pm, proxy)
            thermalListener = proxy
            thermalListenerRegistered = true
        } catch (_: Exception) {
        }
    }

    fun stop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val listener = thermalListener ?: return
            try {
                val listenerClass = Class.forName("android.os.PowerManager\$OnThermalStatusChangedListener")
                val pm = powerManager ?: return
                pm.javaClass.getMethod("removeThermalStatusListener", listenerClass).invoke(pm, listener)
                thermalListenerRegistered = false
            } catch (_: Exception) {
            }
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
