package com.fantopo.metacrtl.core.data.manager

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock
import android.util.Log
import com.fantopo.metacrtl.core.data.repository.SettingsRepository
import com.fantopo.metacrtl.core.model.AppSettings
import com.fantopo.metacrtl.core.model.FakeGpsState
import com.fantopo.metacrtl.core.model.LocationPoint
import com.fantopo.metacrtl.core.model.ProviderServiceType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FakeGpsManager(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val randomizer: LocationRandomizer = LocationRandomizer(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val externalScope: CoroutineScope = CoroutineScope(dispatcher)
) {
    private val _state = MutableStateFlow(
        FakeGpsState(
            isActive = false,
            pinnedLocation = null,
            mockedLocation = null
        )
    )
    val state: StateFlow<FakeGpsState> = _state.asStateFlow()

    private var mockJob: Job? = null

    private fun pushMockLocation(point: LocationPoint) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)

        for (provider in providers) {
            try {
                try {
                    locationManager.addTestProvider(provider, false, false, false, false, true, true, true, 0, 5)
                } catch (e: IllegalArgumentException) {
                    // Provider might already exist or not be allowed
                } catch (e: SecurityException) {
                    Log.e("FakeGpsManager", "SecurityException: Mock locations not enabled for $provider")
                    continue
                }

                try {
                    locationManager.setTestProviderEnabled(provider, true)
                } catch (e: Exception) {
                    // Ignore
                }

                val loc = Location(provider)
                loc.latitude = point.latitude
                loc.longitude = point.longitude
                loc.accuracy = if (point.accuracy > 0f) point.accuracy else 3f
                loc.speed = point.speed
                loc.bearing = point.bearing
                loc.time = System.currentTimeMillis()
                loc.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()

                try {
                    locationManager.setTestProviderLocation(provider, loc)
                } catch (e: IllegalArgumentException) {
                    // Try one more time to add and set
                    try {
                        locationManager.addTestProvider(provider, false, false, false, false, true, true, true, 0, 5)
                        locationManager.setTestProviderEnabled(provider, true)
                        locationManager.setTestProviderLocation(provider, loc)
                    } catch (e2: Exception) {
                        Log.e("FakeGpsManager", "Exception pushing mock location to $provider after retry", e2)
                    }
                }
            } catch (e: Exception) {
                Log.e("FakeGpsManager", "Exception handling provider $provider", e)
            }
        }
    }
    
    private fun clearMockProvider() {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            try { locationManager.removeTestProvider(LocationManager.GPS_PROVIDER) } catch (e: Exception) {}
            try { locationManager.removeTestProvider(LocationManager.NETWORK_PROVIDER) } catch (e: Exception) {}
        } catch (e: Exception) {
            Log.e("FakeGpsManager", "Failed to remove test providers", e)
        }
    }


    fun setPinnedLocation(point: LocationPoint) {
        _state.update { current ->
            current.copy(
                pinnedLocation = point,
                mockedLocation = point
            )
        }
        if (_state.value.isActive) {
            refreshLocation()
        }
    }

    fun startMock(provider: ProviderServiceType? = null) {
        _state.update {
            it.copy(
                isActive = true,
                activeProvider = provider
            )
        }
        restartMockLoop()
    }

    fun stopMock() {
        mockJob?.cancel()
        mockJob = null
        _state.update { it.copy(isActive = false) }
        clearMockProvider()
    }

    fun toggleMock(provider: ProviderServiceType? = null) {
        if (_state.value.isActive) {
            stopMock()
        } else {
            startMock(provider)
        }
    }

    /**
     * Refreshes the mocked location for current pinned marker immediately.
     */
    fun refreshLocation() {
        externalScope.launch(dispatcher) {
            val settings = settingsRepository.getSettings().first()
            val base = _state.value.pinnedLocation
            val mocked = randomizer.randomize(base, settings)
            _state.update { current ->
                current.copy(
                    mockedLocation = mocked,
                    lastUpdatedAt = System.currentTimeMillis(),
                    refreshCount = current.refreshCount + 1
                )
            }
            if (mocked != null) {
                pushMockLocation(mocked)
            }
        }
    }

    private fun restartMockLoop() {
        mockJob?.cancel()
        mockJob = externalScope.launch(dispatcher) {
            while (isActive) {
                val settings = settingsRepository.getSettings().first()
                val base = _state.value.pinnedLocation
                val mocked = randomizer.randomize(base, settings)

                _state.update { current ->
                    current.copy(
                        mockedLocation = mocked,
                        lastUpdatedAt = System.currentTimeMillis(),
                        refreshCount = current.refreshCount + 1
                    )
                }
                
                if (mocked != null) {
                    pushMockLocation(mocked)
                }

                val delayMs = if (settings.refreshTimeMs <= 0L) {
                    100L // Safe minimum delay to prevent busy looping
                } else {
                    settings.refreshTimeMs
                }
                delay(delayMs)
            }
        }
    }
}
