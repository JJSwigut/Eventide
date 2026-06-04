package com.jjswigut.eventide.network.responses

import com.google.android.gms.maps.model.LatLng
import com.jjswigut.eventide.data.models.Station
import com.jjswigut.eventide.network.responses.StationsResponse.StationDto
import kotlinx.serialization.Serializable

@Serializable
data class StationsResponse(
    val count: Int,
    val stations: List<StationDto>,
) {
    @Serializable
    data class StationDto(
        val id: String,
        val lat: Double,
        val lng: Double,
        val name: String,
        val portscode: String,
        val state: String,
        val tideType: String,
        val tidepredoffsets: Tidepredoffsets,
        val timemeridian: Int?,
        val timezonecorr: Int,
        val type: String,
    ) {
        @Serializable
        data class Tidepredoffsets(
            val self: String,
        )
    }
}

fun StationDto.toModel(): Station {
    return Station(
        id = id,
        latLng = LatLng(lat, lng),
        name = name,
        state = state,
    )
}
