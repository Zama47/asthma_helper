package com.example.asthmahelper.ui.weather

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.asthmahelper.data.api.dto.GeocodingResult
import kotlinx.coroutines.launch

/**
 * Диалог поиска города (Open-Meteo Geocoding, без ключа).
 * [onSearch] возвращает null при ошибке сети и пустой список, если ничего не найдено.
 */
@Composable
fun CityPickerDialog(
    onDismiss: () -> Unit,
    onSearch: suspend (String) -> List<GeocodingResult>?,
    onCitySelected: (name: String, lat: Double, lon: Double) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<GeocodingResult>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }
    var networkError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val startSearch = {
        val trimmed = query.trim()
        if (trimmed.isNotEmpty() && !searching) {
            searching = true
            searched = true
            networkError = false
            scope.launch {
                val response = onSearch(trimmed)
                networkError = response == null
                results = response ?: emptyList()
                searching = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Выбор города",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Название города") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { startSearch() })
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = { startSearch() }, enabled = query.isNotBlank() && !searching) {
                        Text("Найти")
                    }
                    if (searching) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }

                when {
                    searching -> Unit
                    !searched -> Text(
                        text = "Введите название и нажмите «Найти».",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    networkError -> Text(
                        text = "Ошибка сети. Проверьте подключение к интернету.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    results.isEmpty() -> Text(
                        text = "Ничего не найдено.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    else -> Column(
                        modifier = Modifier
                            .heightIn(max = 260.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        results.forEach { result ->
                            TextButton(
                                onClick = {
                                    onCitySelected(result.name, result.latitude, result.longitude)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = buildString {
                                        append(result.name)
                                        result.admin1?.takeIf { it.isNotBlank() }?.let { append(", $it") }
                                        result.country?.takeIf { it.isNotBlank() }?.let { append(", $it") }
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                if (searched && !searching && results.isNotEmpty() && !networkError) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Погода и воздух будут загружаться для выбранного города.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
