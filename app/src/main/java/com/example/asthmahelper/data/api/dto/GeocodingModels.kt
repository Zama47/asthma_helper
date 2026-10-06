package com.example.asthmahelper.data.api.dto

import com.google.gson.annotations.SerializedName

/**
 * Ответ Open-Meteo Geocoding API (поиск города по названию).
 * https://open-meteo.com/en/docs/geocoding-api
 */
data class GeocodingResponse(
    val results: List<GeocodingResult>? = null
)

data class GeocodingResult(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    @SerializedName("admin1") val admin1: String? = null
)
