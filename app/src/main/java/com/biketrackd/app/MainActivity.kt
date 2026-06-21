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
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.biketrackd.app.data.AppDatabase
import com.biketrackd.app.R
import com.biketrackd.app.data.LanguagePreferences
import com.biketrackd.app.data.OrientationPreferences
import com.biketrackd.app.data.PedalSession
import com.biketrackd.app.location.DeviceThermalManager
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
import com.biketrackd.app.ui.theme.TextPrimary
import com.biketrackd.app.weather.WeatherRepository
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val lang = LanguagePreferences.get(newBase)
        val context = if (lang.isNotEmpty()) {
            val locale = java.util.Locale(lang)
            val config = android.content.res.Configuration(newBase.resources.configuration)
            config.setLocale(locale)
            newBase.createConfigurationContext(config)
        } else {
            newBase
        }
        super.attachBaseContext(context)
    }

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
            DeviceThermalManager.onBatteryChanged(intent)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            // Permissão concedida — onStart() iniciará o service
        } else {
            Toast.makeText(this, getString(R.string.toast_gps_permission), Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MapLibre.getInstance(this)

        LocationRepository.init(this)
        DeviceThermalManager.init(this)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val orientation = OrientationPreferences.get(this)
        requestedOrientation = when (orientation) {
            OrientationPreferences.Orientation.PORTRAIT ->
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            OrientationPreferences.Orientation.LANDSCAPE ->
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }

        setContent {
            GpsOssTheme {
                var currentScreen by androidx.compose.runtime.remember {
                    androidx.compose.runtime.mutableStateOf(Screen.GPS)
                }
                var pendingSession by androidx.compose.runtime.remember {
                    mutableStateOf<SessionSummary?>(null)
                }
                var showSidebar by androidx.compose.runtime.remember {
                    mutableStateOf(false)
                }
                val scope = rememberCoroutineScope()

                Box(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.weight(1f)) {
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                when (currentScreen) {
                                    Screen.GPS -> GpsScreen()
                                    Screen.SPEEDOMETER -> SpeedometerScreen(
                                        batteryLevel = batteryLevel,
                                        isBatteryCharging = isBatteryCharging,
                                    )
                                    Screen.SETTINGS -> SettingsScreen()
                                }

                                Button(
                                    onClick = { showSidebar = !showSidebar },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                                        contentColor = TextPrimary,
                                    ),
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(8.dp),
                                ) {
                                    Icon(
                                        imageVector = if (showSidebar) Icons.Default.Close
                                            else Icons.Default.Menu,
                                        contentDescription = stringResource(if (showSidebar) R.string.desc_close_sidebar else R.string.desc_open_sidebar),
                                    )
                                }
                            }
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

                    AnimatedVisibility(
                        visible = showSidebar,
                        enter = slideInHorizontally { -it },
                        exit = slideOutHorizontally { -it },
                    ) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            Sidebar(
                                currentScreen = currentScreen,
                                onScreenSelected = { screen ->
                                    currentScreen = screen
                                    showSidebar = false
                                },
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(Color.Black.copy(alpha = 0.4f))
                                    .clickable { showSidebar = false },
                            )
                        }
                    }
                }

                pendingSession?.let { summary ->
                    AlertDialog(
                        onDismissRequest = { pendingSession = null },
                        title = { Text(stringResource(R.string.dialog_save_title)) },
                        text = {
                            val km = summary.totalDistance / 1000f
                            val minutes = summary.durationSeconds / 60
                            Text(
                                stringResource(R.string.dialog_save_message, km, summary.avgSpeed, summary.maxSpeed, minutes)
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
                                LocationRepository.addToTotalOdometer(summary.totalDistance)
                                LocationRepository.resetSession()
                                pendingSession = null
                            }) { Text(stringResource(R.string.btn_save)) }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                LocationRepository.resetSession()
                                pendingSession = null
                            }) { Text(stringResource(R.string.btn_discard)) }
                        },
                    )
                }
            }
        }

        enableImmersiveMode()
        requestPermissions()
        observeLocationForWeather()
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    override fun onStart() {
        super.onStart()
        if (hasLocationPermission()) {
            startGpsService()
        }
    }

    override fun onStop() {
        super.onStop()
        LocationService.stop(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(batteryReceiver)
        DeviceThermalManager.stop()
    }

    private fun enableImmersiveMode() {
        val decorView = window.decorView
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.apply {
                hide(
                    WindowInsets.Type.statusBars()
                        or WindowInsets.Type.navigationBars()
                )
                systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )

            decorView.setOnSystemUiVisibilityChangeListener { visibility ->
                if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                    decorView.postDelayed({
                        @Suppress("DEPRECATION")
                        decorView.systemUiVisibility = (
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        )
                    }, 100)
                }
            }
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            requestPermissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startGpsService() {
        LocationService.start(this)
        DeviceThermalManager.start()
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
}
