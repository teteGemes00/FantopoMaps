package com.fantopo.metacrtl.core.data.manager

import com.fantopo.metacrtl.core.model.AppSettings
import com.fantopo.metacrtl.core.model.LocationPoint
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class LocationRandomizer(
    private val random: Random = Random.Default
) {
    /**
     * Applies randomization logic based on the provided settings.
     * Random radius jitter is applied first (moves the point within a circle
     * around the pin), then random accuracy (horizontal & vertical) is applied.
     */
    fun randomize(basePoint: LocationPoint?, settings: AppSettings): LocationPoint? {
        if (basePoint == null) return null
        var lat = basePoint.latitude
        var lng = basePoint.longitude
        var alt = basePoint.altitude
        var bearing = basePoint.bearing
        var speed = basePoint.speed
        var accuracy = basePoint.accuracy
        var verticalAccuracy = basePoint.verticalAccuracy

        // 1) Random radius: jitter the coordinate uniformly within a circle of
        // radius `randomRadiusMeters` (0.0 - 20.0m) around the pinned point.
        if (settings.isRandomCoordinate) {
            val radius = settings.randomRadiusMeters.coerceIn(
                AppSettings.RADIUS_ALLOWED_MIN,
                AppSettings.RADIUS_ALLOWED_MAX
            )
            if (radius > 0.0) {
                // sqrt() keeps the distribution uniform across the circle's area
                // instead of clustering points toward the outer edge.
                val distanceMeters = radius * sqrt(random.nextDouble())
                val angleRad = random.nextDouble() * 2.0 * Math.PI

                val deltaLatMeters = distanceMeters * cos(angleRad)
                val deltaLngMeters = distanceMeters * sin(angleRad)

                val latOffset = deltaLatMeters / 111111.0
                val lngOffset = deltaLngMeters / (111111.0 * cos(Math.toRadians(lat)).coerceAtLeast(0.0001))

                lat += latOffset
                lng += lngOffset
            }
        }

        // 2) Random accuracy: one setting controls both horizontal and vertical accuracy.
        if (settings.isRandomAccuracy) {
            val min = settings.accuracyMin.coerceAtLeast(AppSettings.ACCURACY_ALLOWED_MIN)
            val max = settings.accuracyMax.coerceAtMost(AppSettings.ACCURACY_ALLOWED_MAX)
            accuracy = randomAccuracyValue(min, max)
            verticalAccuracy = randomAccuracyValue(min, max)
        }

        if (settings.isRandomAltitude) {
            val min = settings.altitudeMin.coerceAtLeast(AppSettings.ALTITUDE_ALLOWED_MIN)
            val max = settings.altitudeMax.coerceAtMost(AppSettings.ALTITUDE_ALLOWED_MAX)
            alt = if (max > min) {
                (min + random.nextFloat() * (max - min)).toDouble()
            } else {
                min.toDouble()
            }
        }

        if (settings.isRandomBearing) {
            bearing = random.nextFloat() * 360f
        }

        if (settings.isRandomSpeed) {
            val min = settings.speedMin.coerceAtLeast(AppSettings.SPEED_ALLOWED_MIN)
            var max = settings.speedMax.coerceAtMost(AppSettings.SPEED_ALLOWED_MAX)

            // Guard against a reported speed that is physically inconsistent with the
            // tiny position jitter applied above. A GPS consumer (map/ride-hailing app)
            // commonly dead-reckons/extrapolates the marker between fixes using the
            // reported speed+bearing; if speed implies far more travel per refresh tick
            // than the actual random-radius jitter allows, the extrapolated marker keeps
            // "walking" in a new random direction every tick and can end up far outside
            // the configured radius even though every real fix stays within it. Cap the
            // upper bound so implied travel (speed * refresh interval) never wildly
            // exceeds the configured jitter radius, while never dropping below the
            // user's own minimum.
            if (settings.isRandomCoordinate && settings.refreshTimeMs > 0L) {
                val radius = settings.randomRadiusMeters.coerceIn(
                    AppSettings.RADIUS_ALLOWED_MIN,
                    AppSettings.RADIUS_ALLOWED_MAX
                )
                val refreshSeconds = settings.refreshTimeMs / 1000.0
                val maxRealisticSpeed = (radius / refreshSeconds).toFloat()
                max = max.coerceAtMost(maxRealisticSpeed.coerceAtLeast(min))
            }

            speed = if (max > min) {
                min + random.nextFloat() * (max - min)
            } else {
                min
            }
        }

        return LocationPoint(
            latitude = lat,
            longitude = lng,
            altitude = alt,
            bearing = bearing,
            speed = speed,
            accuracy = accuracy,
            verticalAccuracy = verticalAccuracy
        )
    }

    private fun randomAccuracyValue(min: Double, max: Double): Float {
        return if (max > min) {
            (min + random.nextDouble() * (max - min)).toFloat()
        } else {
            min.toFloat()
        }
    }
}
