package com.jjswigut.eventide.map.models

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import com.jjswigut.eventide.data.models.Station

class StationClusterItem(val station: Station) : ClusterItem {
    override fun getPosition(): LatLng = station.latLng

    override fun getTitle(): String = station.name

    override fun getSnippet(): String = station.name
    override fun getZIndex(): Float? = null

    override fun equals(other: Any?): Boolean {
        return if (other is StationClusterItem) {
            station == other.station
        } else {
            super.equals(other)
        }
    }

    override fun hashCode(): Int {
        return station.hashCode()
    }
}
