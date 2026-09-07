package com.fantopo.metacrtl.core.data.manager

import android.content.Context
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.location.LocationProvider
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.fantopo.metacrtl.core.model.LocationPoint

/**
 * Abstraction over pushing a mocked/fake location to the Android OS test
 * providers. Extracted from [FakeGpsManager] so the manager's business
 * logic can be unit tested on the plain JVM without requiring a real
 * Android [Context].
 */
interface MockLocationPusher {
    fun pushLocation(point: LocationPoint)
    fun clear()
}

/**
 * No-op implementation used as the default for [FakeGpsManager] so it can be
 * constructed and unit tested without any Android framework dependency.
 */
object NoOpMockLocationPusher : MockLocationPusher {
    override fun pushLocation(point: LocationPoint) {
        // Intentionally does nothing.
    }

    override fun clear() {
        // Intentionally does nothing.
    }
}

/**
 * Production implementation that registers Android test location providers
 * and pushes mock [Location] updates to them.
 */
class AndroidMockLocationPusher(private val context: Context) : MockLocationPusher {

    // Providers that have already been added/enabled/marked AVAILABLE. Re-issuing
    // addTestProvider/setTestProviderEnabled/setTestProviderStatus on every single
    // location push (once per refresh cycle) makes the system treat the provider as
    // if it just came back online each time, which is what causes the status bar
    // location icon to blink continuously instead of staying solid. Providers are
    // only (re-)initialized once, and subsequent pushes just update the location.
    private val readyProviders = mutableSetOf<String>()

    private fun ensureProviderReady(locationManager: LocationManager, provider: String): Boolean {
        if (readyProviders.contains(provider)) return true
        try {
            try {
                locationManager.addTestProvider(
                    provider, false, false, false, false, true, true, true,
                    Criteria.POWER_LOW, Criteria.ACCURACY_FINE
                )
            } catch (e: IllegalArgumentException) {
                // Provider might already exist or not be allowed
            } catch (e: SecurityException) {
                Log.e("MockLocationPusher", "SecurityException: Mock locations not enabled for $provider")
                return false
            }

            try {
                locationManager.setTestProviderEnabled(provider, true)
            } catch (e: Exception) {
                // Ignore
            }

            try {
                // Mark the provider as AVAILABLE once so the system status bar location
                // icon shows a stable/solid state instead of blinking as if it were
                // still "searching" for a fix.
                locationManager.setTestProviderStatus(
                    provider,
                    LocationProvider.AVAILABLE,
                    null,
                    System.currentTimeMillis()
                )
            } catch (e: Exception) {
                // Ignore
            }

            readyProviders.add(provider)
            return true
        } catch (e: Exception) {
            Log.e("MockLocationPusher", "Exception preparing provider $provider", e)
            return false
        }
    }

    override fun pushLocation(point: LocationPoint) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)

        for (provider in providers) {
            try {
                if (!ensureProviderReady(locationManager, provider)) continue

                val loc = Location(provider)
                loc.latitude = point.latitude
                loc.longitude = point.longitude
                loc.altitude = point.altitude
                loc.accuracy = if (point.accuracy > 0f) point.accuracy else 3f
                loc.speed = point.speed
                loc.bearing = point.bearing
                loc.time = System.currentTimeMillis()
                loc.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    loc.verticalAccuracyMeters = if (point.verticalAccuracy > 0f) point.verticalAccuracy else loc.accuracy
                }

                try {
                    locationManager.setTestProviderLocation(provider, loc)
                } catch (e: IllegalArgumentException) {
                    // Provider may have been removed externally; re-add and retry once.
                    readyProviders.remove(provider)
                    try {
                        if (ensureProviderReady(locationManager, provider)) {
                            locationManager.setTestProviderLocation(provider, loc)
                        }
                    } catch (e2: Exception) {
                        Log.e("MockLocationPusher", "Exception pushing mock location to $provider after retry", e2)
                    }
                }
            } catch (e: Exception) {
                Log.e("MockLocationPusher", "Exception handling provider $provider", e)
            }
        }
    }

    override fun clear() {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
            try { locationManager.removeTestProvider(LocationManager.GPS_PROVIDER) } catch (e: Exception) {}
            try { locationManager.removeTestProvider(LocationManager.NETWORK_PROVIDER) } catch (e: Exception) {}
        } catch (e: Exception) {
            Log.e("MockLocationPusher", "Failed to remove test providers", e)
        } finally {
            readyProviders.clear()
        }
    }
}
