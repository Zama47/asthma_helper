package io.github.zama47.asthmahelper.ui.breathing

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.zama47.asthmahelper.domain.export.DailyPeakFlow
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.ceil
import kotlin.math.max

/**
 * Диаграмма утро/вечер по дням: два столбика на день (утренний и вечерний замер).
 * Помогает увидеть суточный разброс ПСВ — ключевой признак нестабильности бронхов.
 */
@Composable
fun PeakFlowBarsChart(modifier: Modifier = Modifier, days: List<DailyPeakFlow>) {
    val colorScheme = MaterialTheme.colorScheme
    val locale = LocalConfiguration.current.locales[0]
    val density = LocalDensity.current
    val axisColor = colorScheme.onSurfaceVariant
    val gridColor = colorScheme.onSurfaceVariant.copy(alpha = 0.10f)
    val morningColor = colorScheme.primary
    val eveningColor = colorScheme.tertiary
    val labelStyle = MaterialTheme.typography.labelSmall

    val labelPaint = remember(axisColor, density) {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = axisColor.toArgb()
            textSize = with(density) { 11.sp.toPx() }
            textAlign = android.graphics.Paint.Align.RIGHT
        }
    }

    val dataMax = days.flatMap { listOfNotNull(it.morning, it.evening) }.maxOrNull() ?: 0f
    val maxY = ceil(max(600f, dataMax) / 100f) * 100f
    val dateFormat = remember(locale) { SimpleDateFormat("dd.MM", locale) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colorScheme.surfaceContainerHighest, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Утро и вечер по дням", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
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
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawText(text, plotLeft - 8.dp.toPx(), yPos - labelHalf, labelPaint)
                        }
                        gridValue += step
                    }

                    if (days.isNotEmpty()) {
                        val groupWidth = plotWidth / days.size
                        val barWidth = groupWidth * 0.28f
                        val corner = 4.dp.toPx()

                        days.forEachIndexed { index, day ->
                            val groupStart = plotLeft + index * groupWidth
                            val morningLeft = groupStart + groupWidth * 0.22f
                            val eveningLeft = morningLeft + barWidth + 2.dp.toPx()

                            day.morning?.let { value ->
                                val barHeight = plotBottom - yFor(value)
                                if (barHeight > 0f) {
                                    drawRoundRect(
                                        color = morningColor,
                                        topLeft = Offset(morningLeft, yFor(value)),
                                        size = Size(barWidth, barHeight),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner)
                                    )
                                }
                            }
                            day.evening?.let { value ->
                                val barHeight = plotBottom - yFor(value)
                                if (barHeight > 0f) {
                                    drawRoundRect(
                                        color = eveningColor,
                                        topLeft = Offset(eveningLeft, yFor(value)),
                                        size = Size(barWidth, barHeight),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner)
                                    )
                                }
                            }
                        }
                    }
                }

                if (days.isEmpty()) {
                    Text(
                        text = "Нет данных за период",
                        modifier = Modifier.align(Alignment.Center),
                        style = labelStyle,
                        color = axisColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.width(44.dp))
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween) {
                    if (days.isNotEmpty()) {
                        Text(
                            text = dateFormat.format(Date(days.first().dayMillis)),
                            style = labelStyle,
                            color = axisColor
                        )
                    }
                    if (days.size > 1) {
                        Text(
                            text = dateFormat.format(Date(days.last().dayMillis)),
                            style = labelStyle,
                            color = axisColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                LegendDot(color = morningColor, text = "утро", axisColor = axisColor)
                Spacer(modifier = Modifier.width(16.dp))
                LegendDot(color = eveningColor, text = "вечер", axisColor = axisColor)
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, text: String, axisColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, style = MaterialTheme.typography.labelSmall, color = axisColor)
    }
}
