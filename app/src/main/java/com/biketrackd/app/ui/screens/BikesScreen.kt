package com.biketrackd.app.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.biketrackd.app.R
import com.biketrackd.app.data.AppDatabase
import com.biketrackd.app.data.Bike
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun BikesScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.getInstance(context).bikeDao() }
    val scope = rememberCoroutineScope()

    val bikes by dao.getAllFlow().collectAsState(initial = emptyList())

    var showDialog by remember { mutableStateOf(false) }
    var editingBike by remember { mutableStateOf<Bike?>(null) }
    var bikeToDelete by remember { mutableStateOf<Bike?>(null) }

    // Delete confirmation
    if (bikeToDelete != null) {
        AlertDialog(
            onDismissRequest = { bikeToDelete = null },
            title = { Text(stringResource(R.string.dialog_delete_bike_title)) },
            text = { Text(stringResource(R.string.dialog_delete_bike_message, bikeToDelete!!.name)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        dao.deleteById(bikeToDelete!!.id)
                        bikeToDelete = null
                    }
                }) {
                    Text(stringResource(R.string.btn_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { bikeToDelete = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            },
        )
    }

    // Add / Edit dialog
    if (showDialog || editingBike != null) {
        val existing = editingBike
        var name by remember { mutableStateOf(existing?.name ?: "") }
        var model by remember { mutableStateOf(existing?.model ?: "") }
        var type by remember { mutableStateOf(existing?.type ?: "") }
        var acquisitionDate by remember {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            mutableStateOf(if (existing != null && existing.acquisitionDate > 0) sdf.format(Date(existing.acquisitionDate)) else "")
        }
        var notes by remember { mutableStateOf(existing?.notes ?: "") }
        var isDefault by remember { mutableStateOf(existing?.isDefault ?: false) }

        val datePickerClickListener = {
            val cal = Calendar.getInstance()
            if (acquisitionDate.isNotBlank()) {
                try {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    sdf.parse(acquisitionDate)?.let { cal.time = it }
                } catch (_: Exception) {}
            }
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    acquisitionDate = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH),
            ).show()
        }

        AlertDialog(
            onDismissRequest = {
                showDialog = false
                editingBike = null
            },
            title = { Text(stringResource(if (existing != null) R.string.title_edit_bike else R.string.title_add_bike)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.label_bike_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = { Text(stringResource(R.string.label_bike_model)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = type,
                        onValueChange = { type = it },
                        label = { Text(stringResource(R.string.label_bike_type)) },
                        placeholder = { Text(stringResource(R.string.hint_bike_type)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = acquisitionDate,
                        onValueChange = {},
                        label = { Text(stringResource(R.string.label_bike_date)) },
                        placeholder = { Text(stringResource(R.string.hint_bike_date)) },
                        enabled = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { datePickerClickListener() },
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
                    if (name.isBlank()) return@TextButton
                    scope.launch {
                        val dateMillis = try {
                            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            sdf.parse(acquisitionDate)?.time ?: 0L
                        } catch (_: Exception) { 0L }

                        if (isDefault) dao.clearDefault()

                        val bike = Bike(
                            id = existing?.id ?: 0,
                            name = name.trim(),
                            model = model.trim(),
                            type = type.trim(),
                            acquisitionDate = dateMillis,
                            notes = notes.trim(),
                            isDefault = isDefault,
                        )
                        if (existing != null) dao.update(bike)
                        else dao.insert(bike)
                        showDialog = false
                        editingBike = null
                    }
                }) {
                    Text(stringResource(R.string.btn_save))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog = false
                    editingBike = null
                }) {
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
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.desc_add_bike))
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
                    text = stringResource(R.string.section_bikes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }

            if (bikes.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.label_no_bikes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
            }

            items(bikes, key = { it.id }) { bike ->
                BikeCard(
                    bike = bike,
                    onEdit = { editingBike = bike },
                    onDelete = { bikeToDelete = bike },
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun BikeCard(
    bike: Bike,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val dateStr = if (bike.acquisitionDate > 0) sdf.format(Date(bike.acquisitionDate)) else ""

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onEdit() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (bike.isDefault)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = bike.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (bike.isDefault) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 4.dp),
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
            if (bike.model.isNotBlank() || bike.type.isNotBlank()) {
                Text(
                    text = listOfNotNull(
                        bike.model.ifBlank { null },
                        bike.type.ifBlank { null },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (dateStr.isNotBlank()) {
                Text(
                    text = stringResource(R.string.label_bike_since, dateStr),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        }
    }
}
