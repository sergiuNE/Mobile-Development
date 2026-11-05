package be.ap.nowaste.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import be.ap.nowaste.ui.map.Coordinates
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

// A first GPS fix indoors can take long; after this the customer picks a location on the map instead
private const val LOCATION_TIMEOUT_MS = 15_000L

/** Location of the phone through the built-in LocationManager, so no Google Play services library is needed. */
class LocationRepository(private val context: Context) {

    private val locationManager = context.getSystemService(LocationManager::class.java)

    /** True when the user allowed precise or approximate location. */
    fun hasPermission(): Boolean =
        isGranted(Manifest.permission.ACCESS_FINE_LOCATION) || isGranted(Manifest.permission.ACCESS_COARSE_LOCATION)

    fun hasPreciseLocation(): Boolean = isGranted(Manifest.permission.ACCESS_FINE_LOCATION)

    /** Current location, or null without permission, with location turned off, or when no fix comes in time. */
    @SuppressLint("MissingPermission") // checked with hasPermission() first
    suspend fun currentLocation(): Coordinates? {
        if (!hasPermission()) return null
        val provider = bestProvider() ?: return null
        return withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val cancellationSignal = CancellationSignal()
                continuation.invokeOnCancellation { cancellationSignal.cancel() }
                LocationManagerCompat.getCurrentLocation(
                    locationManager,
                    provider,
                    cancellationSignal,
                    ContextCompat.getMainExecutor(context),
                ) { location ->
                    continuation.resume(location?.let { Coordinates(it.latitude, it.longitude) })
                }
            }
        }
    }

    // Fused combines GPS, Wi-Fi and cell towers (Android 12+). getProviders(true) only returns enabled providers.
    private fun bestProvider(): String? {
        val enabled = locationManager.getProviders(true)
        val preferred = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
            add(LocationManager.GPS_PROVIDER)
        }
        return preferred.firstOrNull { it in enabled }
    }

    private fun isGranted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
