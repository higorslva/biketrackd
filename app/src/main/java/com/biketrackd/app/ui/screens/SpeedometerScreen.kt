package com.biketrackd.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
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
import com.biketrackd.app.ui.theme.SpeedHigh
import com.biketrackd.app.ui.theme.SpeedLow
import com.biketrackd.app.ui.theme.SpeedMedium
import com.biketrackd.app.weather.WeatherRepository

@Composable
fun SpeedometerScreen(
    batteryLevel: Int = -1,
    isBatteryCharging: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val state by LocationRepository.state.collectAsState()
    val weather by WeatherRepository.weather.collectAsState()

    val speed = if (state.hasFix) state.speedKmh else -1f
    val animatedSpeed by animateFloatAsState(
        targetValue = if (speed >= 0) speed else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "speed",
    )

    val speedColor = when {
        !state.hasFix -> TextSecondary
        animatedSpeed < 30f -> SpeedLow
        animatedSpeed < 50f -> SpeedMedium
        else -> SpeedHigh
    }

    val batteryColor = when {
        isBatteryCharging -> BatteryGood
        batteryLevel <= 15 -> BatteryLow
        batteryLevel <= 40 -> BatteryMedium
        else -> BatteryGood
    }
    val batteryIcon = if (isBatteryCharging) R.drawable.ic_battery_charging
    else batteryIconForLevel(batteryLevel)
    val batteryText = if (batteryLevel < 0) "--%" else "$batteryLevel%"

    val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        .format(java.util.Date())

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(
                        color = if (state.hasFix) GpsFix else GpsNoFix,
                        shape = CircleShape,
                    ),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "GPS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (state.hasFix) {
                Text(
                    text = "OK",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Icon(
                painter = weatherIconPainter(weather?.weatherCode ?: -1),
                contentDescription = "Clima",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = weather?.temperatureDisplay() ?: "--°C",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "CLIMA",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }

        Column(
            modifier = Modifier
                .weight(4f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = if (state.hasFix) String.format("%.0f", animatedSpeed) else "--",
                fontSize = 96.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = speedColor,
                letterSpacing = (-2).sp,
            )

            Text(
                text = "km/h",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(36.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                InfoItem(
                    label = "MAX",
                    value = if (state.hasFix)
                        String.format("%.0f", state.maxSpeedKmh) else "--",
                    unit = "km/h",
                )

                Spacer(modifier = Modifier.width(40.dp))

                InfoItem(
                    label = "TEMPO",
                    value = String.format(
                        "%02d:%02d", state.elapsedSeconds / 60,
                        state.elapsedSeconds % 60
                    ),
                    unit = "min",
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            InfoItem(
                label = "DISTÂNCIA",
                value = if (state.hasFix)
                    String.format("%.2f km", state.totalDistanceMeters / 1000) else "-- km",
                unit = "",
            )
        }

        Column(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.End,
        ) {
            Icon(
                painter = painterResource(batteryIcon),
                contentDescription = "Bateria",
                tint = batteryColor,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = batteryText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "BATERIA",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )

            Spacer(modifier = Modifier.height(28.dp))

            Icon(
                painter = painterResource(R.drawable.ic_clock),
                contentDescription = "Hora",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = time,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "RELÓGIO",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun InfoItem(
    label: String,
    value: String,
    unit: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$value${if (unit.isNotEmpty()) " $unit" else ""}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
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

private fun batteryIconForLevel(pct: Int): Int = when {
    pct <= 0 -> R.drawable.ic_battery_0
    pct <= 25 -> R.drawable.ic_battery_25
    pct <= 50 -> R.drawable.ic_battery_50
    pct <= 75 -> R.drawable.ic_battery_75
    else -> R.drawable.ic_battery_100
}

private val TextSecondary = Color(0xFFB3B3B3)
