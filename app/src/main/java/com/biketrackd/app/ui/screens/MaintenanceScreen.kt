package com.biketrackd.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.biketrackd.app.R
import com.biketrackd.app.data.AppDatabase
import com.biketrackd.app.data.MaintenancePart
import com.biketrackd.app.data.UnitPreferences
import com.biketrackd.app.data.UnitFormatter
import kotlinx.coroutines.launch

private val componentTypes: List<Pair<String, Int>> = listOf(
    "TIRE" to R.string.type_tire,
    "CHAIN" to R.string.type_chain,
    "BRAKE_PAD" to R.string.type_brake_pad,
    "SERVICE" to R.string.type_service,
    "OTHER" to R.string.type_other,
)

private fun componentIcon(type: String): ImageVector = when (type) {
    "TIRE" -> Icons.Default.Settings
    "CHAIN" -> Icons.Default.Build
    "BRAKE_PAD" -> Icons.Default.Warning
    else -> Icons.Default.Build
}

private fun wearFraction(part: MaintenancePart): Float {
    if (part.lifespanKm <= 0f) return 0f
    return (part.usedKm / part.lifespanKm).coerceIn(0f, 1f)
}

private fun wearColor(fraction: Float): Color = when {
    fraction >= 0.9f -> Color(0xFFE53935)
    fraction >= 0.8f -> Color(0xFFFB8C00)
    fraction >= 0.6f -> Color(0xFFFDD835)
    else -> Color(0xFF43A047)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val partDao = remember { db.maintenancePartDao() }
    val bikeDao = remember { db.bikeDao() }
    val scope = rememberCoroutineScope()

    val allParts by partDao.getAllPartsFlow().collectAsState(initial = emptyList())
    val allBikes by bikeDao.getAllFlow().collectAsState(initial = emptyList())
    val unitSystem by remember { mutableStateOf(UnitPreferences.get(context)) }
    val wornCount = remember(allParts) {
        allParts.count { it.lifespanKm > 0f && it.usedKm >= it.lifespanKm * 0.8f }
    }

    var showDialog by remember { mutableStateOf(false) }
    var editingPart by remember { mutableStateOf<MaintenancePart?>(null) }
    var partToDelete by remember { mutableStateOf<MaintenancePart?>(null) }

    val bikePartsMap = remember(allParts, allBikes) {
        allBikes.associate { bike ->
            bike.id to (bike to allParts.filter { it.bikeId == bike.id })
        }
    }

    if (partToDelete != null) {
        AlertDialog(
            onDismissRequest = { partToDelete = null },
            title = { Text(stringResource(R.string.dialog_delete_part_title)) },
            text = { Text(stringResource(R.string.dialog_delete_part_message, partToDelete!!.name)) },
            confirmButton = {
                TextButton(onClick = {
                    val id = partToDelete?.id ?: return@TextButton
                    scope.launch {
                        partDao.deleteById(id)
                        partToDelete = null
                    }
                }) { Text(stringResource(R.string.btn_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { partToDelete = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            },
        )
    }

    if (showDialog || editingPart != null) {
        val existing = editingPart
        var selectedBikeId by remember {
            mutableStateOf(existing?.bikeId ?: allBikes.firstOrNull()?.id ?: 0L)
        }
        var name by remember { mutableStateOf(existing?.name ?: "") }
        var selectedType by remember { mutableStateOf(existing?.componentType ?: "TIRE") }
        var lifespanStr by remember {
            mutableStateOf(if (existing != null) existing.lifespanKm.toInt().toString() else "")
        }
        var usedKmStr by remember {
            mutableStateOf(if (existing != null) existing.usedKm.toInt().toString() else "")
        }
        var notes by remember { mutableStateOf(existing?.notes ?: "") }
        var typeExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showDialog = false; editingPart = null },
            title = {
                Text(stringResource(
                    if (existing != null) R.string.title_edit_part else R.string.title_add_part,
                ))
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = allBikes.find { it.id == selectedBikeId }?.name ?: "",
                        onValueChange = {},
                        label = { Text(stringResource(R.string.label_bike)) },
                        readOnly = true,
                        enabled = existing == null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.label_part_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = typeExpanded,
                        onExpandedChange = { typeExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = stringResource(
                                componentTypes.find { it.first == selectedType }?.second
                                    ?: R.string.type_other,
                            ),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.label_part_type)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                        )
                        ExposedDropdownMenu(
                            expanded = typeExpanded,
                            onDismissRequest = { typeExpanded = false },
                        ) {
                            componentTypes.forEach { (type, resId) ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(resId)) },
                                    onClick = { selectedType = type; typeExpanded = false },
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = lifespanStr,
                        onValueChange = { lifespanStr = it.filter { c -> c.isDigit() }.take(6) },
                        label = { Text(stringResource(R.string.label_lifespan_km)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = usedKmStr,
                        onValueChange = { usedKmStr = it.filter { c -> c.isDigit() }.take(6) },
                        label = { Text(stringResource(R.string.label_used_km)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(stringResource(R.string.label_bike_notes)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val lifespan = lifespanStr.toIntOrNull()
                    val usedKm = usedKmStr.toFloatOrNull()?.coerceAtLeast(0f) ?: 0f
                    if (name.isBlank() || lifespan == null) return@TextButton
                    scope.launch {
                        val part = MaintenancePart(
                            id = existing?.id ?: 0,
                            bikeId = selectedBikeId,
                            name = name.trim(),
                            componentType = selectedType,
                            lifespanKm = lifespan.toFloat(),
                            usedKm = if (existing != null) existing.usedKm else usedKm,
                            notes = notes.trim(),
                        )
                        if (existing != null) partDao.update(part)
                        else partDao.insert(part)
                        showDialog = false
                        editingPart = null
                    }
                }) { Text(stringResource(R.string.btn_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false; editingPart = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            },
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.desc_add_part),
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                Text(
                    text = stringResource(R.string.section_maintenance),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }

            if (wornCount > 0) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                            Text(
                                text = stringResource(R.string.warning_worn_parts, wornCount),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            if (allParts.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.label_no_parts),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
            }

            bikePartsMap.forEach { (_, pair) ->
                val (bike, parts) = pair
                if (parts.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = bike.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    items(parts, key = { it.id }) { part ->
                        PartCard(
                            part = part,
                            unitSystem = unitSystem,
                            onEdit = { editingPart = part },
                            onDelete = { partToDelete = part },
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun PartCard(
    part: MaintenancePart,
    unitSystem: UnitPreferences.UnitSystem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val fraction = wearFraction(part)
    val color = wearColor(fraction)
    val pct = (fraction * 100).toInt().coerceIn(0, 100)

    val useImperial = unitSystem == UnitPreferences.UnitSystem.IMPERIAL
    val factor = if (useImperial) 0.621371f else 1f
    val unitLabel = if (useImperial) "mi" else "km"
    val usedDisplay = String.format("%.0f", part.usedKm * factor)
    val lifespanDisplay = String.format("%.0f", part.lifespanKm * factor)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onEdit() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    componentIcon(part.componentType),
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = part.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "$usedDisplay $unitLabel / $lifespanDisplay $unitLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "$pct%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.desc_delete),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(color),
                )
            }
        }
    }
}
