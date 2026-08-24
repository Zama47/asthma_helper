package com.example.asthmahelper.data.api.dto

import com.google.gson.annotations.SerializedName

data class AirPollutionResponse(
    @SerializedName("list") val list: List<AirPollutionItem>
)

data class AirPollutionItem(
    @SerializedName("main") val main: AirQualityMain,
    @SerializedName("components") val components: AirComponents,
    @SerializedName("dt") val dt: Long
)

data class AirQualityMain(
    @SerializedName("aqi") val aqi: Int // 1..5 (1 - Good, 5 - Very Poor)
)

data class AirComponents(
    @SerializedName("co") val co: Double,
    @SerializedName("no2") val no2: Double,
    @SerializedName("o3") val o3: Double,
    @SerializedName("pm2_5") val pm25: Double,
    @SerializedName("pm10") val pm10: Double
)
