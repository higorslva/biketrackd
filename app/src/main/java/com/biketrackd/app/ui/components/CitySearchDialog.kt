package com.biketrackd.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.biketrackd.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

data class CityResult(
    val displayName: String,
    val lat: Double,
    val lon: Double,
)

@Composable
fun CitySearchDialog(
    onCitySelected: (CityResult) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val results = remember { mutableStateListOf<CityResult>() }
    var isLoading by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_city_search)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { q ->
                        query = q
                        searchJob?.cancel()
                        if (q.length >= 3) {
                            searchJob = scope.launch {
                                delay(500)
                                searchCity(q) { list ->
                                    results.clear()
                                    results.addAll(list)
                                    isLoading = false
                                }
                            }
                        } else {
                            results.clear()
                        }
                    },
                    label = { Text(stringResource(R.string.label_city_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }

                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(results) { city ->
                        Text(
                            text = city.displayName,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCitySelected(city) }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) }
        },
    )
}

private suspend fun searchCity(query: String, onResult: (List<CityResult>) -> Unit) {
    withContext(Dispatchers.IO) {
        try {
            val url = URL(
                "https://nominatim.openstreetmap.org/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}&format=json&limit=10"
            )
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "GPS-OSS/1.0")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.connect()

            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().readText()
                val json = JSONArray(body)
                val list = mutableListOf<CityResult>()
                for (i in 0 until json.length()) {
                    val item = json.getJSONObject(i)
                    list.add(
                        CityResult(
                            displayName = item.getString("display_name"),
                            lat = item.getDouble("lat"),
                            lon = item.getDouble("lon"),
                        )
                    )
                }
                withContext(Dispatchers.Main) {
                    onResult(list)
                }
            }
        } catch (_: Exception) {
            withContext(Dispatchers.Main) {
                onResult(emptyList())
            }
        }
    }
}
