package com.fantopo.metacrtl.core.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.fantopo.metacrtl.core.model.AppSettings
import com.fantopo.metacrtl.core.model.MapStyleMode
import com.fantopo.metacrtl.core.model.ProviderServiceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DefaultSettingsRepository(
    initialSettings: AppSettings = AppSettings(),
    private val prefs: SharedPreferences = InMemorySharedPreferences()
) : SettingsRepository {

    private val _settings = MutableStateFlow(loadSettings(initialSettings))
    
    private fun loadSettings(default: AppSettings): AppSettings {
        return default.copy(
            isFloatingMode = prefs.getBoolean("isFloatingMode", default.isFloatingMode),
            isFusedMode = prefs.getBoolean("isFusedMode", default.isFusedMode),
            isRandomCoordinate = prefs.getBoolean("isRandomCoordinate", default.isRandomCoordinate),
            randomRadiusMeters = prefs.getFloat("randomRadiusMeters", default.randomRadiusMeters.toFloat()).toDouble(),
            isRandomAccuracy = prefs.getBoolean("isRandomAccuracy", default.isRandomAccuracy),
            accuracyMin = prefs.getFloat("accuracyMin", default.accuracyMin.toFloat()).toDouble(),
            accuracyMax = prefs.getFloat("accuracyMax", default.accuracyMax.toFloat()).toDouble(),
            isRandomAltitude = prefs.getBoolean("isRandomAltitude", default.isRandomAltitude),
            altitudeMin = prefs.getFloat("altitudeMin", default.altitudeMin),
            altitudeMax = prefs.getFloat("altitudeMax", default.altitudeMax),
            isRandomBearing = prefs.getBoolean("isRandomBearing", default.isRandomBearing),
            isRandomSpeed = prefs.getBoolean("isRandomSpeed", default.isRandomSpeed),
            speedMin = prefs.getFloat("speedMin", default.speedMin),
            speedMax = prefs.getFloat("speedMax", default.speedMax),
            refreshTimeMs = prefs.getLong("refreshTimeMs", default.refreshTimeMs)
        )
    }

    private fun saveSettings(s: AppSettings) {
        prefs.edit()
            .putBoolean("isFloatingMode", s.isFloatingMode)
            .putBoolean("isFusedMode", s.isFusedMode)
            .putBoolean("isRandomCoordinate", s.isRandomCoordinate)
            .putFloat("randomRadiusMeters", s.randomRadiusMeters.toFloat())
            .putBoolean("isRandomAccuracy", s.isRandomAccuracy)
            .putFloat("accuracyMin", s.accuracyMin.toFloat())
            .putFloat("accuracyMax", s.accuracyMax.toFloat())
            .putBoolean("isRandomAltitude", s.isRandomAltitude)
            .putFloat("altitudeMin", s.altitudeMin)
            .putFloat("altitudeMax", s.altitudeMax)
            .putBoolean("isRandomBearing", s.isRandomBearing)
            .putBoolean("isRandomSpeed", s.isRandomSpeed)
            .putFloat("speedMin", s.speedMin)
            .putFloat("speedMax", s.speedMax)
            .putLong("refreshTimeMs", s.refreshTimeMs)
            .apply()
    }

    override fun getSettings(): Flow<AppSettings> = _settings.asStateFlow()

    override suspend fun updateSettings(settings: AppSettings) {
        _settings.value = settings
        saveSettings(settings)
    }

    override suspend fun setFloatingMode(enabled: Boolean) {
        _settings.update { it.copy(isFloatingMode = enabled) }
        saveSettings(_settings.value)
    }

    override suspend fun setFusedMode(enabled: Boolean) {
        _settings.update { it.copy(isFusedMode = enabled) }
        saveSettings(_settings.value)
    }

    override suspend fun setRandomCoordinate(enabled: Boolean, radiusMeters: Double?) {
        _settings.update {
            it.copy(
                isRandomCoordinate = enabled,
                randomRadiusMeters = radiusMeters ?: it.randomRadiusMeters
            )
        }
        saveSettings(_settings.value)
    }

    override suspend fun setRandomAccuracy(enabled: Boolean, min: Double?, max: Double?) {
        _settings.update {
            it.copy(
                isRandomAccuracy = enabled,
                accuracyMin = min ?: it.accuracyMin,
                accuracyMax = max ?: it.accuracyMax
            )
        }
        saveSettings(_settings.value)
    }

    override suspend fun setRandomAltitude(enabled: Boolean, min: Float?, max: Float?) {
        _settings.update {
            it.copy(
                isRandomAltitude = enabled,
                altitudeMin = min ?: it.altitudeMin,
                altitudeMax = max ?: it.altitudeMax
            )
        }
        saveSettings(_settings.value)
    }

    override suspend fun setRandomBearing(enabled: Boolean) {
        _settings.update { it.copy(isRandomBearing = enabled) }
        saveSettings(_settings.value)
    }

    override suspend fun setRandomSpeed(enabled: Boolean, min: Float?, max: Float?) {
        _settings.update {
            it.copy(
                isRandomSpeed = enabled,
                speedMin = min ?: it.speedMin,
                speedMax = max ?: it.speedMax
            )
        }
        saveSettings(_settings.value)
    }

    override suspend fun setRefreshTimeMs(refreshTimeMs: Long) {
        val clamped = refreshTimeMs.coerceIn(AppSettings.REFRESH_TIME_MIN, AppSettings.REFRESH_TIME_MAX)
        _settings.update { it.copy(refreshTimeMs = clamped) }
        saveSettings(_settings.value)
    }

    override suspend fun toggleMapStyle(style: MapStyleMode) {
        _settings.update { current ->
            val updated = if (current.mapStyles.contains(style)) {
                current.mapStyles - style
            } else {
                current.mapStyles + style
            }
            current.copy(mapStyles = updated)
        }
        saveSettings(_settings.value)
    }

    override suspend fun setSelectedProvider(provider: ProviderServiceType?) {
        _settings.update { it.copy(selectedProvider = provider) }
        saveSettings(_settings.value)
    }

    companion object {
        private const val PREFS_NAME = "app_settings"

        /**
         * Creates a [DefaultSettingsRepository] backed by real Android
         * [SharedPreferences] derived from [context]. Use this factory in
         * production code; the primary constructor's in-memory default is
         * intended for unit tests that don't have a real [Context].
         */
        fun create(context: Context, initialSettings: AppSettings = AppSettings()): DefaultSettingsRepository {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return DefaultSettingsRepository(initialSettings, prefs)
        }
    }
}
