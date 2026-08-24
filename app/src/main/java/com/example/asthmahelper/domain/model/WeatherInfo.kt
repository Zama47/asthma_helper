package com.example.asthmahelper.domain.model

data class WeatherInfo(
    val cityName: String,
    val temperature: Double,
    val feelsLike: Double,
    val humidity: Int,
    val pressure: Int,
    val windSpeed: Double,
    val description: String
)

/** Европейский AQI Open-Meteo (0..100+, чем выше — тем хуже). */
data class AirQualityInfo(
    val aqi: Int,
    val pm25: Double,
    val pm10: Double,
    val o3: Double,
    val no2: Double
) {
    val level: AirQualityLevel
        get() = when {
            aqi <= 20 -> AirQualityLevel.GOOD        // Хорошее (зелёный)
            aqi <= 40 -> AirQualityLevel.FAIR        // Удовлетворительное
            aqi <= 60 -> AirQualityLevel.MODERATE    // Умеренное
            aqi <= 80 -> AirQualityLevel.POOR        // Плохое
            else -> AirQualityLevel.VERY_POOR        // Очень плохое
        }
}

enum class AirQualityLevel(val label: String, val colorHex: Long) {
    GOOD("Хорошее", 0xFF4CAF50),
    FAIR("Удовлетворительное", 0xFF8BC34A),
    MODERATE("Умеренное", 0xFFFFC107),
    POOR("Плохое", 0xFFFF9800),
    VERY_POOR("Очень плохое", 0xFFF44336)
}

/**
 * Пыльца из реальных данных Open-Meteo.
 * Пороги в зёрнах/м³ — общепринятые клинические ориентиры для трав/деревьев.
 */
data class PollenInfo(
    val level: PollenLevel,
    val activePlants: List<Plant>
)

enum class PollenLevel(val label: String) {
    NONE("Нет"),
    LOW("Низкий"),
    MEDIUM("Средний"),
    HIGH("Высокий");

    companion object {
        /** Классификация концентрации по зёрнам/м³. */
        fun fromConcentration(grainsPerM3: Double): PollenLevel = when {
            grainsPerM3 < 1 -> NONE      // нет данных или ноль
            grainsPerM3 < 10 -> LOW
            grainsPerM3 < 30 -> MEDIUM
            else -> HIGH
        }
    }
}

data class Plant(
    val name: String,
    val pollenLevel: PollenLevel
) {
    val isSignificant: Boolean get() = pollenLevel != PollenLevel.NONE
}
