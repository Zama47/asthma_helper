package com.example.asthmahelper.data.api.dto

import com.google.gson.annotations.SerializedName

// ---------- Погода: https://api.open-meteo.com/v1/forecast ----------

data class ForecastResponse(
    val latitude: Double,
    val longitude: Double,
    @SerializedName("current") val current: CurrentWeatherDto
)

data class CurrentWeatherDto(
    @SerializedName("time") val time: String,
    @SerializedName("temperature_2m") val temperature: Double,
    @SerializedName("apparent_temperature") val feelsLike: Double,
    @SerializedName("relative_humidity_2m") val humidity: Int,
    @SerializedName("surface_pressure") val pressure: Double, // гПа
    @SerializedName("wind_speed_10m") val windSpeed: Double,  // м/с
    @SerializedName("weather_code") val weatherCode: Int      // WMO-код
)

// ---------- Воздух и пыльца: air-quality-api.open-meteo.com/v1/air-quality ----------

data class AirQualityResponse(
    val latitude: Double,
    val longitude: Double,
    @SerializedName("current") val current: CurrentAirQualityDto
)

data class CurrentAirQualityDto(
    @SerializedName("european_aqi") val europeanAqi: Double?,      // 0..100+
    @SerializedName("pm2_5") val pm25: Double?,                    // мкг/м³
    @SerializedName("pm10") val pm10: Double?,
    @SerializedName("ozone") val ozone: Double?,
    @SerializedName("nitrogen_dioxide") val nitrogenDioxide: Double?,
    // Пыльца, зёрен/м³ (значение < 1 обычно означает отсутствие данных или ноль)
    @SerializedName("birch_pollen") val birchPollen: Double?,      // берёза
    @SerializedName("alder_pollen") val alderPollen: Double?,      // ольха
    @SerializedName("grass_pollen") val grassPollen: Double?,      // злаковые травы
    @SerializedName("mugwort_pollen") val mugwortPollen: Double?,  // полынь
    @SerializedName("ragweed_pollen") val ragweedPollen: Double?   // амброзия
)
