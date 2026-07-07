package com.jjswigut.eventide.repository

import com.google.android.gms.maps.model.LatLngBounds
import com.jjswigut.eventide.data.models.MarineConditions
import com.jjswigut.eventide.data.models.Station
import com.jjswigut.eventide.data.models.TideDay
import com.jjswigut.eventide.network.utils.Either
import com.jjswigut.eventide.utils.GenericError

interface NoaaRepository {
    suspend fun fetchAndCacheStations()

    suspend fun getAllStations(): Either<List<Station>, GenericError>

    suspend fun getStationsWithinBounds(bounds: LatLngBounds): Either<List<Station>, GenericError>

    suspend fun getTidesForStation(stationID: String): Either<List<TideDay>, GenericError>

    suspend fun getTidesWithWeather(stationID: String): Either<List<TideDay>, GenericError>

    suspend fun getMarineConditionsForStation(stationID: String): Either<MarineConditions, GenericError>

    suspend fun getTidesImmediately(
        stationID: String,
        onWeatherUpdate: (List<TideDay>) -> Unit,
    ): Either<List<TideDay>, GenericError>
}
