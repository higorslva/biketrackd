package com.biketrackd.app.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import com.biketrackd.app.R
import com.biketrackd.app.data.OfflineMapInfo
import com.biketrackd.app.data.OfflineMapManager
import com.biketrackd.app.data.OrientationPreferences
import com.biketrackd.app.data.OrientationPreferences.Orientation
import com.biketrackd.app.data.SpeedLimitPreferences
import com.biketrackd.app.data.AppDatabase
import com.biketrackd.app.data.BurnInPreferences
import com.biketrackd.app.data.FontSizePreferences
import com.biketrackd.app.data.GraphHopperPreferences
import com.biketrackd.app.data.LanguagePreferences
import com.biketrackd.app.data.MapOfflineManager
import com.biketrackd.app.data.PedalSession
import com.biketrackd.app.data.ThemePreferences
import com.biketrackd.app.data.UnitFormatter
import com.biketrackd.app.data.UnitPreferences
import com.biketrackd.app.data.UnitPreferences.UnitSystem
import com.biketrackd.app.location.LocationRepository
import com.biketrackd.app.ui.components.CityResult
import com.biketrackd.app.ui.components.CitySearchDialog
import com.biketrackd.app.ui.components.DownloadDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.biketrackd.app.data.BackupManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { AppDatabase.getInstance(context).pedalSessionDao() }

    val downloadProgress by MapOfflineManager.progress.collectAsState()

    var refreshTrigger by remember { mutableStateOf(0) }
    var showCitySearch by remember { mutableStateOf(false) }
    var offlineMaps by remember { mutableStateOf<List<OfflineMapInfo>>(emptyList()) }

    androidx.compose.runtime.LaunchedEffect(refreshTrigger) {
        offlineMaps = withContext(Dispatchers.Main) {
            OfflineMapManager.listMaps(context)
        }
    }

    var unitSystem by remember { mutableStateOf(UnitPreferences.get(context)) }
    var orientationPref by remember { mutableStateOf(OrientationPreferences.get(context)) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        // === Session controls ===
        item {
            Button(
                onClick = {
                    val state = LocationRepository.state.value
                    if (!state.hasFix || state.elapsedSeconds < 5) {
                        Toast.makeText(context, context.getString(R.string.toast_no_session), Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    scope.launch {
                        val avgSpeed = if (state.elapsedSeconds > 0)
                            state.totalDistanceMeters / state.elapsedSeconds * 3.6f else 0f
                        dao.insert(
                            PedalSession(
                                timestamp = System.currentTimeMillis(),
                                totalDistance = state.totalDistanceMeters,
                                maxSpeed = state.maxSpeedKmh,
                                avgSpeed = avgSpeed,
                                durationSeconds = state.elapsedSeconds,
                            )
                        )
                        LocationRepository.addToTotalOdometer(state.totalDistanceMeters)
                        Toast.makeText(context, context.getString(R.string.toast_session_saved), Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(stringResource(R.string.btn_save_session))
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            Button(
                onClick = {
                    LocationRepository.resetSession()
                    Toast.makeText(context, context.getString(R.string.toast_session_reset), Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Text(stringResource(R.string.btn_reset_session))
            }
        }

        // === Config section ===
        item { Spacer(modifier = Modifier.height(24.dp)) }

        item {
            Text(
                text = stringResource(R.string.section_config),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }

        item {
            val currentMax by LocationRepository.maxSpeedArc.collectAsState()

            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.label_max_speed),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = UnitFormatter.formatSpeed(currentMax, unitSystem),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = currentMax,
                        onValueChange = { LocationRepository.setMaxSpeedArc(it) },
                        valueRange = 20f..100f,
                        steps = 15,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("${UnitFormatter.speedKmhToUnit(20f, unitSystem).toInt()}", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${UnitFormatter.speedKmhToUnit(100f, unitSystem).toInt()}", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // === Speed limit warning section ===
        item { Spacer(modifier = Modifier.height(24.dp)) }

        item {
            val limitEnabled = remember { mutableStateOf(SpeedLimitPreferences.isEnabled(context)) }
            val limitValue = remember { mutableStateOf(SpeedLimitPreferences.getLimit(context).toFloat()) }

            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.section_speed_limit),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.label_enable_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Switch(
                            checked = limitEnabled.value,
                            onCheckedChange = { checked ->
                                limitEnabled.value = checked
                                SpeedLimitPreferences.setEnabled(context, checked)
                            },
                        )
                    }
                    if (limitEnabled.value) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = stringResource(R.string.label_limit),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "${String.format("%.0f", UnitFormatter.speedKmhToUnit(limitValue.value, unitSystem))} ${UnitFormatter.speedUnit(unitSystem)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = limitValue.value,
                            onValueChange = { newValue ->
                                limitValue.value = newValue
                                SpeedLimitPreferences.setLimit(context, newValue.toInt())
                            },
                            valueRange = 10f..60f,
                            steps = 9,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("${UnitFormatter.speedKmhToUnit(10f, unitSystem).toInt()}", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${UnitFormatter.speedKmhToUnit(60f, unitSystem).toInt()}", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // === Unit system section ===
        item { Spacer(modifier = Modifier.height(24.dp)) }

        item {
            Text(
                text = stringResource(R.string.section_unit_system),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                unitSystem = UnitSystem.METRIC
                                UnitPreferences.set(context, UnitSystem.METRIC)
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = unitSystem == UnitSystem.METRIC,
                            onClick = {
                                unitSystem = UnitSystem.METRIC
                                UnitPreferences.set(context, UnitSystem.METRIC)
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                        Text(
                            text = stringResource(R.string.label_metric),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                unitSystem = UnitSystem.IMPERIAL
                                UnitPreferences.set(context, UnitSystem.IMPERIAL)
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = unitSystem == UnitSystem.IMPERIAL,
                            onClick = {
                                unitSystem = UnitSystem.IMPERIAL
                                UnitPreferences.set(context, UnitSystem.IMPERIAL)
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                        Text(
                            text = stringResource(R.string.label_imperial),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        // === Orientation section ===
        item { Spacer(modifier = Modifier.height(24.dp)) }

        item {
            Text(
                text = stringResource(R.string.section_orientation),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                orientationPref = Orientation.AUTOMATIC
                                OrientationPreferences.set(context, Orientation.AUTOMATIC)
                                (context as? Activity)?.requestedOrientation =
                                    ActivityInfo.SCREEN_ORIENTATION_USER
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = orientationPref == Orientation.AUTOMATIC,
                            onClick = {
                                orientationPref = Orientation.AUTOMATIC
                                OrientationPreferences.set(context, Orientation.AUTOMATIC)
                                (context as? Activity)?.requestedOrientation =
                                    ActivityInfo.SCREEN_ORIENTATION_USER
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                        Text(
                            text = stringResource(R.string.orientation_automatic),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                orientationPref = Orientation.LANDSCAPE
                                OrientationPreferences.set(context, Orientation.LANDSCAPE)
                                (context as? Activity)?.requestedOrientation =
                                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = orientationPref == Orientation.LANDSCAPE,
                            onClick = {
                                orientationPref = Orientation.LANDSCAPE
                                OrientationPreferences.set(context, Orientation.LANDSCAPE)
                                (context as? Activity)?.requestedOrientation =
                                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                        Text(
                            text = stringResource(R.string.label_landscape),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                orientationPref = Orientation.PORTRAIT
                                OrientationPreferences.set(context, Orientation.PORTRAIT)
                                (context as? Activity)?.requestedOrientation =
                                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = orientationPref == Orientation.PORTRAIT,
                            onClick = {
                                orientationPref = Orientation.PORTRAIT
                                OrientationPreferences.set(context, Orientation.PORTRAIT)
                                (context as? Activity)?.requestedOrientation =
                                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                        Text(
                            text = stringResource(R.string.label_portrait),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        // === Appearance section ===
        item { Spacer(modifier = Modifier.height(24.dp)) }

        item {
            Text(
                text = stringResource(R.string.section_appearance),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }

        item {
            var themeMode by remember { mutableStateOf(ThemePreferences.get(context)) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                ThemePreferences.set(context, ThemePreferences.ThemeMode.SYSTEM)
                                (context as? Activity)?.recreate()
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = themeMode == ThemePreferences.ThemeMode.SYSTEM,
                            onClick = {
                                ThemePreferences.set(context, ThemePreferences.ThemeMode.SYSTEM)
                                (context as? Activity)?.recreate()
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                        Text(
                            text = stringResource(R.string.theme_system),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                ThemePreferences.set(context, ThemePreferences.ThemeMode.LIGHT)
                                (context as? Activity)?.recreate()
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = themeMode == ThemePreferences.ThemeMode.LIGHT,
                            onClick = {
                                ThemePreferences.set(context, ThemePreferences.ThemeMode.LIGHT)
                                (context as? Activity)?.recreate()
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                        Text(
                            text = stringResource(R.string.theme_light),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                ThemePreferences.set(context, ThemePreferences.ThemeMode.DARK)
                                (context as? Activity)?.recreate()
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = themeMode == ThemePreferences.ThemeMode.DARK,
                            onClick = {
                                ThemePreferences.set(context, ThemePreferences.ThemeMode.DARK)
                                (context as? Activity)?.recreate()
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                        Text(
                            text = stringResource(R.string.theme_dark),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        // === Burn-in Protection section ===
        item { Spacer(modifier = Modifier.height(24.dp)) }

        item {
            Text(
                text = stringResource(R.string.section_burnin),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }

        item {
            val burnInEnabled = remember { mutableStateOf(BurnInPreferences.isEnabled(context)) }
            val dimmingEnabled = remember { mutableStateOf(BurnInPreferences.isDimmingEnabled(context)) }
            val dimTextEnabled = remember { mutableStateOf(BurnInPreferences.isDimTextEnabled(context)) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.label_burnin_enable),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(R.string.label_burnin_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = burnInEnabled.value,
                            onCheckedChange = { checked ->
                                burnInEnabled.value = checked
                                BurnInPreferences.setEnabled(context, checked)
                                (context as? Activity)?.recreate()
                            },
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.label_burnin_dim),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(R.string.label_burnin_dim_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = dimmingEnabled.value,
                            onCheckedChange = { checked ->
                                dimmingEnabled.value = checked
                                BurnInPreferences.setDimmingEnabled(context, checked)
                            },
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.label_burnin_dim_text),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(R.string.label_burnin_dim_text_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = dimTextEnabled.value,
                            onCheckedChange = { checked ->
                                dimTextEnabled.value = checked
                                BurnInPreferences.setDimTextEnabled(context, checked)
                                (context as? Activity)?.recreate()
                            },
                        )
                    }
                }
            }
        }

        // === Font Size section ===
        item { Spacer(modifier = Modifier.height(24.dp)) }

        item {
            Text(
                text = stringResource(R.string.section_font_size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }

        item {
            var fontScale by remember { mutableStateOf(FontSizePreferences.getFontScale(context)) }

            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.label_font_size),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "${String.format("%.1f", fontScale)}x",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = fontScale,
                        onValueChange = { newScale ->
                            fontScale = newScale
                            FontSizePreferences.setFontScale(context, newScale)
                        },
                        valueRange = 0.7f..1.5f,
                        steps = 15,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("0.7x", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("1.5x", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // === Language section ===
        item { Spacer(modifier = Modifier.height(24.dp)) }

        item {
            Text(
                text = stringResource(R.string.section_language),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }

        item {
            var lang by remember { mutableStateOf(LanguagePreferences.get(context)) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                LanguagePreferences.set(context, "")
                                (context as? Activity)?.recreate()
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = lang == "",
                            onClick = {
                                LanguagePreferences.set(context, "")
                                (context as? Activity)?.recreate()
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                        Text(
                            text = stringResource(R.string.label_language_system),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                LanguagePreferences.set(context, "pt")
                                (context as? Activity)?.recreate()
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = lang == "pt",
                            onClick = {
                                LanguagePreferences.set(context, "pt")
                                (context as? Activity)?.recreate()
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                        Text(
                            text = stringResource(R.string.label_language_pt),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                LanguagePreferences.set(context, "en")
                                (context as? Activity)?.recreate()
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = lang == "en",
                            onClick = {
                                LanguagePreferences.set(context, "en")
                                (context as? Activity)?.recreate()
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                        Text(
                            text = stringResource(R.string.label_language_en),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        // === GraphHopper section ===
        item { Spacer(modifier = Modifier.height(24.dp)) }

        item {
            Text(
                text = "Rotas GraphHopper",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }

        item {
            var apiKey by remember { mutableStateOf(GraphHopperPreferences.getApiKey(context)) }
            var saved by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Chave da API para navega\u00E7\u00E3o A\u2192B (graphhopper.com)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it; saved = false },
                        label = { Text("GraphHopper API Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            GraphHopperPreferences.setApiKey(context, apiKey)
                            saved = true
                        },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text(if (saved) "Salvo \u2713" else "Salvar")
                    }
                }
            }
        }

        // === Offline maps section ===
        item { Spacer(modifier = Modifier.height(24.dp)) }

        item {
            Text(
                text = stringResource(R.string.section_offline_maps),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }

        if (offlineMaps.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.label_no_maps),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        } else {
            items(offlineMaps, key = { it.id }) { map ->
                OfflineMapCard(
                    map = map,
                    onDelete = {
                        scope.launch {
                            OfflineMapManager.deleteMap(context, map.id)
                            refreshTrigger++
                        }
                    },
                )
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            Button(
                onClick = { showCitySearch = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Text(stringResource(R.string.btn_download_city))
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        // === Backup & Restore section ===
        item { Spacer(modifier = Modifier.height(24.dp)) }

        item {
            Text(
                text = stringResource(R.string.section_backup_restore),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val backupLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.CreateDocument("application/json"),
                    ) { uri ->
                        if (uri == null) return@rememberLauncherForActivityResult
                        scope.launch {
                            try {
                                val json = BackupManager.export(context)
                                context.contentResolver.openOutputStream(uri)?.use { os ->
                                    os.write(json.toByteArray(Charsets.UTF_8))
                                }
                                Toast.makeText(context, context.getString(R.string.toast_backup_done), Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "${context.getString(R.string.toast_export_error)}: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                    val restoreLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenDocument(),
                    ) { uri ->
                        if (uri == null) return@rememberLauncherForActivityResult
                        scope.launch {
                            try {
                                val json = context.contentResolver.openInputStream(uri)?.use { is_
                                    -> is_.bufferedReader(Charsets.UTF_8).readText()
                                } ?: throw IllegalStateException("Cannot read file")
                                BackupManager.import(context, json).getOrThrow()
                                Toast.makeText(context, context.getString(R.string.toast_restore_done), Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, context.getString(R.string.toast_restore_error, e.message), Toast.LENGTH_LONG).show()
                            }
                        }
                    }

                    Text(
                        text = stringResource(R.string.label_backup_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            backupLauncher.launch("biketrackd_backup.json")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(stringResource(R.string.btn_backup))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.label_restore_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            restoreLauncher.launch(arrayOf("application/json", "*/*"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Text(stringResource(R.string.btn_restore))
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    if (showCitySearch) {
        CitySearchDialog(
            onCitySelected = { city ->
                showCitySearch = false
                downloadMapForCity(context, scope, city)
            },
            onDismiss = { showCitySearch = false },
        )
    }

    DownloadDialog(
        progress = downloadProgress,
        onDismiss = {
            MapOfflineManager.resetProgress()
            refreshTrigger++
        },
    )
}

@Composable
private fun OfflineMapCard(
    map: OfflineMapInfo,
    onDelete: () -> Unit,
) {
    val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(Date(map.createdAt))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = map.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${map.fileSizeDisplay} — ${map.tileCountDisplay} tiles — $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Zoom ${map.minZoom}-${map.maxZoom} · Raio ${map.radiusKm}km",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.desc_delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

private fun downloadMapForCity(
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
    city: CityResult,
) {
    scope.launch {
        MapOfflineManager.download(
            context = context,
            name = city.displayName.split(",").first().trim(),
            centerLat = city.lat,
            centerLon = city.lon,
            radiusKm = 40,
        )
    }
}


