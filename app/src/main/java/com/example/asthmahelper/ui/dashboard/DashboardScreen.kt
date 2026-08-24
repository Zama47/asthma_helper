package com.example.asthmahelper.ui.dashboard

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card

import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.asthmahelper.R
import com.example.asthmahelper.ui.components.AddMeasurementDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(navController: androidx.navigation.NavController) {
    val viewModel: DashboardViewModel = hiltViewModel()
    var showAddMeasurementDialog by remember { mutableStateOf(false) }

    val measurements by viewModel.recentMeasurements.collectAsState()
    val norm by viewModel.breathingNorm.collectAsState()
    val todayMedications by viewModel.todayMedications.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.dashboard),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { showAddMeasurementDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.add_measurement))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Виджеты: аллергия + воздух
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AllergyLevelWidget(
                level = "Средний",
                color = Color(0xFFFFC107),
                modifier = Modifier.weight(1f)
            )
            AirQualityWidget(
                aqi = 25,
                description = "Удовлетворительное",
                modifier = Modifier.weight(1f)
            )
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
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Последний замер: ${
                        last?.value?.toInt()?.toString() ?: "—"
                    } л/мин",
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

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(measurements.take(10)) { measurement ->
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
                                .background(color(measurement.value, norm), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "${measurement.value.toInt()} л/мин",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                                    .format(Date(measurement.timestamp)),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddMeasurementDialog) {
        AddMeasurementDialog(
            onDismiss = { showAddMeasurementDialog = false },
            onSave = { value, note ->
                viewModel.addMeasurement(value, note)
                showAddMeasurementDialog = false
            }
        )
    }
}

private fun formatTime(msSinceMidnight: Long): String {
    val minutes = msSinceMidnight / 60_000
    return String.format(Locale.getDefault(), "%02d:%02d", minutes / 60, minutes % 60)
}

private fun color(
    value: Float,
    norm: com.example.asthmahelper.domain.model.BreathingNorm?
): Color {
    if (norm == null) return Color(0xFF9E9E9E)
    return if (value < norm.minNormal || value > norm.maxNormal) Color(0xFFF44336)
    else Color(0xFF4CAF50)
}

@Composable
private fun AllergyLevelWidget(
    level: String,
    color: Color,
    modifier: Modifier = Modifier
) {
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
private fun AirQualityWidget(
    aqi: Int,
    description: String,
    modifier: Modifier = Modifier
) {
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
