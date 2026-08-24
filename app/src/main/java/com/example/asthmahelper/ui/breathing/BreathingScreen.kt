package com.example.asthmahelper.ui.breathing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.asthmahelper.ui.components.SetBreathingNormDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class Period(val label: String, val days: Int) {
    DAY("День", 1),
    WEEK("Неделя", 7),
    MONTH("Месяц", 30),
    YEAR("Год", 365)
}

@Composable
fun BreathingScreen(navController: androidx.navigation.NavController) {
    val viewModel: BreathingViewModel = hiltViewModel()

    val measurements by viewModel.measurements.collectAsState()
    val norm by viewModel.breathingNorm.collectAsState()

    var selectedPeriod by remember { mutableStateOf(Period.WEEK) }
    var showNormDialog by remember { mutableStateOf(false) }

    val filtered = remember(measurements, selectedPeriod) {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -selectedPeriod.days)
        }.timeInMillis
        measurements.filter { it.timestamp >= calendar }
            .sortedBy { it.timestamp }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Дыхание",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Переключатель периодов
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Period.values().forEach { period ->
                FilterChip(
                    selected = selectedPeriod == period,
                    onClick = { selectedPeriod = period },
                    label = { Text(period.label) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        BreathingChart(
            measurements = filtered.map { it.timestamp to it.value },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        norm?.let {
            Text(
                text = "Норма: ${it.minNormal.toInt()}–${it.maxNormal.toInt()} л/мин",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { showNormDialog = true }) {
                Text("Настроить норму")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "История замеров",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(measurements.sortedByDescending { it.timestamp }) { measurement ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${measurement.value.toInt()} л/мин",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = SimpleDateFormat(
                                    "dd.MM.yyyy HH:mm",
                                    Locale.getDefault()
                                ).format(Date(measurement.timestamp)),
                                style = MaterialTheme.typography.bodySmall
                            )
                            measurement.note?.let {
                                Text(text = it, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        IconButton(onClick = { viewModel.deleteMeasurement(measurement) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Удалить",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNormDialog) {
        SetBreathingNormDialog(
            onDismiss = { showNormDialog = false },
            onSave = { min, max, formulaType ->
                viewModel.setBreathingNorm(min, max, formulaType)
                showNormDialog = false
            },
            currentMin = norm?.minNormal,
            currentMax = norm?.maxNormal,
            currentFormulaType = norm?.formulaType ?: 0
        )
    }
}
