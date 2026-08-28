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
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

data class GeoPoint(
    val lat: Double,
    val lon: Double
)

/**
 * Провайдер геолокации через FusedLocationProviderClient (Google Play Services).
 *
 * Ключевая особенность: [getCurrentLocation] ВСЕГДА завершается за конечное время —
 * сначала мгновенно пробует последнее известное местоположение, затем запрашивает
 * свежее обновление; при таймауте (5 сек) возвращает null вместо бесконечного ожидания.
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
     * Текущие координаты или null, если получить не удалось за [timeoutMs].
     * Стратегия: lastLocation (мгновенно) → свежий запрос → таймаут → null.
     */
    suspend fun getCurrentLocation(timeoutMs: Long = 5_000L): GeoPoint? {
        if (!hasPermission()) return null
        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                try {
                    client.lastLocation
                        .addOnSuccessListener { location ->
                            if (location != null) {
                                cont.resumeSafely(GeoPoint(location.latitude, location.longitude))
                            } else {
                                requestFreshLocation(cont)
                            }
                        }
                        .addOnFailureListener {
                            requestFreshLocation(cont)
                        }
                } catch (e: SecurityException) {
                    cont.resumeSafely(null)
                }
            }
        }
    }

    /** Запрашивает одно свежее обновление локации. */
    private fun requestFreshLocation(cont: CancellableContinuation<GeoPoint?>) {
        val request = LocationRequest.Builder(Priority.PRIORITY_LOW_POWER, 10_000L)
            .setWaitForAccurateLocation(false)
            .setMaxUpdates(1)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                client.removeLocationUpdates(this)
                val location = result.lastLocation
                cont.resumeSafely(location?.let { GeoPoint(it.latitude, it.longitude) })
            }
        }

        try {
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            cont.resumeSafely(null)
        }
    }

    /** resume, безопасный при отмене (таймаут) — не бросает исключение при повторном вызове. */
    private fun CancellableContinuation<GeoPoint?>.resumeSafely(value: GeoPoint?) {
        if (isActive) resume(value)
    }
}
