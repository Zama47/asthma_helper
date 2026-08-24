package com.example.asthmahelper.ui.weather

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.asthmahelper.R
import com.example.asthmahelper.domain.model.PollenLevel

@Composable
fun WeatherScreen(navController: androidx.navigation.NavController) {
    val viewModel: WeatherViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsState()

    // Запрос разрешения на геолокацию (Android 6.0+)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        // После ответа пользователя перезагружаем данные (с геолокацией или без)
        viewModel.loadWeatherData()
    }

    LaunchedEffect(Unit) {
        if (viewModel.hasLocationPermission()) {
            viewModel.loadWeatherData()
        } else {
            // Сначала загружаем по умолчанию (Москва), параллельно просим разрешение
            viewModel.loadWeatherData(useLocation = false)
            permissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.weather),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = state.error!!, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadWeatherData() }) {
                            Text(text = "Повторить")
                        }
                    }
                }
            }
            else -> {
                state.weather?.let { weather ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(weather.cityName, style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${weather.temperature.toInt()}°C",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(text = weather.description.replaceFirstChar { it.uppercase() })
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Ощущается как: ${weather.feelsLike.toInt()}°C")
                            Text("Влажность: ${weather.humidity}%")
                            Text("Давление: ${weather.pressure} гПа")
                            Text("Ветер: ${weather.windSpeed} м/с")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                state.airQuality?.let { air ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Качество воздуха",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .width(14.dp)
                                        .height(14.dp)
                                        .background(aqiColor(air.aqi), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${air.level.label} (AQI ${air.aqi})",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("PM2.5: ${air.pm25} мкг/м³")
                            Text("PM10: ${air.pm10} мкг/м³")
                            Text("O₃: ${air.o3} мкг/м³")
                            Text("NO₂: ${air.no2} мкг/м³")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                state.pollen?.let { pollen ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .width(14.dp)
                                        .height(14.dp)
                                        .background(pollenColor(pollen.level), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Аллергенность района: ${pollen.level.label}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            if (pollen.activePlants.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Сейчас цветут:")
                                pollen.activePlants.forEach { plant ->
                                    Text("• ${plant.name} — ${plant.pollenLevel.label}")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Рекомендации
                RecommendationsBlock(state)

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun RecommendationsBlock(state: WeatherUiState) {
    val air = state.airQuality
    val pollen = state.pollen
    val weather = state.weather

    // Европейский AQI Open-Meteo: 0..100+
    val recommendations = buildList {
        if (air != null && air.aqi > 80) {
            add("Качество воздуха очень плохое — рекомендуется оставаться дома и закрыть окна.")
        } else if (air != null && air.aqi > 60) {
            add("Качество воздуха плохое — ограничьте длительные прогулки, особенно у дорог.")
        } else if (air != null && air.aqi > 40) {
            add("Воздух умеренного качества — чувствительным людям стоит снизить активность на улице.")
        }
        if (pollen?.level == PollenLevel.HIGH) {
            add("Уровень пыльцы высокий — используйте маску на улице, примите антигистаминное по назначению врача.")
        }
        pollen?.activePlants?.filter { it.pollenLevel == PollenLevel.HIGH }?.let { highPlants ->
            if (highPlants.isNotEmpty()) {
                add("Максимальная концентрация: ${highPlants.joinToString(", ") { it.name }}.")
            }
        }
        if (weather != null && weather.windSpeed > 6 && pollen != null &&
            pollen.level != PollenLevel.NONE && pollen.level != PollenLevel.LOW
        ) {
            add("Сухо и ветрено — пыльца разносится активно, ограничьте прогулки.")
        }
        if (isEmpty()) {
            // Данные есть и все в норме — только тогда даём позитивную рекомендацию
            add("Показатели в норме — хороший день для прогулок на свежем воздухе!")
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Рекомендации",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (air == null && pollen == null) {
                // Нет данных ни по воздуху, ни по пыльце — не вводим в заблуждение
                Text(
                    "Данные о воздухе и пыльце недоступны для вашего региона.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                recommendations.forEach {
                    Text("• $it", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}



@Composable
private fun aqiColor(aqi: Int): Color = when {
    aqi <= 20 -> Color(0xFF4CAF50)   // Хорошее
    aqi <= 40 -> Color(0xFF8BC34A)   // Удовлетворительное
    aqi <= 60 -> Color(0xFFFFC107)   // Умеренное
    aqi <= 80 -> Color(0xFFFF9800)   // Плохое
    else -> Color(0xFFF44336)        // Очень плохое
}

@Composable
private fun pollenColor(level: PollenLevel): Color = when (level) {
    PollenLevel.NONE -> Color(0xFF9E9E9E)
    PollenLevel.LOW -> Color(0xFF4CAF50)
    PollenLevel.MEDIUM -> Color(0xFFFFC107)
    PollenLevel.HIGH -> Color(0xFFF44336)
}
