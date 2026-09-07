package com.fantopo.metacrtl.core.model

data class AppSettings(
    val isFloatingMode: Boolean = false,
    val isFusedMode: Boolean = true,
    val isRandomCoordinate: Boolean = false,
    val randomRadiusMeters: Double = 3.0,
    val isRandomAccuracy: Boolean = false,
    val accuracyMin: Double = 5.0,
    val accuracyMax: Double = 5.0,
    val isRandomAltitude: Boolean = false,
    val altitudeMin: Float = 0f,
    val altitudeMax: Float = 15f,
    val isRandomBearing: Boolean = false,
    val isRandomSpeed: Boolean = false,
    val speedMin: Float = 1f,
    val speedMax: Float = 5f,
    val refreshTimeMs: Long = 1000L,
    val mapStyles: Set<MapStyleMode> = emptySet(),
    val selectedProvider: ProviderServiceType? = null
) {
    companion object {
        const val ACCURACY_ALLOWED_MIN = 0.0
        const val ACCURACY_ALLOWED_MAX = 20.0
        const val RADIUS_ALLOWED_MIN = 0.0
        const val RADIUS_ALLOWED_MAX = 20.0
        const val ALTITUDE_ALLOWED_MIN = 0f
        const val ALTITUDE_ALLOWED_MAX = 75f
        const val SPEED_ALLOWED_MIN = 0f
        const val SPEED_ALLOWED_MAX = 55f
        const val REFRESH_TIME_MIN = 0L
        const val REFRESH_TIME_MAX = 1300L
        const val DEFAULT_REFRESH_TIME = 1000L
    }
}
