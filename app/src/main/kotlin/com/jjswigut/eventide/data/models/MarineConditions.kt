package com.jjswigut.eventide.data.models

import java.time.Instant

data class MarineConditions(
    val observations: List<MarineObservation> = emptyList(),
    val alerts: List<MarineAlert> = emptyList(),
    val buoy: BuoyConditions? = null,
) {
    val hasData: Boolean
        get() = observations.isNotEmpty() || alerts.isNotEmpty() || buoy != null
}

data class MarineObservation(
    val label: String,
    val value: String,
    val observedAt: Instant? = null,
    val source: String = "NOAA CO-OPS",
    val sourceType: String = "Observation",
)

data class MarineAlert(
    val event: String,
    val headline: String,
    val severity: String,
    val effectiveAt: Instant? = null,
    val expiresAt: Instant? = null,
    val source: String = "National Weather Service",
)

data class BuoyConditions(
    val stationId: String,
    val stationName: String,
    val distanceMiles: Double,
    val wind: String? = null,
    val waveHeight: String? = null,
    val wavePeriod: String? = null,
    val waterTemperature: String? = null,
    val pressure: String? = null,
    val observedAt: Instant? = null,
    val source: String = "NDBC buoy",
    val sourceType: String = "Observation",
)
