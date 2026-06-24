package com.biketrackd.app.ui.components

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biketrackd.app.R
import com.biketrackd.app.data.UnitFormatter
import com.biketrackd.app.data.UnitPreferences
import com.biketrackd.app.location.LocationRepository
import com.biketrackd.app.ui.theme.ArcTrack
import com.biketrackd.app.ui.theme.GpsFix
import com.biketrackd.app.ui.theme.GpsNoFix
import com.biketrackd.app.ui.theme.SpeedHigh
import com.biketrackd.app.ui.theme.SpeedLow
import com.biketrackd.app.ui.theme.SpeedMedium
import com.biketrackd.app.ui.theme.TextSecondary
import com.biketrackd.app.weather.WeatherRepository

@Composable
fun MiniSpeedometerContent(modifier: Modifier = Modifier) {
    val state by LocationRepository.state.collectAsState()
    val maxSpeedArc by LocationRepository.maxSpeedArc.collectAsState()
    val weather by WeatherRepository.weather.collectAsState()
    val unitSystem = UnitPreferences.get(context = LocalContext.current)
    val segmentFont = FontFamily(Font(R.font.segment7standard))
    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT

    val speed = if (state.hasFix) state.speedKmh else -1f
    val animatedSpeed by animateFloatAsState(
        targetValue = speed,
        animationSpec = tween(durationMillis = 400),
        label = "mini-speed",
    )

    val progress = if (state.hasFix) (animatedSpeed / maxSpeedArc).coerceIn(0f, 1f) else 0f
    val speedColor = when {
        !state.hasFix -> MaterialTheme.colorScheme.onSurfaceVariant
        animatedSpeed >= UnitFormatter.speedKmhToUnit(maxSpeedArc, unitSystem) * 0.85f -> SpeedHigh
        animatedSpeed >= UnitFormatter.speedKmhToUnit(maxSpeedArc, unitSystem) * 0.6f -> SpeedMedium
        else -> SpeedLow
    }

    val displaySpeed = if (animatedSpeed < 0f) "--"
    else String.format("%.0f", UnitFormatter.speedKmhToUnit(animatedSpeed, unitSystem))

    val outerModifier = modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
        .padding(horizontal = 12.dp, vertical = 6.dp)

    if (isPortrait) {
        Row(
            modifier = outerModifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(0.5f).fillMaxHeight()) {
                arcContent(progress, speedColor, displaySpeed, unitSystem, segmentFont)
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(modifier = Modifier.weight(0.5f).fillMaxHeight()) {
                StatsColumn(state, unitSystem, weather)
            }
        }
    } else {
        Column(
            modifier = outerModifier,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.weight(0.5f).fillMaxWidth()) {
                arcContent(progress, speedColor, displaySpeed, unitSystem, segmentFont)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.weight(0.5f).fillMaxWidth()) {
                StatsColumn(state, unitSystem, weather)
            }
        }
    }
}

@Composable
private fun arcContent(
    progress: Float,
    speedColor: Color,
    displaySpeed: String,
    unitSystem: com.biketrackd.app.data.UnitPreferences.UnitSystem,
    segmentFont: FontFamily,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = (minOf(size.width, size.height) * 0.08f).coerceIn(4f, 12f)
            val arcDim = (minOf(size.width, size.height) - strokeWidth).coerceAtLeast(1f)
            val arcSize = Size(arcDim, arcDim)
            val topLeft = Offset((size.width - arcDim) / 2f, (size.height - arcDim) / 2f)

            drawArc(
                color = ArcTrack,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )

            if (progress > 0.001f) {
                drawArc(
                    color = speedColor,
                    startAngle = 135f,
                    sweepAngle = 270f * progress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = displaySpeed,
                        fontSize = 45.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = segmentFont,
                color = speedColor,
            )
            Text(
                text = UnitFormatter.speedUnit(unitSystem),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun StatRowMini(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier.width(40.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun StatsColumn(
    state: com.biketrackd.app.location.LocationState,
    unitSystem: com.biketrackd.app.data.UnitPreferences.UnitSystem,
    weather: com.biketrackd.app.weather.WeatherData?,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
    ) {
        StatRowMini("TIME", formatElapsedMini(state.elapsedSeconds))
        Spacer(modifier = Modifier.height(4.dp))
        StatRowMini("DIST", UnitFormatter.formatLongDistance(state.totalDistanceMeters, unitSystem))
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "MAX",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier.width(40.dp),
            )
            Text(
                text = UnitFormatter.formatSpeed(state.maxSpeedKmh, unitSystem),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.weight(1f))
            weather?.let { w ->
                Icon(
                    painter = weatherIconPainter(w.weatherCode),
                    contentDescription = "Weather",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = UnitFormatter.formatCelsius(Math.round(w.temperature), unitSystem),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (state.hasFix) GpsFix else GpsNoFix),
            )
        }
    }
}

private fun formatElapsedMini(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return String.format("%02d:%02d:%02d", h, m, s)
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
