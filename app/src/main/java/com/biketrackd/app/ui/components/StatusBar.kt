package com.biketrackd.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biketrackd.app.R
import com.biketrackd.app.data.UnitFormatter
import com.biketrackd.app.data.UnitPreferences
import com.biketrackd.app.location.LocationRepository
import com.biketrackd.app.ui.theme.GpsFix
import com.biketrackd.app.ui.theme.GpsNoFix
import com.biketrackd.app.weather.WeatherRepository

@Composable
fun StatusBar(
    batteryLevel: Int,
    isBatteryCharging: Boolean,
    onStartSession: () -> Unit = {},
    onStopSession: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val locationState by LocationRepository.state.collectAsState()
    val weather by WeatherRepository.weather.collectAsState()
    val unitSystem = UnitPreferences.get(context = LocalContext.current)

    val batteryText = if (batteryLevel < 0) "--%" else "$batteryLevel%"

    val batteryBlinking = batteryLevel <= 15 && !isBatteryCharging
    var batteryVisible by remember { mutableStateOf(true) }
    LaunchedEffect(batteryBlinking) {
        if (batteryBlinking) {
            while (true) {
                batteryVisible = !batteryVisible
                kotlinx.coroutines.delay(500)
            }
        } else {
            batteryVisible = true
        }
    }

    val batteryIcon = when {
        isBatteryCharging -> R.drawable.ic_battery_charging
        batteryLevel <= 15 -> R.drawable.ic_battery_0
        batteryLevel <= 40 -> R.drawable.ic_battery_25
        batteryLevel <= 65 -> R.drawable.ic_battery_50
        batteryLevel <= 87 -> R.drawable.ic_battery_75
        else -> R.drawable.ic_battery_100
    }
    val currentIcon = if (batteryBlinking && !batteryVisible)
        R.drawable.ic_battery_outline else batteryIcon

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(currentIcon),
            contentDescription = "Bateria",
            tint = Color.Unspecified,
            modifier = Modifier.size(16.dp),
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = batteryText,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = "GPS:",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.width(4.dp))

        val gpsBlinking = !locationState.hasFix
        var dotVisible by remember { mutableStateOf(true) }
        LaunchedEffect(gpsBlinking) {
            if (gpsBlinking) {
                while (true) {
                    dotVisible = !dotVisible
                    kotlinx.coroutines.delay(500)
                }
            } else {
                dotVisible = true
            }
        }
        val dotColor = when {
            locationState.hasFix -> GpsFix
            dotVisible -> GpsNoFix
            else -> Color.Transparent
        }
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color = dotColor, shape = CircleShape),
            content = {},
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = if (locationState.hasFix) "OK" else "--",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = if (locationState.hasFix)
                UnitFormatter.formatSpeed(locationState.speedKmh, unitSystem) else "--",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.width(14.dp))

        Icon(
            painter = weatherIconPainter(weather?.weatherCode ?: -1),
            contentDescription = "Clima",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(16.dp),
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = weather?.let {
                UnitFormatter.formatCelsius(Math.round(it.temperature), unitSystem)
            } ?: "--",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.width(14.dp))

        Surface(
            onClick = {
                if (locationState.isSessionActive) onStopSession()
                else onStartSession()
            },
            shape = RoundedCornerShape(6.dp),
            color = if (locationState.isSessionActive)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Text(
                text = if (locationState.isSessionActive) "⏹ ENCERRAR"
                    else "▶ INICIAR",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (locationState.isSessionActive)
                    MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Icon(
            painter = painterResource(R.drawable.ic_clock),
            contentDescription = "Hora",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(16.dp),
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date()),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}



@Composable
private fun weatherIconPainter(code: Int): Painter = when (code) {
    0 -> painterResource(R.drawable.ic_weather_clear)
    in 1..3 -> painterResource(R.drawable.ic_weather_cloudy)
    in 45..48 -> painterResource(R.drawable.ic_weather_cloudy)
    in 51..55 -> painterResource(R.drawable.ic_weather_rain)
    in 61..65 -> painterResource(R.drawable.ic_weather_rain)
    in 71..77 -> painterResource(R.drawable.ic_weather_snow)
    in 80..82 -> painterResource(R.drawable.ic_weather_rain)
    in 95..99 -> painterResource(R.drawable.ic_weather_storm)
    else -> painterResource(R.drawable.ic_weather_clear)
}
