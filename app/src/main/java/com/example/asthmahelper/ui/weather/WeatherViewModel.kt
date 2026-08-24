package com.example.asthmahelper.ui.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.asthmahelper.data.api.WeatherApiService
import com.example.asthmahelper.domain.model.AirQualityInfo
import com.example.asthmahelper.domain.model.PollenInfo
import com.example.asthmahelper.domain.model.Plant
import com.example.asthmahelper.domain.model.PollenLevel
import com.example.asthmahelper.domain.model.WeatherInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WeatherUiState(
    val isLoading: Boolean = false,
    val weather: WeatherInfo? = null,
    val airQuality: AirQualityInfo? = null,
    val pollen: PollenInfo? = null,
    val error: String? = null
)

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val api: WeatherApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState

    // Координаты по умолчанию (Москва), пока нет геолокации.
    private var lat: Double = 55.7558
    private var lon: Double = 37.6173

    fun loadWeatherData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val forecastResp = api.getForecast(lat, lon)
                val airResp = api.getAirQuality(lat, lon)

                val weather = with(forecastResp.current) {
                    WeatherInfo(
                        cityName = cityNameByCoords(lat, lon),
                        temperature = temperature,
                        feelsLike = feelsLike,
                        humidity = humidity,
                        pressure = pressure.toInt(),
                        windSpeed = windSpeed,
                        description = describeWeatherCode(weatherCode)
                    )
                }

                val air = with(airResp.current) {
                    AirQualityInfo(
                        aqi = europeanAqi?.toInt() ?: 0,
                        pm25 = pm25 ?: 0.0,
                        pm10 = pm10 ?: 0.0,
                        o3 = ozone ?: 0.0,
                        no2 = nitrogenDioxide ?: 0.0
                    )
                }

                val pollen = buildPollen(airResp)

                _uiState.value = WeatherUiState(
                    isLoading = false,
                    weather = weather,
                    airQuality = air,
                    pollen = pollen
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Ошибка загрузки данных. Проверьте подключение к интернету."
                )
            }
        }
    }

    /** Собирает пыльцу из ответа API (реальные данные, без эвристик). */
    private fun buildPollen(resp: com.example.asthmahelper.data.api.dto.AirQualityResponse): PollenInfo {
        val c = resp.current
        data class Entry(val name: String, val value: Double?)

        val entries = listOf(
            Entry("Берёза", c.birchPollen),
            Entry("Ольха", c.alderPollen),
            Entry("Злаковые", c.grassPollen),
            Entry("Полынь", c.mugwortPollen),
            Entry("Амброзия", c.ragweedPollen)
        )

        val plants = entries
            .filter { it.value != null && it.value!! >= 1.0 }
            .map { Plant(it.name, PollenLevel.fromConcentration(it.value!!)) }
            .sortedByDescending { it.pollenLevel.ordinal }

        // Общий уровень района — максимум среди цветущих растений
        val overall = plants.maxOfOrNull { it.pollenLevel } ?: PollenLevel.NONE

        return PollenInfo(level = overall, activePlants = plants)
    }

    /**
     * WMO weather codes → описание на русском.
     * https://open-meteo.com/en/docs (weather_code)
     */
    private fun describeWeatherCode(code: Int): String = when (code) {
        0 -> "Ясно"
        1, 2 -> "Переменная облачность"
        3 -> "Пасмурно"
        in 45..48 -> "Туман"
        in 51..55 -> "Морось"
        56, 57 -> "Ледяная морось"
        in 61..65 -> "Дождь"
        66, 67 -> "Ледяной дождь"
        in 71..77 -> "Снег"
        in 80..82 -> "Ливни"
        85, 86 -> "Снежные ливни"
        95 -> "Гроза"
        96, 99 -> "Гроза с градом"
        else -> "—"
    }

    /** Названия города нет в ответе Open-Meteo — определяем приблизительно по координатам. */
    private fun cityNameByCoords(lat: Double, lon: Double): String =
        when {
            lat in 55.5..56.0 && lon in 37.2..38.2 -> "Москва"
            lat in 59.8..60.2 && lon in 29.9..30.7 -> "Санкт-Петербург"
            else -> "%.2f°, %.2f°".format(lat, lon)
        }
}
