package com.jjswigut.eventide.network.responses

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
    val effective: String? = null,
    val onset: String? = null,
    val expires: String? = null,
    val ends: String? = null,
)
