package com.example.asthmahelper.util

import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Ключи сохранённого города в DataStore (настройки погоды).
 * Используются и экраном погоды, и дашбордом, чтобы данные совпадали.
 */
object WeatherSettingsKeys {
    val CITY_NAME = stringPreferencesKey("weather_city_name")
    val CITY_LAT = doublePreferencesKey("weather_city_lat")
    val CITY_LON = doublePreferencesKey("weather_city_lon")
}
