package com.jjswigut.eventide.data.models

import com.google.android.gms.maps.model.LatLng

data class Station(
    val id: String,
    val latLng: LatLng,
    val name: String,
    val state: String,
)
