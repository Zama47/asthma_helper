package com.example.asthmahelper.ui.medications

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import com.example.asthmahelper.domain.model.Medicine

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
            Text("Добавить приём")
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
            onConfirmCustom = { name, description, dose, hour, minute, days ->
                viewModel.addCustomMedicineWithSchedule(
                    name = name,
                    description = description,
                    dose = dose,
                    timeOfDayMinutes = hour * 60L + minute,
                    daysOfWeek = days
                )
                showAddDialog = false
            },
            onConfirmExisting = { selectedMedicineId, dose, hour, minute, days ->
                viewModel.addSchedule(
                    medicineId = selectedMedicineId,
                    dose = dose,
                    timeOfDayMinutes = hour * 60L + minute,
                    daysOfWeek = days
                )
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

/**
 * Диалог добавления приёма лекарства.
 * Три режима:
 *  - «Из списка» — выбор лекарства из уже добавленных;
 *  - «Новое» — ввод названия/описания нового лекарства;
 *  Оба режима + выбор дней недели + время.
 */
@Composable
private fun AddMedicineWithScheduleDialog(
    onDismiss: () -> Unit,
    existingMedicines: List<Medicine>,
    onConfirmCustom: (name: String, description: String, dose: String, hour: Int, minute: Int, days: String) -> Unit,
    onConfirmExisting: (medicineId: Long, dose: String, hour: Int, minute: Int, days: String) -> Unit
) {
    var mode by remember { mutableStateOf(if (existingMedicines.isEmpty()) MODE_NEW else MODE_EXISTING) }
    var selectedMedicineId by remember { mutableStateOf(existingMedicines.firstOrNull()?.id) }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dose by remember { mutableStateOf("") }
    var timeText by remember { mutableStateOf("08:00") }

    // Выбранные дни недели (по умолчанию — все)
    var selectedDays by remember { mutableStateOf(setOf(1, 2, 3, 4, 5, 6, 7)) }

    val timeValid = remember(timeText) {
        val parts = timeText.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: -1
        val m = parts.getOrNull(1)?.toIntOrNull() ?: -1
        h in 0..23 && m in 0..59
    }
    val canSave = when (mode) {
        MODE_EXISTING -> selectedMedicineId != null && dose.isNotBlank() && timeValid && selectedDays.isNotEmpty()
        else -> name.isNotBlank() && dose.isNotBlank() && timeValid && selectedDays.isNotEmpty()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый приём лекарства") },
        text = {
            Column {
                // Переключатель режима: существующее / новое
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = mode == MODE_EXISTING,
                        onClick = { mode = MODE_EXISTING },
                        label = { Text("Из списка") },
                        enabled = existingMedicines.isNotEmpty()
                    )
                    FilterChip(
                        selected = mode == MODE_NEW,
                        onClick = { mode = MODE_NEW },
                        label = { Text("Новое") }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                when (mode) {
                    MODE_EXISTING -> {
                        if (existingMedicines.isEmpty()) {
                            Text(
                                "Список лекарств пуст — добавьте новое.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            LazyColumn(modifier = Modifier.height(160.dp)) {
                                items(existingMedicines) { medicine ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedMedicineId = medicine.id }
                                            .padding(vertical = 2.dp)
                                    ) {
                                        RadioButton(
                                            selected = selectedMedicineId == medicine.id,
                                            onClick = { selectedMedicineId = medicine.id }
                                        )
                                        Text(
                                            text = if (medicine.description.isBlank()) {
                                                medicine.name
                                            } else {
                                                "${medicine.name} (${medicine.description})"
                                            },
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = dose,
                            onValueChange = { dose = it },
                            label = { Text("Дозировка (например, 1 таблетка)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    else -> {
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
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = timeText,
                    onValueChange = { timeText = it },
                    label = { Text("Время (ЧЧ:ММ)") },
                    isError = timeText.isNotBlank() && !timeValid,
                    supportingText = if (timeText.isNotBlank() && !timeValid) {
                        { Text("Формат: 08:30") }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text("Дни приёма", style = MaterialTheme.typography.titleSmall)

                // Чипы выбора дней недели
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DayChip("Пн", 1, selectedDays) { day, selected ->
                        selectedDays = if (selected) selectedDays + day else selectedDays - day
                    }
                    DayChip("Вт", 2, selectedDays) { day, selected ->
                        selectedDays = if (selected) selectedDays + day else selectedDays - day
                    }
                    DayChip("Ср", 3, selectedDays) { day, selected ->
                        selectedDays = if (selected) selectedDays + day else selectedDays - day
                    }
                    DayChip("Чт", 4, selectedDays) { day, selected ->
                        selectedDays = if (selected) selectedDays + day else selectedDays - day
                    }
                    DayChip("Пт", 5, selectedDays) { day, selected ->
                        selectedDays = if (selected) selectedDays + day else selectedDays - day
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DayChip("Сб", 6, selectedDays) { day, selected ->
                        selectedDays = if (selected) selectedDays + day else selectedDays - day
                    }
                    DayChip("Вс", 7, selectedDays) { day, selected ->
                        selectedDays = if (selected) selectedDays + day else selectedDays - day
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = canSave,
                onClick = {
                    val parts = timeText.split(":")
                    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
                    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                    val daysString = selectedDays.sorted().joinToString("")
                    when (mode) {
                        MODE_EXISTING -> selectedMedicineId?.let {
                            onConfirmExisting(it, dose, hour, minute, daysString)
                        }
                        else -> onConfirmCustom(name, description, dose, hour, minute, daysString)
                    }
                }
            ) {
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

@Composable
private fun DayChip(
    label: String,
    dayNumber: Int,
    selectedDays: Set<Int>,
    onToggle: (Int, Boolean) -> Unit
) {
    FilterChip(
        selected = dayNumber in selectedDays,
        onClick = { onToggle(dayNumber, dayNumber !in selectedDays) },
        label = { Text(label) }
    )
}

private const val MODE_EXISTING = 0
private const val MODE_NEW = 1
