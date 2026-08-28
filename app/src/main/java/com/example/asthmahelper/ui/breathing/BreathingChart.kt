package com.example.asthmahelper.ui.breathing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.asthmahelper.domain.model.BreathingNorm

@Composable
fun BreathingChartWidget(
    modifier: Modifier = Modifier,
    measurements: List<Pair<Long, Float>>,
    norm: BreathingNorm? = null,
    defaultNorm: BreathingNorm = BreathingNorm(350f, 450f, 200),
    onNormChanged: (BreathingNorm) -> Unit
) {
    var currentNorm by remember { mutableStateOf(norm ?: defaultNorm) }
    var showConfigDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
    ) {
        // Кнопка в начале без отступов
        Button(
            onClick = { showConfigDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Настроить нормы")
        }

        ChartContainer(
            modifier = Modifier.fillMaxWidth(),
            measurements = measurements,
            norm = currentNorm
        )
    }

    // Диалоговое окно для настройки норм
    if (showConfigDialog) {
        NormConfigDialog(
            norm = currentNorm,
            onNormChanged = { newNorm ->
                currentNorm = newNorm
                onNormChanged(newNorm)
            },
            onClose = { showConfigDialog = false }
        )
    }
}

@Composable
private fun NormConfigDialog(
    norm: BreathingNorm,
    onNormChanged: (BreathingNorm) -> Unit,
    onClose: () -> Unit
) {
    var minNormal by remember { mutableStateOf(norm.minNormal.toString()) }
    var maxNormal by remember { mutableStateOf(norm.maxNormal.toString()) }
    var redZoneThreshold by remember { mutableStateOf(norm.redZoneThreshold.toString()) }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Настройка норм") },
        text = {
            Column(modifier = Modifier.padding(8.dp)) {
                TextField(
                    value = minNormal,
                    onValueChange = { minNormal = it },
                    label = { Text("Нижняя граница жёлтой зоны (minNormal)") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = maxNormal,
                    onValueChange = { maxNormal = it },
                    label = { Text("Верхняя граница зелёной зоны (maxNormal)") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = redZoneThreshold,
                    onValueChange = { redZoneThreshold = it },
                    label = { Text("Порог красной зоны (redZoneThreshold)") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onNormChanged(
                        BreathingNorm(
                            minNormal.toFloatOrNull() ?: norm.minNormal,
                            maxNormal.toFloatOrNull() ?: norm.maxNormal,
                            (redZoneThreshold.toIntOrNull() ?: norm.redZoneThreshold).toInt()
                        )
                    )
                    onClose()
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            Button(onClick = onClose) {
                Text("Отмена")
            }
        }
    )
}

@Composable
private fun ChartContainer(
    modifier: Modifier = Modifier,
    measurements: List<Pair<Long, Float>>,
    norm: BreathingNorm
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(Color.LightGray, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // Цифры на оси Y (0, 300, 600) у левого края виджета
            Column(
                modifier = Modifier
                    .width(30.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "600",
                    fontSize = 12.sp
                )
                Text(
                    text = "300",
                    fontSize = 12.sp
                )
                Text(
                    text = "0",
                    fontSize = 12.sp
                )
            }

            // Линия оси Y
            Canvas(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
            ) {
                drawLine(
                    color = Color.Black,
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = 1f
                )
            }

            // Основной график
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val maxDisplayValue = 600f
                    val minDisplayValue = 0f
                    val displayRange = maxDisplayValue - minDisplayValue

                    // Отрисовка линий норм
                    drawLine(
                        color = Color.Green,
                        start = Offset(0f, size.height * (1 - (norm.maxNormal - minDisplayValue) / displayRange)),
                        end = Offset(size.width, size.height * (1 - (norm.maxNormal - minDisplayValue) / displayRange)),
                        strokeWidth = 2f
                    )

                    drawLine(
                        color = Color.Yellow,
                        start = Offset(0f, size.height * (1 - (norm.minNormal - minDisplayValue) / displayRange)),
                        end = Offset(size.width, size.height * (1 - (norm.minNormal - minDisplayValue) / displayRange)),
                        strokeWidth = 2f
                    )

                    drawLine(
                        color = Color.Red,
                        start = Offset(0f, size.height * (1 - (norm.redZoneThreshold - minDisplayValue) / displayRange)),
                        end = Offset(size.width, size.height * (1 - (norm.redZoneThreshold - minDisplayValue) / displayRange)),
                        strokeWidth = 2f
                    )

                    // Отрисовка дополнительных линий (100, 200, 400, 500)
                    listOf(100f, 200f, 400f, 500f).forEach { value ->
                        val yPos = size.height * (1 - (value - minDisplayValue) / displayRange)
                        drawLine(
                            color = Color.Gray,
                            start = Offset(0f, yPos),
                            end = Offset(size.width, yPos),
                            strokeWidth = 1f
                        )
                    }

                    // Отрисовка данных
                    if (measurements.isNotEmpty()) {
                        val step = size.width / (measurements.size - 1)
                        for (i in 0 until measurements.size - 1) {
                            val startX = i * step
                            val endX = (i + 1) * step
                            val startY = size.height * (1 - (measurements[i].second - minDisplayValue) / displayRange)
                            val endY = size.height * (1 - (measurements[i + 1].second - minDisplayValue) / displayRange)

                            drawLine(
                                color = Color.Blue,
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = 2f
                            )

                            // Добавление точек для каждого измерения
                            drawCircle(
                                color = Color.Blue,
                                radius = 4f,
                                center = Offset(startX, startY)
                            )
                        }

                        // Добавление последней точки
                        val lastX = (measurements.size - 1) * step
                        val lastY = size.height * (1 - (measurements.last().second - minDisplayValue) / displayRange)
                        drawCircle(
                            color = Color.Blue,
                            radius = 4f,
                            center = Offset(lastX, lastY)
                        )
                    }

                    // Отметки на оси Y (0, 300, 600)
                    listOf(0f, 300f, 600f).forEach { value ->
                        val yPos = size.height * (1 - (value - minDisplayValue) / displayRange)
                        drawLine(
                            color = Color.Black,
                            start = Offset(0f, yPos),
                            end = Offset(size.width, yPos),
                            strokeWidth = 0.5f
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BreathingChart(
    modifier: Modifier = Modifier,
    measurements: List<Pair<Long, Float>>,
    norm: BreathingNorm? = null,
    onNormChanged: (BreathingNorm) -> Unit
) {
    BreathingChartWidget(
        modifier = modifier,
        measurements = measurements,
        norm = norm,
        defaultNorm = BreathingNorm(350f, 450f, 200),
        onNormChanged = onNormChanged
    )
}