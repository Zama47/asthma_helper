package com.example.asthmahelper.ui.weather

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.asthmahelper.data.api.WeatherApiService
import com.example.asthmahelper.data.api.dto.GeocodingResult
import com.example.asthmahelper.domain.model.AirQualityInfo
import com.example.asthmahelper.domain.model.PollenInfo
import com.example.asthmahelper.domain.model.Plant
import com.example.asthmahelper.domain.model.PollenLevel
import com.example.asthmahelper.domain.model.WeatherInfo
import com.example.asthmahelper.util.LocationProvider
import com.example.asthmahelper.util.WeatherSettingsKeys
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

data class WeatherUiState(
    val isLoading: Boolean = false,
    val weather: WeatherInfo? = null,
    val airQuality: AirQualityInfo? = null,
    val pollen: PollenInfo? = null,
    val error: String? = null,
    val isUsingRealLocation: Boolean = false, // true — геолокация получена
    val isManualCity: Boolean = false,        // true — выбран город вручную
    val locationUnavailable: Boolean = false  // true — гео не сработало и города нет
)

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val api: WeatherApiService,
    private val dataStore: DataStore<Preferences>,
    @param:ApplicationContext private val appContext: Context
) : ViewModel() {

    private val locationProvider = LocationProvider(appContext)

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState

    // Координаты по умолчанию (Москва) — используются, если геолокация недоступна.
    private var lat: Double = 55.7558
    private var lon: Double = 37.6173

    // Одноразовый флаг: пользователь явно запросил геолокацию,
    // сохранённый город в этой загрузке игнорируется.
    private var forceDeviceLocation = false

    /** Есть ли разрешение на геолокацию. */
    fun hasLocationPermission(): Boolean = locationProvider.hasPermission()

    /**
     * Загружает погоду. Приоритет координат:
     * 1) город, выбранный вручную и сохранённый в DataStore;
     * 2) геолокация устройства (если разрешено и [useLocation]);
     * 3) координаты по умолчанию (Москва).
     */
    fun loadWeatherData(useLocation: Boolean = true) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val prefs = dataStore.data.first()
                val savedName = prefs[WeatherSettingsKeys.CITY_NAME]
                val savedLat = prefs[WeatherSettingsKeys.CITY_LAT]
                val savedLon = prefs[WeatherSettingsKeys.CITY_LON]
                val savedCityAvailable =
                    !forceDeviceLocation && savedName != null && savedLat != null && savedLon != null
                forceDeviceLocation = false

                var resolvedCityName: String? = null

                if (savedCityAvailable) {
                    lat = savedLat
                    lon = savedLon
                    resolvedCityName = savedName
                    _uiState.value = _uiState.value.copy(
                        isManualCity = true,
                        isUsingRealLocation = false,
                        locationUnavailable = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isManualCity = false)
                    if (useLocation) {
                        val geoPoint = if (locationProvider.hasPermission()) {
                            withTimeoutOrNull(10_000) { locationProvider.getCurrentLocation() }
                        } else {
                            null
                        }
                        if (geoPoint != null) {
                            lat = geoPoint.lat
                            lon = geoPoint.lon
                            _uiState.value = _uiState.value.copy(
                                isUsingRealLocation = true,
                                locationUnavailable = false
                            )
                        } else {
                            // Ни города, ни геолокации — подсказываем выбрать город вручную
                            _uiState.value = _uiState.value.copy(
                                isUsingRealLocation = false,
                                locationUnavailable = true
                            )
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(isUsingRealLocation = false)
                    }
                }

                val forecastResp = try {
                    api.getForecast(lat, lon)
                } catch (e: Exception) {
                    Log.w("WeatherViewModel", "Forecast request failed", e)
                    null
                }

                val weather = forecastResp?.let { resp ->
                    with(resp.current) {
                        WeatherInfo(
                            cityName = resolvedCityName ?: cityNameByCoords(lat, lon),
                            temperature = temperature,
                            feelsLike = feelsLike,
                            humidity = humidity,
                            pressure = pressure.toInt(),
                            windSpeed = windSpeed,
                            description = describeWeatherCode(weatherCode)
                        )
                    }
                }

                var air: AirQualityInfo? = null
                var pollen: PollenInfo? = null
                try {
                    val airResp = api.getAirQuality(lat, lon)
                    air = with(airResp.current) {
                        AirQualityInfo(
                            aqi = europeanAqi?.toInt() ?: 0,
                            pm25 = pm25 ?: 0.0,
                            pm10 = pm10 ?: 0.0,
                            o3 = ozone ?: 0.0,
                            no2 = nitrogenDioxide ?: 0.0
                        )
                    }
                    pollen = buildPollen(airResp)
                } catch (e: Exception) {
                    Log.w("WeatherViewModel", "Air quality request failed", e)
                }

                // Прогноз и воздух независимы: падение одного не скрывает остальное
                val error = when {
                    weather == null && air == null ->
                        "Ошибка загрузки данных. Проверьте подключение к интернету."
                    weather == null ->
                        "Прогноз погоды временно недоступен. Остальные данные загружены."
                    air == null ->
                        "Данные о качестве воздуха временно недоступны."
                    else -> null
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    weather = weather,
                    airQuality = air,
                    pollen = pollen,
                    error = error
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Ошибка загрузки данных. Проверьте подключение к интернету."
                )
            }
        }
    }

    /** Сохраняет выбранный город и перезагружает погоду для него. */
    fun selectCity(name: String, cityLat: Double, cityLon: Double) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[WeatherSettingsKeys.CITY_NAME] = name
                prefs[WeatherSettingsKeys.CITY_LAT] = cityLat
                prefs[WeatherSettingsKeys.CITY_LON] = cityLon
            }
            loadWeatherData()
        }
    }

    /**
     * Переключение обратно на геолокацию: сбрасывает сохранённый город.
     * Вызывается перед loadWeatherData() или запросом разрешения.
     */
    fun prepareDeviceLocation() {
        forceDeviceLocation = true
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs.remove(WeatherSettingsKeys.CITY_NAME)
                prefs.remove(WeatherSettingsKeys.CITY_LAT)
                prefs.remove(WeatherSettingsKeys.CITY_LON)
            }
        }
    }

    /** Поиск города по названию. null — ошибка сети, пустой список — ничего не найдено. */
    suspend fun searchCities(query: String): List<GeocodingResult>? = try {
        api.searchCities(query).results ?: emptyList()
    } catch (e: Exception) {
        null
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

    /**
     * Название города через бесплатный BigDataCloud reverse geocoding (без ключа).
     * При ошибке — fallback на координаты/крупные города.
     */
    private fun cityNameByCoords(lat: Double, lon: Double): String {
        return try {
            val url = "https://api.bigdatacloud.net/data/reverse-geocode-client" +
                "?latitude=$lat&longitude=$lon&localityLanguage=ru"
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            connection.requestMethod = "GET"
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val cityMatch = Regex("\"city\"\\s*:\\s*\"([^\"]+)\"").find(body)
            val localityMatch = Regex("\"locality\"\\s*:\\s*\"([^\"]+)\"").find(body)
            cityMatch?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
                ?: localityMatch?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
                ?: fallbackCityName(lat, lon)
        } catch (e: Exception) {
            fallbackCityName(lat, lon)
        }
    }

    private fun fallbackCityName(lat: Double, lon: Double): String = when {
        lat in 55.5..56.0 && lon in 37.2..38.2 -> "Москва"
        lat in 59.8..60.2 && lon in 29.9..30.7 -> "Санкт-Петербург"
        else -> "%.2f°, %.2f°".format(lat, lon)
    }
}
