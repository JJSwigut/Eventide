package com.jjswigut.eventide.data.models

data class MarineConditions(
    val observations: List<MarineObservation> = emptyList(),
    val alerts: List<MarineAlert> = emptyList(),
    val buoy: BuoyConditions? = null,
    val waveForecast: WaveForecast? = null,
) {
    val hasData: Boolean
        get() = observations.isNotEmpty() || alerts.isNotEmpty() || buoy != null || waveForecast != null
}

data class MarineObservation(
    val label: String,
    val value: String,
)

data class MarineAlert(
    val event: String,
    val headline: String,
    val severity: String,
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
)

data class WaveForecast(
    val waveHeight: String? = null,
    val wavePeriod: String? = null,
    val seaSurfaceTemperature: String? = null,
    val attribution: String = "Open-Meteo Marine",
)
