package com.fantopo.metacrtl.core.data.manager

import com.fantopo.metacrtl.core.data.repository.SettingsRepository
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
    private val settingsRepository: SettingsRepository,
    private val locationPusher: MockLocationPusher = NoOpMockLocationPusher,
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

    // The location basis actually used to compute the pushed mock location.
    // Decoupled from `pinnedLocation` (the marker shown on the map) so that
    // moving the pin while mock GPS is active does not, by itself, move the
    // simulated GPS position — only the floating overlay's refresh button
    // (see refreshLocation()) advances the mock GPS to the latest pin.
    private var activeBaseLocation: LocationPoint? = null

    fun setPinnedLocation(point: LocationPoint) {
        _state.update { current ->
            if (current.isActive) {
                // While mock GPS is running, only move the pinned marker. The actual
                // mocked/pushed location must not jump until the user explicitly taps
                // the floating overlay's refresh button (see refreshLocation()).
                current.copy(pinnedLocation = point)
            } else {
                current.copy(
                    pinnedLocation = point,
                    mockedLocation = point
                )
            }
        }
        if (!_state.value.isActive) {
            activeBaseLocation = point
        }
    }

    fun startMock(provider: ProviderServiceType? = null) {
        activeBaseLocation = _state.value.pinnedLocation
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
        locationPusher.clear()
    }

    fun toggleMock(provider: ProviderServiceType? = null) {
        if (_state.value.isActive) {
            stopMock()
        } else {
            startMock(provider)
        }
    }

    /**
     * Advances the mock GPS to the currently pinned marker and refreshes the
     * mocked location immediately. This is the only way the simulated GPS
     * position moves to a newly-marked pin while mock GPS is active.
     */
    fun refreshLocation() {
        activeBaseLocation = _state.value.pinnedLocation
        externalScope.launch(dispatcher) {
            val settings = settingsRepository.getSettings().first()
            val base = activeBaseLocation
            val mocked = randomizer.randomize(base, settings)
            _state.update { current ->
                current.copy(
                    mockedLocation = mocked,
                    lastUpdatedAt = System.currentTimeMillis(),
                    refreshCount = current.refreshCount + 1
                )
            }
            if (mocked != null) {
                locationPusher.pushLocation(mocked)
            }
        }
    }

    private fun restartMockLoop() {
        mockJob?.cancel()
        mockJob = externalScope.launch(dispatcher) {
            while (isActive) {
                val settings = settingsRepository.getSettings().first()
                val base = activeBaseLocation
                val mocked = randomizer.randomize(base, settings)

                _state.update { current ->
                    current.copy(
                        mockedLocation = mocked,
                        lastUpdatedAt = System.currentTimeMillis(),
                        refreshCount = current.refreshCount + 1
                    )
                }
                
                if (mocked != null) {
                    locationPusher.pushLocation(mocked)
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
