package com.example.asthmahelper.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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

@Composable
fun AddMeasurementDialog(
    onDismiss: () -> Unit,
    onSave: (Float, String?) -> Unit
) {
    var value by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val parsedValue = value.replace(',', '.').toFloatOrNull()
    val canSave = parsedValue != null && parsedValue > 0f

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
                    isError = value.isNotBlank() && !canSave,
                    supportingText = {
                        if (value.isNotBlank() && !canSave) {
                            Text("Введите число больше нуля, например 450")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

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
                    onSave(parsedValue!!, note.takeIf { it.isNotBlank() })
                },
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Text(text = stringResource(R.string.save))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    )
}
