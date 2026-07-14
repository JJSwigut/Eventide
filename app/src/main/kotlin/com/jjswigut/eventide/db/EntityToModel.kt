package com.jjswigut.eventide.db

import com.google.android.gms.maps.model.LatLng
import com.jjswigut.eventide.data.models.Station
import com.jjswigut.eventide.data.models.TideAlertPreference
import com.jjswigut.eventide.data.models.toTideAlertFilter

fun List<StationEntity>.toModel(): List<Station> {
    return this.map {
        Station(
            id = it.id,
            latLng = LatLng(it.latitude, it.longitude),
            name = it.name,
            state = it.state,
        )
    }
}

fun List<FavoriteStationEntity>.toFavoriteModel(): List<Station> {
    return this.map {
        Station(
            id = it.id,
            latLng = LatLng(it.latitude, it.longitude),
            name = it.name,
            state = it.state,
        )
    }
}

fun List<TideAlertPreferenceEntity>.toAlertPreferenceModel(): List<TideAlertPreference> {
    return this.map {
        TideAlertPreference(
            stationId = it.station_id,
            leadTimeMinutes = it.lead_time_minutes.toInt(),
            tideFilter = it.tide_filter.toTideAlertFilter(),
            enabled = it.enabled == 1L,
        )
    }
}
