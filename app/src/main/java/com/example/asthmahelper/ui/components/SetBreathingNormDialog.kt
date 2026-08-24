package com.example.asthmahelper.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import com.example.asthmahelper.domain.usecase.SetBreathingNormUseCase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetBreathingNormDialog(
    onDismiss: () -> Unit,
    onSave: (Float, Float, Int) -> Unit,
    currentMin: Float? = null,
    currentMax: Float? = null,
    currentFormulaType: Int = 0
) {
    var minValue by remember { mutableStateOf(currentMin?.toString() ?: "") }
    var maxValue by remember { mutableStateOf(currentMax?.toString() ?: "") }
    var formulaType by remember { mutableStateOf(currentFormulaType) }
    var expanded by remember { mutableStateOf(false) }
    var age by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }

    val formulaTypes = listOf(
        "Ручной ввод" to 0,
        "По возрасту и росту" to 1
    )

    val minParsed = minValue.replace(',', '.').toFloatOrNull()
    val maxParsed = maxValue.replace(',', '.').toFloatOrNull()
    val ageParsed = age.toIntOrNull()
    val heightParsed = height.toFloatOrNull()

    val manualValid = minParsed != null && maxParsed != null &&
        minParsed > 0f && maxParsed > minParsed
    val formulaValid = ageParsed != null && ageParsed in 1..119 &&
        heightParsed != null && heightParsed >= 80f && heightParsed <= 250f
    val canSave = if (formulaType == 0) manualValid else formulaValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Настройка нормы дыхания") },
        text = {
            Column {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = formulaTypes.first { it.second == formulaType }.first,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Способ расчёта") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        formulaTypes.forEach { (name, type) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    formulaType = type
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (formulaType == 0) {
                    OutlinedTextField(
                        value = minValue,
                        onValueChange = { minValue = it },
                        label = { Text("Минимальное значение (л/мин)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = maxValue,
                        onValueChange = { maxValue = it },
                        label = { Text("Максимальное значение (л/мин)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                } else {
                    OutlinedTextField(
                        value = age,
                        onValueChange = { age = it },
                        label = { Text("Возраст (лет)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = height,
                        onValueChange = { height = it },
                        label = { Text("Рост (см)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    if (formulaValid) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val estimated =
                            SetBreathingNormUseCase.calculateByAgeAndHeight(ageParsed!!, heightParsed!!)
                        Text(
                            "Расчётная норма: ${estimated.first.toInt()}–${estimated.second.toInt()} л/мин"
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = canSave,
                onClick = {
                    if (formulaType == 0) {
                        onSave(minParsed!!, maxParsed!!, 0)
                    } else {
                        val estimated = SetBreathingNormUseCase.calculateByAgeAndHeight(
                            ageParsed!!,
                            heightParsed!!
                        )
                        onSave(estimated.first, estimated.second, 1)
                    }
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
