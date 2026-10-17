package com.example.asthmahelper.ui.breathing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.asthmahelper.domain.clinical.Zone
import com.example.asthmahelper.domain.model.BreathingNorm
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

@Composable
fun BreathingChartWidget(
    modifier: Modifier = Modifier,
    measurements: List<Pair<Long, Float>>,
    norm: BreathingNorm? = null,
    personalBest: Float? = null,
    percentBaseline: Float? = null,
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
            norm = currentNorm,
            personalBest = personalBest,
            percentBaseline = percentBaseline
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
private fun NormConfigDialog(norm: BreathingNorm, onNormChanged: (BreathingNorm) -> Unit, onClose: () -> Unit) {
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
    norm: BreathingNorm,
    personalBest: Float?,
    percentBaseline: Float?
) {
    val colorScheme = MaterialTheme.colorScheme
    val locale = LocalConfiguration.current.locales[0]
    val density = LocalDensity.current
    val axisColor = colorScheme.onSurfaceVariant
    val gridColor = colorScheme.onSurfaceVariant.copy(alpha = 0.10f)
    val dataColor = colorScheme.primary
    val bestColor = colorScheme.tertiary
    val labelStyle = MaterialTheme.typography.labelSmall

    val labelPaint = remember(axisColor, density) {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = axisColor.toArgb()
            textSize = with(density) { 11.sp.toPx() }
            textAlign = android.graphics.Paint.Align.RIGHT
        }
    }

    val dataMax = measurements.maxOfOrNull { it.second } ?: 0f
    val maxY = ceil(max(600f, max(norm.maxNormal, max(dataMax, personalBest ?: 0f))) / 100f) * 100f

    val dateFormat = remember(locale) { SimpleDateFormat("dd.MM", locale) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colorScheme.surfaceContainerHighest, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val gutter = 44.dp.toPx()
                    val topInset = 10.dp.toPx()
                    val bottomInset = 10.dp.toPx()
                    val plotLeft = gutter
                    val plotTop = topInset
                    val plotBottom = size.height - bottomInset
                    val plotWidth = size.width - gutter

                    fun yFor(value: Float): Float = plotBottom - (value / maxY) * (plotBottom - plotTop)

                    val baseline = percentBaseline ?: norm.maxNormal
                    val greenLower = min(baseline * 0.8f, maxY)
                    val yellowLower = min(
                        if (percentBaseline != null) baseline * 0.5f else norm.redZoneThreshold,
                        greenLower
                    )

                    drawRect(
                        color = Zone.GREEN.color.copy(alpha = 0.10f),
                        topLeft = Offset(plotLeft, yFor(maxY)),
                        size = Size(plotWidth, yFor(greenLower) - yFor(maxY))
                    )
                    drawRect(
                        color = Zone.YELLOW.color.copy(alpha = 0.10f),
                        topLeft = Offset(plotLeft, yFor(greenLower)),
                        size = Size(plotWidth, yFor(yellowLower) - yFor(greenLower))
                    )
                    drawRect(
                        color = Zone.RED.color.copy(alpha = 0.10f),
                        topLeft = Offset(plotLeft, yFor(yellowLower)),
                        size = Size(plotWidth, plotBottom - yFor(yellowLower))
                    )

                    val step = when {
                        maxY <= 300f -> 50f
                        maxY <= 600f -> 100f
                        maxY <= 1200f -> 200f
                        else -> 300f
                    }
                    val labelHalf = (labelPaint.descent() + labelPaint.ascent()) / 2f

                    var gridValue = 0f
                    while (gridValue <= maxY + 0.5f) {
                        val yPos = yFor(gridValue)
                        drawLine(
                            color = gridColor,
                            start = Offset(plotLeft, yPos),
                            end = Offset(size.width, yPos),
                            strokeWidth = 1f
                        )
                        val text = gridValue.toInt().toString()
                        val baseline = yPos - labelHalf
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawText(text, plotLeft - 8.dp.toPx(), baseline, labelPaint)
                        }
                        gridValue += step
                    }

                    drawLine(
                        color = Zone.GREEN.color.copy(alpha = 0.28f),
                        start = Offset(plotLeft, yFor(greenLower)),
                        end = Offset(size.width, yFor(greenLower)),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = Zone.RED.color.copy(alpha = 0.28f),
                        start = Offset(plotLeft, yFor(yellowLower)),
                        end = Offset(size.width, yFor(yellowLower)),
                        strokeWidth = 1f
                    )

                    if (personalBest != null && personalBest in 0f..maxY) {
                        val yBest = yFor(personalBest)
                        drawLine(
                            color = bestColor,
                            start = Offset(plotLeft, yBest),
                            end = Offset(size.width, yBest),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(8.dp.toPx(), 6.dp.toPx())
                            )
                        )
                    }

                    if (measurements.isNotEmpty()) {
                        val count = measurements.size
                        val stepX = if (count == 1) 0f else plotWidth / (count - 1)
                        fun xAt(index: Int): Float =
                            if (count == 1) plotLeft + plotWidth / 2f else plotLeft + index * stepX

                        val linePath = Path()
                        measurements.forEachIndexed { index, (_, value) ->
                            val x = xAt(index)
                            val y = yFor(value)
                            if (index == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
                        }

                        if (count > 1) {
                            val areaPath = Path().apply {
                                addPath(linePath)
                                lineTo(xAt(count - 1), plotBottom)
                                lineTo(xAt(0), plotBottom)
                                close()
                            }
                            drawPath(
                                path = areaPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(dataColor.copy(alpha = 0.28f), dataColor.copy(alpha = 0f)),
                                    startY = plotTop,
                                    endY = plotBottom
                                )
                            )
                        }

                        drawPath(
                            path = linePath,
                            color = dataColor,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        measurements.forEachIndexed { index, (_, value) ->
                            val center = Offset(xAt(index), yFor(value))
                            drawCircle(
                                color = colorScheme.surfaceContainerHighest,
                                radius = 5.dp.toPx(),
                                center = center
                            )
                            drawCircle(color = dataColor, radius = 3.dp.toPx(), center = center)
                        }
                    }
                }

                if (measurements.isEmpty()) {
                    Text(
                        text = "Нет замеров",
                        modifier = Modifier.align(Alignment.Center),
                        style = labelStyle,
                        color = axisColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.width(44.dp))
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (measurements.isNotEmpty()) {
                        Text(
                            text = dateFormat.format(Date(measurements.first().first)),
                            style = labelStyle,
                            color = axisColor
                        )
                    }
                    if (measurements.size > 1) {
                        Text(
                            text = dateFormat.format(Date(measurements.last().first)),
                            style = labelStyle,
                            color = axisColor
                        )
                    }
                }
            }

            if (personalBest != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(
                        modifier = Modifier
                            .width(20.dp)
                            .height(2.dp)
                    ) {
                        drawLine(
                            color = bestColor,
                            start = Offset(0f, size.height / 2f),
                            end = Offset(size.width, size.height / 2f),
                            strokeWidth = size.height,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f))
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Личный максимум: ${personalBest.toInt()} л/мин",
                        style = labelStyle,
                        color = axisColor
                    )
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
    personalBest: Float? = null,
    percentBaseline: Float? = null,
    onNormChanged: (BreathingNorm) -> Unit
) {
    BreathingChartWidget(
        modifier = modifier,
        measurements = measurements,
        norm = norm,
        personalBest = personalBest,
        percentBaseline = percentBaseline,
        defaultNorm = BreathingNorm(350f, 450f, 200),
        onNormChanged = onNormChanged
    )
}
