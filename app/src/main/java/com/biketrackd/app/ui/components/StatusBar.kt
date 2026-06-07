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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biketrackd.app.R
import com.biketrackd.app.location.LocationRepository
import com.biketrackd.app.ui.theme.BatteryGood
import com.biketrackd.app.ui.theme.BatteryLow
import com.biketrackd.app.ui.theme.BatteryMedium
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

    val batteryColor = when {
        isBatteryCharging -> BatteryGood
        batteryLevel <= 15 -> BatteryLow
        batteryLevel <= 40 -> BatteryMedium
        else -> BatteryGood
    }

    val batteryText = if (batteryLevel < 0) "--%" else "$batteryLevel%"
    val batteryIcon = if (isBatteryCharging) R.drawable.ic_battery_charging
    else batteryIconForLevel(batteryLevel)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(batteryIcon),
            contentDescription = "Bateria",
            tint = batteryColor,
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

        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    color = if (locationState.hasFix) GpsFix else GpsNoFix,
                    shape = CircleShape,
                ),
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
                String.format("%.0f", locationState.speedKmh) else "--",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.width(3.dp))

        Text(
            text = "km/h",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            text = weather?.temperatureDisplay() ?: "--°C",
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

private fun batteryIconForLevel(pct: Int): Int = when {
    pct <= 0 -> R.drawable.ic_battery_0
    pct <= 25 -> R.drawable.ic_battery_25
    pct <= 50 -> R.drawable.ic_battery_50
    pct <= 75 -> R.drawable.ic_battery_75
    else -> R.drawable.ic_battery_100
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
