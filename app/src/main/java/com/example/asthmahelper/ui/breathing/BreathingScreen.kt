package com.example.asthmahelper.ui.breathing

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.asthmahelper.domain.clinical.DailyVariability
import com.example.asthmahelper.domain.clinical.VariabilityLevel
import com.example.asthmahelper.domain.clinical.Zone
import com.example.asthmahelper.domain.clinical.computeDailyVariability
import com.example.asthmahelper.domain.clinical.percentOfBest
import com.example.asthmahelper.domain.clinical.personalBest
import com.example.asthmahelper.domain.clinical.variabilityLevel
import com.example.asthmahelper.domain.clinical.zoneOf
import com.example.asthmahelper.domain.model.BreathingNorm
import com.example.asthmahelper.ui.components.AddMeasurementWithDateDialog
import com.example.asthmahelper.ui.components.SetBreathingNormDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private enum class Period(val label: String, val days: Int) {
    DAY("День", 1),
    WEEK("Неделя", 7),
    MONTH("Месяц", 30),
    YEAR("Год", 365)
}

@Composable
fun BreathingScreen(navController: androidx.navigation.NavController) {
    val viewModel: BreathingViewModel = hiltViewModel()
    val locale = LocalConfiguration.current.locales[0]

    val measurements by viewModel.measurements.collectAsState()
    val norm by viewModel.breathingNorm.collectAsState()

    var selectedPeriod by remember { mutableStateOf(Period.WEEK) }
    var showNormDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    val filtered = remember(measurements, selectedPeriod) {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -selectedPeriod.days)
        }.timeInMillis
        measurements.filter { it.timestamp >= calendar }
            .sortedBy { it.timestamp }
    }

    val variability = remember(filtered) { computeDailyVariability(filtered) }

    val personalBestValue = remember(measurements) { personalBest(measurements) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Дыхание",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Добавить замер")
                }
                OutlinedButton(onClick = { showNormDialog = true }) {
                    Text("Настроить норму")
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
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
        }

        item {
            BreathingChart(
                measurements = filtered.map { it.timestamp to it.value },
                norm = norm,
                personalBest = personalBestValue,
                onNormChanged = { newNorm -> viewModel.setBreathingNorm(newNorm) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            VariabilityCard(variability = variability)
        }

        if (norm != null) {
            item {
                ZoneLegend(norm = norm!!)
            }
        }

        item {
            Text(
                text = "История замеров",
                style = MaterialTheme.typography.titleMedium
            )
        }

        items(measurements.sortedByDescending { it.timestamp }) { measurement ->
            val sharePercent = percentOfBest(measurement.value, personalBestValue)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(zoneOf(measurement.value, norm).color, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${measurement.value.toInt()} л/мин" +
                                (sharePercent?.let { "  ·  ${it.roundToInt()}% от максимума" } ?: ""),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = SimpleDateFormat(
                                "dd.MM.yyyy HH:mm",
                                locale
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

    if (showAddDialog) {
        AddMeasurementWithDateDialog(
            onDismiss = { showAddDialog = false },
            onSave = { value, timestamp, note ->
                viewModel.addMeasurement(value, timestamp, note)
                showAddDialog = false
            }
        )
    }
}

/** Карточка с формулой суточной вариабельности (разброса) под графиком. */
@Composable
private fun VariabilityCard(variability: DailyVariability?) {
    val colorScheme = MaterialTheme.colorScheme
    val locale = LocalConfiguration.current.locales[0]

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Суточная вариабельность (разброса)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Главный маркер астмы: насколько сильно «скачут» показатели в течение суток.",
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        colorScheme.secondaryContainer,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = "Вариабельность (%) = (Вечернее значение − Утреннее значение) " +
                        "/ Максимальное значение за сутки × 100 %",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    color = colorScheme.onSecondaryContainer
                )
            }

            if (variability == null) {
                Text(
                    text = "Недостаточно данных: добавьте минимум два замера за одни сутки.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            } else {
                val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
                val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }

                Text(
                    text = dateFormat.format(Date(variability.dayMillis)),
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurfaceVariant
                )

                Row(modifier = Modifier.fillMaxWidth()) {
                    VariabilityValueCell(
                        label = "Утро",
                        value = variability.morning,
                        time = timeFormat.format(Date(variability.morningTime)),
                        modifier = Modifier.weight(1f)
                    )
                    VariabilityValueCell(
                        label = "Вечер",
                        value = variability.evening,
                        time = timeFormat.format(Date(variability.eveningTime)),
                        modifier = Modifier.weight(1f)
                    )
                    VariabilityValueCell(
                        label = "Максимум",
                        value = variability.max,
                        time = null,
                        modifier = Modifier.weight(1f)
                    )
                }

                val verdict = variabilityVerdict(variability.percent)

                Text(
                    text = String.format(locale, "%.1f %%", variability.percent),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = verdict.color
                )

                Text(
                    text = verdict.text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = verdict.color
                )

                Text(
                    text = "Норма: колебания менее 15–20%. Если разница между утром и вечером " +
                        "больше 20 %, это говорит о нестабильном состоянии бронхов.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private data class VariabilityVerdict(val text: String, val color: Color)

private fun variabilityVerdict(percent: Float): VariabilityVerdict = when (variabilityLevel(percent)) {
    VariabilityLevel.NORMAL -> VariabilityVerdict("Норма: бронхи стабильны", Zone.GREEN.color)
    VariabilityLevel.BORDERLINE -> VariabilityVerdict("Граница нормы: наблюдайте за состоянием", Zone.YELLOW.color)
    VariabilityLevel.HIGH -> VariabilityVerdict("Повышенная вариабельность: бронхи нестабильны", Zone.RED.color)
}

@Composable
private fun VariabilityValueCell(label: String, value: Float, time: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "${value.toInt()} л/мин",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        if (time != null) {
            Text(
                text = time,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Легенда трёх зон нормы. */
@Composable
private fun ZoneLegend(norm: BreathingNorm) {
    val greenRange = "${(norm.maxNormal * 0.8f).toInt()}–${norm.maxNormal.toInt()}"
    val yellowRange = "${norm.redZoneThreshold.toInt()}–${(norm.maxNormal * 0.8f).toInt()}"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Зоны нормы (л/мин)", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            ZoneLegendRow(Zone.GREEN.color, "Зелёная: $greenRange — всё хорошо")
            ZoneLegendRow(Zone.YELLOW.color, "Жёлтая: $yellowRange — внимание")
            ZoneLegendRow(Zone.RED.color, "Красная: < ${norm.redZoneThreshold.toInt()} — срочно к врачу")
        }
    }
}

@Composable
private fun ZoneLegendRow(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodySmall)
    }
}
