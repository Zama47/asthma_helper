package com.example.asthmahelper.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.asthmahelper.R
import java.util.Calendar
import java.util.Locale

/**
 * Диалог добавления замера пикфлоуметрии на произвольную дату и время.
 * Пользователь вводит значение, дату (дд.мм.гггг) и время (ЧЧ:ММ).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMeasurementWithDateDialog(
    onDismiss: () -> Unit,
    onSave: (value: Float, timestamp: Long, note: String?) -> Unit
) {
    var value by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val now = Calendar.getInstance()
    var dateText by remember {
        mutableStateOf(
            String.format(
                Locale.getDefault(),
                "%02d.%02d.%04d",
                now.get(Calendar.DAY_OF_MONTH),
                now.get(Calendar.MONTH) + 1,
                now.get(Calendar.YEAR)
            )
        )
    }

    val timeState = rememberTimePickerState(
        initialHour = now.get(Calendar.HOUR_OF_DAY),
        initialMinute = now.get(Calendar.MINUTE),
        is24Hour = true
    )

    val parsedValue = value.replace(',', '.').toFloatOrNull()

    // Валидация даты дд.мм.гггг
    val dateValid = remember(dateText) {
        val parts = dateText.split(".")
        if (parts.size != 3) return@remember false
        val d = parts[0].toIntOrNull() ?: return@remember false
        val m = parts[1].toIntOrNull() ?: return@remember false
        val y = parts[2].toIntOrNull() ?: return@remember false
        d in 1..31 && m in 1..12 && y in 2000..2100
    }

    val canSave = parsedValue != null && parsedValue > 0f && dateValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.add_measurement)) },
        text = {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(text = stringResource(R.string.measurement_value)) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = value.isNotBlank() && parsedValue?.let { it <= 0f } != false,
                    supportingText = {
                        if (value.isNotBlank() && (parsedValue == null || parsedValue <= 0f)) {
                            Text("Введите число больше нуля, например 450")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Дата (дд.мм.гггг)") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = !dateValid,
                    supportingText = {
                        if (!dateValid) Text("Формат: 24.08.2026")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Время замера")
                TimePicker(state = timeState)

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(text = stringResource(R.string.measurement_note)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = canSave,
                onClick = {
                    val parts = dateText.split(".")
                    val calendar = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_MONTH, parts[0].toInt())
                        set(Calendar.MONTH, parts[1].toInt() - 1)
                        set(Calendar.YEAR, parts[2].toInt())
                        set(Calendar.HOUR_OF_DAY, timeState.hour)
                        set(Calendar.MINUTE, timeState.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    onSave(parsedValue!!, calendar.timeInMillis, note.takeIf { it.isNotBlank() })
                }
            ) {
                Text(text = stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    )
}
