package com.biketrackd.app

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.biketrackd.app.data.AppDatabase
import com.biketrackd.app.data.PedalSession
import com.biketrackd.app.location.LocationRepository
import com.biketrackd.app.location.LocationService
import com.biketrackd.app.location.SessionSummary
import com.biketrackd.app.ui.components.Screen
import com.biketrackd.app.ui.components.Sidebar
import com.biketrackd.app.ui.components.StatusBar
import com.biketrackd.app.ui.screens.GpsScreen
import com.biketrackd.app.ui.screens.SettingsScreen
import com.biketrackd.app.ui.screens.SpeedometerScreen
import com.biketrackd.app.ui.theme.GpsOssTheme
import com.biketrackd.app.weather.WeatherRepository
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var batteryLevel by mutableIntStateOf(-1)
    private var isBatteryCharging by mutableStateOf(false)

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            batteryLevel = if (level >= 0 && scale > 0)
                (level * 100f / scale).toInt() else -1
            isBatteryCharging = intent.getIntExtra(
                BatteryManager.EXTRA_STATUS, -1
            ) == BatteryManager.BATTERY_STATUS_CHARGING
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            startGpsService()
        } else {
            Toast.makeText(this, "Permissão GPS necessária", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        org.osmdroid.config.Configuration.getInstance().apply {
            userAgentValue = "GPS-OSS/1.0"
            osmdroidBasePath = cacheDir
            osmdroidTileCache = cacheDir
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            GpsOssTheme {
                var currentScreen by androidx.compose.runtime.remember {
                    androidx.compose.runtime.mutableStateOf(Screen.GPS)
                }
                var pendingSession by androidx.compose.runtime.remember {
                    mutableStateOf<SessionSummary?>(null)
                }
                val scope = rememberCoroutineScope()

                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        when (currentScreen) {
                            Screen.GPS -> GpsScreen()
                            Screen.SPEEDOMETER -> SpeedometerScreen(
                                batteryLevel = batteryLevel,
                                isBatteryCharging = isBatteryCharging,
                                modifier = Modifier.padding(start = 80.dp)
                            )
                            Screen.SETTINGS -> SettingsScreen(
                                modifier = Modifier.padding(start = 80.dp)
                            )
                        }

                        Sidebar(
                            currentScreen = currentScreen,
                            onScreenSelected = { currentScreen = it },
                        )
                    }

                    if (currentScreen == Screen.GPS) {
                        StatusBar(
                            batteryLevel = batteryLevel,
                            isBatteryCharging = isBatteryCharging,
                            onStartSession = { LocationRepository.startSession() },
                            onStopSession = {
                                pendingSession = LocationRepository.stopSession()
                            },
                        )
                    }
                }

                pendingSession?.let { summary ->
                    AlertDialog(
                        onDismissRequest = { pendingSession = null },
                        title = { Text("Salvar sessão?") },
                        text = {
                            val km = summary.totalDistance / 1000f
                            val minutes = summary.durationSeconds / 60
                            Text(
                                "Distância: ${String.format("%.2f", km)} km\n" +
                                    "Média: ${String.format("%.1f", summary.avgSpeed)} km/h\n" +
                                    "Max: ${String.format("%.0f", summary.maxSpeed)} km/h\n" +
                                    "Duração: $minutes min"
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                scope.launch {
                                    AppDatabase.getInstance(this@MainActivity)
                                        .pedalSessionDao().insert(
                                            PedalSession(
                                                timestamp = System.currentTimeMillis(),
                                                totalDistance = summary.totalDistance,
                                                maxSpeed = summary.maxSpeed,
                                                avgSpeed = summary.avgSpeed,
                                                durationSeconds = summary.durationSeconds,
                                                trailData = LocationRepository.trailToJson(),
                                            )
                                        )
                                }
                                LocationRepository.resetSession()
                                pendingSession = null
                            }) { Text("Salvar") }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                LocationRepository.resetSession()
                                pendingSession = null
                            }) { Text("Descartar") }
                        },
                    )
                }
            }
        }

        enableImmersiveMode()
        checkPermissionsAndStart()
        observeLocationForWeather()
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    private fun enableImmersiveMode() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
    }

    private fun checkPermissionsAndStart() {
        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) {
            startGpsService()
        } else {
            requestPermissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun startGpsService() {
        LocationService.start(this)
    }

    private fun observeLocationForWeather() {
        lifecycleScope.launch {
            LocationRepository.state.collect { state ->
                if (state.hasFix) {
                    WeatherRepository.refresh(
                        state.latitude, state.longitude,
                        state.totalDistanceMeters / 1000f
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(batteryReceiver)
        if (isFinishing) {
            LocationService.stop(this)
        }
    }
}
