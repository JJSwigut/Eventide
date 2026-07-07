package com.jjswigut.eventide.network.responses

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NwsAlertsResponse(
    val features: List<NwsAlertFeature> = emptyList(),
)

@Serializable
data class NwsAlertFeature(
    val properties: NwsAlertProperties,
)

@Serializable
data class NwsAlertProperties(
    val event: String? = null,
    val headline: String? = null,
    val severity: String? = null,
)

@Serializable
data class OpenMeteoMarineResponse(
    val current: OpenMeteoMarineCurrent? = null,
)

@Serializable
data class OpenMeteoMarineCurrent(
    @SerialName("wave_height")
    val waveHeight: Double? = null,
    @SerialName("wave_period")
    val wavePeriod: Double? = null,
    @SerialName("sea_surface_temperature")
    val seaSurfaceTemperature: Double? = null,
)
