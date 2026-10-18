package io.github.zama47.asthmahelper.ui.dashboard

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.github.zama47.asthmahelper.R
import io.github.zama47.asthmahelper.domain.clinical.Zone
import io.github.zama47.asthmahelper.domain.clinical.actLevel
import io.github.zama47.asthmahelper.domain.clinical.isRescueUseHigh
import io.github.zama47.asthmahelper.domain.clinical.percentOfBest
import io.github.zama47.asthmahelper.domain.clinical.zoneOf
import io.github.zama47.asthmahelper.domain.clinical.zoneOfPercent
import io.github.zama47.asthmahelper.domain.model.BreathingNorm
import io.github.zama47.asthmahelper.domain.model.PollenLevel
import io.github.zama47.asthmahelper.ui.act.color
import io.github.zama47.asthmahelper.ui.act.label
import io.github.zama47.asthmahelper.ui.components.AddMeasurementDialog
import io.github.zama47.asthmahelper.widget.AsthmaWidgetProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    navController: androidx.navigation.NavController,
    startWithAddMeasurement: Boolean = false,
    onStartAddMeasurementHandled: () -> Unit = {}
) {
    val viewModel: DashboardViewModel = hiltViewModel()
    val locale = LocalConfiguration.current.locales[0]
    val context = LocalContext.current
    var showAddMeasurementDialog by remember { mutableStateOf(false) }

    // Виджет может попросить открыть диалог добавления замера сразу после запуска.
    LaunchedEffect(startWithAddMeasurement) {
        if (startWithAddMeasurement) {
            showAddMeasurementDialog = true
            onStartAddMeasurementHandled()
        }
    }

    val measurements by viewModel.recentMeasurements.collectAsState()
    val norm by viewModel.breathingNorm.collectAsState()
    val todayMedications by viewModel.todayMedications.collectAsState()
    val weatherData by viewModel.weatherData.collectAsState()
    val personalBest by viewModel.personalBestValue.collectAsState()
    val exacerbation by viewModel.exacerbation.collectAsState()
    val rescueStats by viewModel.rescueStats.collectAsState()
    val lastAct by viewModel.lastAct.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.dashboard),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        exacerbation?.let { warning ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Возможно обострение",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ПСВ держится на ${warning.worstPercentOfBest.roundToInt()}% от личного " +
                                "максимума ${warning.consecutiveDays} дн. подряд. Проверьте план действий " +
                                "и свяжитесь с врачом.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { showAddMeasurementDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.add_measurement))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Виджеты: аллергия + воздух (реальные данные из API)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AllergyLevelWidget(
                level = weatherData.pollen?.level?.label ?: "Нет данных",
                color = pollenColor(weatherData.pollen?.level ?: PollenLevel.NONE),
                modifier = Modifier.weight(1f)
            )
            AirQualityWidget(
                aqi = weatherData.airQuality?.aqi ?: 0,
                description = weatherData.airQuality?.level?.label ?: "Нет данных",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatWidget(
                title = "Личный максимум",
                value = personalBest?.let { "${it.toInt()} л/мин" } ?: "—",
                caption = "за 14 дней",
                color = Color(0xFF3949AB),
                modifier = Modifier.weight(1f)
            )
            StatWidget(
                title = "Спасательный ингалятор",
                value = "${rescueStats.today} / ${rescueStats.thisWeek}",
                caption = "сегодня / за неделю",
                color = if (isRescueUseHigh(rescueStats.thisWeek)) Color(0xFFF44336) else Color(0xFF4CAF50),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            val act = lastAct
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Контроль астмы (ACT)", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    if (act != null) {
                        val level = actLevel(act.total)
                        Text(
                            text = "${act.total} / 25 · ${level.label}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = level.color,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text(
                            text = "Пройдите тест из 5 вопросов",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                OutlinedButton(onClick = { navController.navigate("act") { launchSingleTop = true } }) {
                    Text(if (act == null) "Пройти" else "Обновить")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Туду-список приёмов лекарств на сегодня
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Приём лекарств на сегодня",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (todayMedications.isNotEmpty() && todayMedications.all { it.taken }) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (todayMedications.isEmpty()) {
                    Text(
                        text = "Нет приёмов на сегодня",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    todayMedications.forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = item.taken,
                                onCheckedChange = { checked ->
                                    viewModel.toggleMedication(item, checked)
                                    // Держим галочку на виджете в актуальном состоянии.
                                    AsthmaWidgetProvider.refresh(context)
                                }
                            )
                            Column {
                                Text(text = item.schedule.dose, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = formatTime(item.schedule.timeOfDay),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Сводка
        val last = measurements.firstOrNull()
        val lastPercent = last?.let { percentOfBest(it.value, personalBest) }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Последний замер: ${last?.value?.toInt()?.toString() ?: "—"} л/мин" +
                        (lastPercent?.let { " (${it.roundToInt()}% от максимума)" } ?: ""),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Норма: ${norm?.minNormal?.toInt() ?: 0}–${norm?.maxNormal?.toInt() ?: 0}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Последние записи",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Прокрутка общая для всей страницы, поэтому список записей — обычный Column.
        Column(modifier = Modifier.fillMaxWidth()) {
            measurements.take(10).forEach { measurement ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(peakFlowZoneColor(measurement.value, norm, personalBest), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "${measurement.value.toInt()} л/мин",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = SimpleDateFormat("dd.MM.yyyy HH:mm", locale)
                                    .format(Date(measurement.timestamp)),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Ссылка на политику конфиденциальности и дисклеймер — внутри приложения (требование RuStore).
        OutlinedButton(
            onClick = { navController.navigate("about") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.about_and_privacy))
        }
    }

    if (showAddMeasurementDialog) {
        AddMeasurementDialog(
            onDismiss = { showAddMeasurementDialog = false },
            onSave = { value, note ->
                viewModel.addMeasurement(value, note)
                showAddMeasurementDialog = false
                // Сразу обновляем виджет домашнего экрана, не дожидаясь периодического обновления.
                AsthmaWidgetProvider.refresh(context)
            }
        )
    }
}

private fun formatTime(msSinceMidnight: Long): String {
    val minutes = msSinceMidnight / 60_000
    return String.format(Locale.getDefault(), "%02d:%02d", minutes / 60, minutes % 60)
}

private fun peakFlowZoneColor(value: Float, norm: BreathingNorm?, best: Float?): Color {
    if (norm == null && best == null) return Color(0xFF9E9E9E)
    val zone = if (best != null) zoneOfPercent(value, best) else zoneOf(value, norm)
    return when (zone) {
        Zone.GREEN -> Color(0xFF4CAF50)
        Zone.YELLOW -> Color(0xFFFFC107)
        Zone.RED -> Color(0xFFF44336)
    }
}

@Composable
private fun pollenColor(level: PollenLevel): Color = when (level) {
    PollenLevel.NONE -> Color(0xFF9E9E9E)
    PollenLevel.LOW -> Color(0xFF4CAF50)
    PollenLevel.MEDIUM -> Color(0xFFFFC107)
    PollenLevel.HIGH -> Color(0xFFF44336)
}

@Composable
private fun AllergyLevelWidget(level: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(color, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.allergy_level),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = level, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun AirQualityWidget(aqi: Int, description: String, modifier: Modifier = Modifier) {
    // Европейский AQI Open-Meteo: 0..100+
    val aqiColor = when {
        aqi <= 20 -> Color(0xFF4CAF50)
        aqi <= 40 -> Color(0xFF8BC34A)
        aqi <= 60 -> Color(0xFFFFC107)
        aqi <= 80 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(aqiColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.air_quality),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "AQI $aqi", style = MaterialTheme.typography.titleMedium)
            Text(text = description, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun StatWidget(title: String, value: String, caption: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
            Text(text = caption, style = MaterialTheme.typography.bodySmall)
        }
    }
}
