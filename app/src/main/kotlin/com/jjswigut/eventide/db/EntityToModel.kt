package com.jjswigut.eventide.db

import com.google.android.gms.maps.model.LatLng
import com.jjswigut.eventide.data.models.Station

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
