package com.example.asthmahelper.data.api

import com.example.asthmahelper.data.api.dto.AirQualityResponse
import com.example.asthmahelper.data.api.dto.ForecastResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * API Open-Meteo: без ключа и регистрации, бесплатно для некоммерческого использования.
 * Документация: https://open-meteo.com/en/docs
 */
interface WeatherApiService {

    /** Текущая погода по координатам. */
    @GET("https://api.open-meteo.com/v1/forecast")
    suspend fun getForecast(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") current: String =
            "temperature_2m,apparent_temperature,relative_humidity_2m," +
                "surface_pressure,wind_speed_10m,weather_code",
        @Query("wind_speed_unit") windUnit: String = "ms",
        @Query("timezone") timezone: String = "auto"
    ): ForecastResponse

    /** Качество воздуха (европейский AQI) + концентрации пыльцы, зёрен/м³. */
    @GET("https://air-quality-api.open-meteo.com/v1/air-quality")
    suspend fun getAirQuality(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") current: String =
            "european_aqi,pm2_5,pm10,ozone,nitrogen_dioxide," +
                "birch_pollen,alder_pollen,grass_pollen,mugwort_pollen,ragweed_pollen"
    ): AirQualityResponse
}
