package com.example.asthmahelper.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class GeoPoint(
    val lat: Double,
    val lon: Double
)

/**
 * Провайдер геолокации через FusedLocationProviderClient (Google Play Services).
 * Возвращает Flow с последним известным местоположением.
 */
class LocationProvider(private val context: Context) {

    private val client by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    /** Проверка: есть ли разрешение на геолокацию. */
    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Поток координат. Эмитит последнее известное местоположение,
     * затем обновления (одного достаточно — отписываемся после первого).
     */
    fun locationFlow(): Flow<GeoPoint> = callbackFlow {
        if (!hasPermission()) {
            close(SecurityException("Нет разрешения ACCESS_FINE_LOCATION"))
            return@callbackFlow
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 60_000L)
            .setMinUpdateIntervalMillis(30_000L)
            .setMaxUpdates(1) // достаточно одного обновления
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    trySend(GeoPoint(location.latitude, location.longitude))
                    close() // получили координаты — закрываем поток
                }
            }
        }

        try {
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            close(e)
        }

        awaitClose { client.removeLocationUpdates(callback) }
    }
}
