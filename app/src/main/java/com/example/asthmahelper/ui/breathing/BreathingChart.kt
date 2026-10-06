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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.asthmahelper.domain.model.BreathingNorm
import kotlin.math.ceil
import kotlin.math.max

@Composable
fun BreathingChartWidget(
    modifier: Modifier = Modifier,
    measurements: List<Pair<Long, Float>>,
    norm: BreathingNorm? = null,
    defaultNorm: BreathingNorm = BreathingNorm(350f, 450f, 200),
    onNormChanged: (BreathingNorm) -> Unit
) {
    var currentNorm by remember(norm) { mutableStateOf(norm ?: defaultNorm) }
    var showConfigDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Button(
            onClick = { showConfigDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Настроить нормы")
        }

        Spacer(modifier = Modifier.height(8.dp))

        ChartContainer(
            modifier = Modifier.fillMaxWidth(),
            measurements = measurements,
            norm = currentNorm
        )
    }

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
    val colorScheme = MaterialTheme.colorScheme
    val containerColor = colorScheme.surfaceContainerHighest
    val axisColor = colorScheme.onSurfaceVariant
    val gridColor = colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
    val dataColor = colorScheme.primary
    val labelStyle = MaterialTheme.typography.labelSmall

    val dataMax = measurements.maxOfOrNull { it.second } ?: 0f
    val maxY = ceil(max(600f, max(norm.maxNormal, dataMax)) / 100f) * 100f
    val midY = maxY / 2f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(containerColor, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .width(34.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "${maxY.toInt()}", style = labelStyle, color = axisColor)
                Text(text = "${midY.toInt()}", style = labelStyle, color = axisColor)
                Text(text = "0", style = labelStyle, color = axisColor)
            }

            Canvas(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
            ) {
                drawLine(
                    color = axisColor,
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = 1f
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val minDisplayValue = 0f
                    val displayRange = maxY - minDisplayValue

                    fun yFor(value: Float): Float =
                        size.height * (1 - (value - minDisplayValue) / displayRange)

                    var gridValue = 100f
                    while (gridValue < maxY) {
                        val yPos = yFor(gridValue)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, yPos),
                            end = Offset(size.width, yPos),
                            strokeWidth = 1f
                        )
                        gridValue += 100f
                    }

                    drawLine(
                        color = Zone.RED.color,
                        start = Offset(0f, yFor(norm.redZoneThreshold)),
                        end = Offset(size.width, yFor(norm.redZoneThreshold)),
                        strokeWidth = 2.dp.toPx()
                    )

                    drawLine(
                        color = Zone.YELLOW.color,
                        start = Offset(0f, yFor(norm.minNormal)),
                        end = Offset(size.width, yFor(norm.minNormal)),
                        strokeWidth = 2.dp.toPx()
                    )

                    drawLine(
                        color = Zone.GREEN.color,
                        start = Offset(0f, yFor(norm.maxNormal)),
                        end = Offset(size.width, yFor(norm.maxNormal)),
                        strokeWidth = 2.dp.toPx()
                    )

                    when {
                        measurements.isEmpty() -> Unit
                        measurements.size == 1 -> {
                            drawCircle(
                                color = dataColor,
                                radius = 5.dp.toPx(),
                                center = Offset(size.width / 2f, yFor(measurements.first().second))
                            )
                        }
                        else -> {
                            val step = size.width / (measurements.size - 1)
                            for (i in 0 until measurements.size - 1) {
                                val startX = i * step
                                val endX = (i + 1) * step
                                val startY = yFor(measurements[i].second)
                                val endY = yFor(measurements[i + 1].second)

                                drawLine(
                                    color = dataColor,
                                    start = Offset(startX, startY),
                                    end = Offset(endX, endY),
                                    strokeWidth = 2.dp.toPx()
                                )

                                drawCircle(
                                    color = dataColor,
                                    radius = 5.dp.toPx(),
                                    center = Offset(startX, startY)
                                )
                            }

                            drawCircle(
                                color = dataColor,
                                radius = 5.dp.toPx(),
                                center = Offset(
                                    (measurements.size - 1) * step,
                                    yFor(measurements.last().second)
                                )
                            )
                        }
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
