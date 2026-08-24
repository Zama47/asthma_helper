package com.example.asthmahelper.ui.medications

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.asthmahelper.R

@Composable
fun MedicationsScreen(navController: androidx.navigation.NavController) {
    val viewModel: MedicationsViewModel = hiltViewModel()
    var showAddDialog by remember { mutableStateOf(false) }

    val medicines by viewModel.medicines.collectAsState()
    val schedules by viewModel.schedules.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.medications),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { showAddDialog = true }) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.padding(4.dp))
            Text("Добавить лекарство")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Расписание приёма", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (schedules.isEmpty()) {
            Text(
                "Расписание пустое. Добавьте лекарство и время приёма.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(schedules) { schedule ->
                val medicine = medicines.firstOrNull { it.id == schedule.medicineId }
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
                                text = medicine?.name ?: "Лекарство #${schedule.medicineId}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${formatMinutes(schedule.timeOfDay)} — ${schedule.dose}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = daysLabel(schedule.daysOfWeek),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        IconButton(onClick = { viewModel.deleteSchedule(schedule) }) {
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

    if (showAddDialog) {
        AddMedicineWithScheduleDialog(
            onDismiss = { showAddDialog = false },
            existingMedicines = medicines,
            onConfirmCustom = { name, description, dose, hour, minute ->
                viewModel.addCustomMedicineWithSchedule(
                    name = name,
                    description = description,
                    dose = dose,
                    timeOfDayMinutes = hour * 60L + minute,
                    daysOfWeek = "1234567"
                )
                showAddDialog = false
            },
            onConfirmExisting = { selectedMedicineId, dose, minutes ->
                viewModel.addSchedule(selectedMedicineId, dose, minutes, "1234567")
                showAddDialog = false
            }
        )
    }
}

private fun formatMinutes(msSinceMidnight: Long): String {
    val totalMinutes = msSinceMidnight / 60_000
    return String.format("%02d:%02d", totalMinutes / 60, totalMinutes % 60)
}

private fun daysLabel(daysOfWeek: String): String {
    if (daysOfWeek == "1234567") return "Каждый день"
    return daysOfWeek.mapNotNull { c ->
        when (c) {
            '1' -> "Пн"; '2' -> "Вт"; '3' -> "Ср"; '4' -> "Чт"
            '5' -> "Пт"; '6' -> "Сб"; '7' -> "Вс"
            else -> null
        }
    }.joinToString(", ")
}

@Composable
private fun AddMedicineWithScheduleDialog(
    onDismiss: () -> Unit,
    onConfirmCustom: (String, String, String, Int, Int) -> Unit,
    existingMedicines: List<com.example.asthmahelper.domain.model.Medicine>,
    onConfirmExisting: (Long, String, Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dose by remember { mutableStateOf("") }
    var timeText by remember { mutableStateOf("08:00") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новое лекарство") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = dose,
                    onValueChange = { dose = it },
                    label = { Text("Дозировка (например, 1 таблетка)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = timeText,
                    onValueChange = { timeText = it },
                    label = { Text("Время (ЧЧ:ММ)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val parts = timeText.split(":")
                val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
                val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                onConfirmCustom(name, description, dose, hour, minute)
            }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
