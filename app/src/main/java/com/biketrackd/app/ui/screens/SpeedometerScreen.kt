package com.biketrackd.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import com.biketrackd.app.R
import com.biketrackd.app.data.SpeedLimitPreferences
import com.biketrackd.app.data.UnitFormatter
import com.biketrackd.app.data.UnitPreferences
import com.biketrackd.app.location.DeviceThermalManager
import com.biketrackd.app.location.LocationRepository
import com.biketrackd.app.location.ThermalLevel
import com.biketrackd.app.ui.theme.ArcTrack
import com.biketrackd.app.ui.theme.Cyan
import com.biketrackd.app.ui.theme.GpsFix
import com.biketrackd.app.ui.theme.GpsNoFix
import com.biketrackd.app.ui.theme.GpsWeak
import com.biketrackd.app.ui.theme.SpeedHigh
import com.biketrackd.app.ui.theme.SpeedLow
import com.biketrackd.app.ui.theme.SpeedMedium
import com.biketrackd.app.ui.theme.WarningAmber
import com.biketrackd.app.ui.theme.WarningRed
import com.biketrackd.app.weather.WeatherRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val ARC_START = 135f
private const val ARC_SWEEP = 270f

@Composable
fun SpeedometerScreen(
    batteryLevel: Int = -1,
    isBatteryCharging: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val state by LocationRepository.state.collectAsState()
    val weather by WeatherRepository.weather.collectAsState()
    val thermalLevel by DeviceThermalManager.thermalLevel.collectAsState()
    val maxSpeedArc by LocationRepository.maxSpeedArc.collectAsState()

    val speed = if (state.hasFix) state.speedKmh else -1f
    val animatedSpeed by animateFloatAsState(
        targetValue = if (speed >= 0) speed else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "speed",
    )

    val ctx = LocalContext.current
    val unitSystem = UnitPreferences.get(ctx)
    val speedLimitEnabled = SpeedLimitPreferences.isEnabled(ctx)
    val speedLimit = SpeedLimitPreferences.getLimit(ctx)
    val speedLimitExceeded = speedLimitEnabled && state.hasFix && animatedSpeed > speedLimit

    val speedProgress = if (state.hasFix && maxSpeedArc > 0f)
        (animatedSpeed / maxSpeedArc).coerceIn(0f, 1f) else 0f
    val speedColor = when {
        speedLimitExceeded -> WarningRed
        !state.hasFix -> TextSecondary
        speedProgress < 0.4f -> SpeedLow
        speedProgress < 0.7f -> SpeedMedium
        else -> SpeedHigh
    }

    val batteryColor = when {
        isBatteryCharging -> SpeedLow
        batteryLevel <= 15 -> WarningRed
        batteryLevel <= 40 -> WarningAmber
        else -> SpeedLow
    }
    val batteryText = if (batteryLevel < 0) "--%" else "$batteryLevel%"

    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    val elapsedHms = formatElapsed(state.elapsedSeconds)

    val isMoving = state.speedKmh >= 3f
    val showGearWarning = !state.isSessionActive && isMoving && state.hasFix
    val segmentFont = FontFamily(Font(R.font.segment7standard))

    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    if (isPortrait) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WarningLight(
                    color = when {
                        batteryLevel <= 15 -> WarningRed
                        batteryLevel <= 40 -> WarningAmber
                        else -> SpeedLow
                    },
                    label = "BAT",
                    blinking = batteryLevel <= 15,
                    isCritical = batteryLevel <= 15,
                )
                WarningLight(
                    color = if (showGearWarning) WarningAmber else SpeedLow,
                    label = "ENG",
                )
                WarningLight(
                    color = when {
                        !state.hasFix -> GpsNoFix
                        state.speedKmh < 3f -> GpsWeak
                        else -> GpsFix
                    },
                    label = "GPS",
                    blinking = !state.hasFix,
                    isCritical = !state.hasFix,
                )
                WarningLight(
                    color = when (thermalLevel) {
                        ThermalLevel.NORMAL, ThermalLevel.UNKNOWN -> SpeedLow
                        ThermalLevel.WARM -> WarningAmber
                        ThermalLevel.MODERATE, ThermalLevel.HOT, ThermalLevel.CRITICAL -> WarningRed
                    },
                    label = "TMP",
                    blinking = thermalLevel in listOf(
                        ThermalLevel.MODERATE, ThermalLevel.HOT, ThermalLevel.CRITICAL
                    ),
                    isCritical = thermalLevel in listOf(
                        ThermalLevel.MODERATE, ThermalLevel.HOT, ThermalLevel.CRITICAL
                    ),
                )
            }

            Box(
                modifier = Modifier.size(260.dp),
                contentAlignment = Alignment.Center,
            ) {
                SpeedometerArc(
                    speed = animatedSpeed,
                    hasFix = state.hasFix,
                    maxSpeedArc = maxSpeedArc,
                    speedColor = speedColor,
                    modifier = Modifier.fillMaxSize(),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (state.hasFix) String.format("%.0f", animatedSpeed) else "--",
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = segmentFont,
                        color = speedColor,
                        letterSpacing = (-2).sp,
                    )
                    Text(
                        text = UnitFormatter.speedUnit(unitSystem),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (speedLimitExceeded) {
                        Text(
                            text = "\u26A0 CICLOVIA ${UnitFormatter.speedKmhToUnit(speedLimit.toFloat(), unitSystem).toInt()} ${UnitFormatter.speedUnit(unitSystem)}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarningRed,
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        RoundedCornerShape(12.dp),
                    )
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = time,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    DataValue(
                        label = "ALTITUDE",
                        value = if (state.hasFix) UnitFormatter.formatAltitude(state.altitude.toFloat(), unitSystem) else "--",
                        valueSize = 16,
                    )
                    val slopeColor = when {
                        state.slope > 5f -> WarningRed
                        state.slope > 2f -> WarningAmber
                        state.slope < -5f -> WarningRed
                        state.slope < -2f -> WarningAmber
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    val slopeSign = if (state.slope > 0f) "+" else ""
                    DataValue(
                        label = "INCLINAÇÃO",
                        value = if (state.hasFix) "${slopeSign}${String.format("%.0f", state.slope)}%" else "--",
                        valueColor = slopeColor,
                        valueSize = 16,
                    )
                    DataValue(
                        label = "MÁX",
                        value = if (state.hasFix) UnitFormatter.formatSpeed(state.maxSpeedKmh, unitSystem) else "--",
                        valueSize = 16,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            painter = weatherIconPainter(weather?.weatherCode ?: -1),
                            contentDescription = stringResource(R.string.desc_weather),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = weather?.let {
                                UnitFormatter.formatCelsius(Math.round(it.temperature), unitSystem)
                            } ?: "--",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    DataValue(label = "TEMPO", value = elapsedHms, valueSize = 16)
                    DataValue(
                        label = "DISTÂNCIA",
                        value = if (state.hasFix)
                            UnitFormatter.formatLongDistance(state.totalDistanceMeters, unitSystem) else "--",
                        valueSize = 16,
                    )
                    DataValue(
                        label = "TOTAL",
                        value = if (state.hasFix)
                            UnitFormatter.formatTotalDistance(state.totalOdometerMeters, unitSystem) else "--",
                        valueSize = 16,
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        BatteryBar(
                            level = batteryLevel,
                            isCharging = isBatteryCharging,
                            modifier = Modifier.fillMaxWidth(0.6f).height(16.dp),
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = batteryText,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = batteryColor,
                            )
                            if (isBatteryCharging) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_bolt),
                                    contentDescription = stringResource(R.string.desc_charging),
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(20.dp).padding(start = 2.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // === LEFT CLUSTER ===
        Column(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Altitude
            Column(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                DataValue(
                    label = "ALTITUDE",
                    value = if (state.hasFix) UnitFormatter.formatAltitude(state.altitude.toFloat(), unitSystem) else "--",
                    valueSize = 20,
                )
            }

            // Inclinação
            Column(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                val slopeColor = when {
                    state.slope > 5f -> WarningRed
                    state.slope > 2f -> WarningAmber
                    state.slope < -5f -> WarningRed
                    state.slope < -2f -> WarningAmber
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                val slopeSign = if (state.slope > 0f) "+" else ""
                DataValue(
                    label = "INCLINAÇÃO",
                    value = if (state.hasFix) "${slopeSign}${String.format("%.0f", state.slope)}%" else "--",
                    valueColor = slopeColor,
                    valueSize = 20,
                )
            }

            Column(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                DataValue(
                    label = "MÁX",
                    value = if (state.hasFix)
                        UnitFormatter.formatSpeed(state.maxSpeedKmh, unitSystem) else "--",
                    valueSize = 20,
                )
            }

            // Weather
            Column(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        painter = weatherIconPainter(weather?.weatherCode ?: -1),
                        contentDescription = stringResource(R.string.desc_weather),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = weather?.let {
                            UnitFormatter.formatCelsius(Math.round(it.temperature), unitSystem)
                        } ?: "--",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // === CENTER CLUSTER ===
        Column(
            modifier = Modifier
                .weight(3f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Warning lights
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                WarningLight(
                    color = when {
                        batteryLevel <= 15 -> WarningRed
                        batteryLevel <= 40 -> WarningAmber
                        else -> SpeedLow
                    },
                    label = "BAT",
                    blinking = batteryLevel <= 15,
                    isCritical = batteryLevel <= 15,
                )
                WarningLight(
                    color = if (showGearWarning) WarningAmber else SpeedLow,
                    label = "ENG",
                )
                WarningLight(
                    color = when {
                        !state.hasFix -> GpsNoFix
                        state.speedKmh < 3f -> GpsWeak
                        else -> GpsFix
                    },
                    label = "GPS",
                    blinking = !state.hasFix,
                    isCritical = !state.hasFix,
                )
                WarningLight(
                    color = when (thermalLevel) {
                        ThermalLevel.NORMAL, ThermalLevel.UNKNOWN -> SpeedLow
                        ThermalLevel.WARM -> WarningAmber
                        ThermalLevel.MODERATE, ThermalLevel.HOT, ThermalLevel.CRITICAL -> WarningRed
                    },
                    label = "TMP",
                    blinking = thermalLevel in listOf(
                        ThermalLevel.MODERATE, ThermalLevel.HOT, ThermalLevel.CRITICAL
                    ),
                    isCritical = thermalLevel in listOf(
                        ThermalLevel.MODERATE, ThermalLevel.HOT, ThermalLevel.CRITICAL
                    ),
                )
            }

            // Speedometer arc with digital speed overlaid
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(220.dp),
            ) {
                SpeedometerArc(
                    speed = animatedSpeed,
                    hasFix = state.hasFix,
                    maxSpeedArc = maxSpeedArc,
                    speedColor = speedColor,
                    modifier = Modifier.fillMaxSize(),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (state.hasFix) String.format("%.0f", animatedSpeed) else "--",
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = segmentFont,
                        color = speedColor,
                        letterSpacing = (-2).sp,
                    )
                    Text(
                        text = UnitFormatter.speedUnit(unitSystem),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (speedLimitExceeded) {
                        Text(
                            text = stringResource(R.string.warning_speed_limit, UnitFormatter.speedKmhToUnit(speedLimit.toFloat(), unitSystem).toInt(), UnitFormatter.speedUnit(unitSystem)),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarningRed,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp),
                    )
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = time,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // === RIGHT CLUSTER ===
        Column(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Group 1: TEMPO
            Column(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                DataValue(
                    label = "TEMPO",
                    value = elapsedHms,
                    valueSize = 20,
                )
            }

            // Group 2: DISTÂNCIA + TOTAL
            Column(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                DataValue(
                    label = "DISTÂNCIA",
                    value = if (state.hasFix)
                        UnitFormatter.formatLongDistance(state.totalDistanceMeters, unitSystem) else "--",
                    valueSize = 20,
                )
                Spacer(modifier = Modifier.height(8.dp))
                DataValue(
                    label = "TOTAL",
                    value = if (state.hasFix)
                        UnitFormatter.formatTotalDistance(state.totalOdometerMeters, unitSystem) else "--",
                    valueSize = 20,
                )
            }

            // Group 3: Battery
            Column(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                BatteryBar(
                    level = batteryLevel,
                    isCharging = isBatteryCharging,
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(20.dp),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = batteryText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = batteryColor,
                    )
                    if (isBatteryCharging) {
                        Icon(
                            painter = painterResource(R.drawable.ic_bolt),
                            contentDescription = stringResource(R.string.desc_charging),
                            tint = Color.Unspecified,
                            modifier = Modifier
                                .size(24.dp)
                                .padding(start = 4.dp),
                        )
                    }
                }
            }
        }
    }
    }
}

// ---- Sub-components ----

@Composable
private fun SpeedometerArc(
    speed: Float,
    hasFix: Boolean,
    maxSpeedArc: Float,
    speedColor: Color,
    modifier: Modifier = Modifier,
) {
    val progress = if (hasFix) (speed / maxSpeedArc).coerceIn(0f, 1f) else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 400),
        label = "arc",
    )

    Canvas(modifier = modifier) {
        val strokeWidth = 14.dp.toPx()
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

        // Background arc
        drawArc(
            color = ArcTrack,
            startAngle = ARC_START,
            sweepAngle = ARC_SWEEP,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )

        // Active arc
        if (animatedProgress > 0.001f) {
            val sweepAngle = ARC_SWEEP * animatedProgress

            drawArc(
                color = speedColor,
                startAngle = ARC_START,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
private fun WarningLight(
    color: Color,
    label: String,
    blinking: Boolean = false,
    isCritical: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(blinking) {
        if (blinking) {
            while (true) {
                visible = !visible
                kotlinx.coroutines.delay(500)
            }
        } else {
            visible = true
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        if (isCritical) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = label,
                tint = if (visible) color else Color.Transparent,
                modifier = Modifier.size(12.dp),
            )
        } else {
            Canvas(modifier = Modifier.size(10.dp)) {
                drawCircle(
                    color = if (visible) color else Color.Transparent,
                    radius = size.width / 2f,
                )
            }
        }
        Text(
            text = label,
            fontSize = 7.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BatteryBar(
    level: Int,
    isCharging: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = when {
        isCharging -> SpeedLow
        level <= 15 -> WarningRed
        level <= 40 -> WarningAmber
        else -> SpeedLow
    }
    val fill = if (level >= 0) level / 100f else 0f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cornerRadius = 3.dp.toPx()

        // Background
        drawRoundRect(
            color = ArcTrack,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius),
            size = Size(w, h),
        )

        // Fill
        if (fill > 0.01f) {
            drawRoundRect(
                color = color,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius),
                size = Size(w * fill, h),
            )
        }
    }
}

@Composable
private fun DataValue(
    label: String,
    value: String,
    unit: String = "",
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    valueSize: Int = 16,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$value${if (unit.isNotEmpty()) " $unit" else ""}",
            fontSize = valueSize.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
        )
        Text(
            text = label,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---- Utility ----

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

private val TextSecondary = Color(0xFFB3B3B3)

private fun formatElapsed(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return String.format("%02d:%02d:%02d", h, m, s)
}
