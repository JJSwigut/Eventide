package com.jjswigut.eventide.repository

import com.google.android.gms.maps.model.LatLngBounds
import com.jjswigut.eventide.StationsDb
import com.jjswigut.eventide.data.models.Station
import com.jjswigut.eventide.data.models.TideDay
import com.jjswigut.eventide.db.toModel
import com.jjswigut.eventide.network.responses.StationsResponse.StationDto
import com.jjswigut.eventide.network.service.NoaaService
import com.jjswigut.eventide.network.utils.Either
import com.jjswigut.eventide.network.utils.processSuccess
import com.jjswigut.eventide.utils.DbError
import com.jjswigut.eventide.utils.GenericError

class NoaaRepositoryImpl(
    private val noaaService: NoaaService,
    private val stationsDb: StationsDb,
) : NoaaRepository {

    override suspend fun fetchAndCacheStations() {
        if (shouldUpdateStationList()) {
            noaaService.getStations().processSuccess { response ->
                cacheStations(response.stations)
            }
        }
    }

    override suspend fun getTidesForStation(stationID: String): Either<List<TideDay>, GenericError> {
        return noaaService.getTidesForStation(stationID).processSuccess { response ->
            response.toListOfTideDays()
        }
    }


    override suspend fun getStationsWithinBounds(bounds: LatLngBounds): Either<List<Station>, GenericError> {
        return runCatching {
            stationsDb.stationsQueries.selectStationsWithinBounds(
                bounds.southwest.latitude,
                bounds.northeast.latitude,
                bounds.southwest.longitude,
                bounds.northeast.longitude
            ).executeAsList()
        }.fold(
            onSuccess = { list ->
                Either.success(list.toModel())
            },
            onFailure = {
                Either.failure(DbError())
            }
        )
    }


    private fun cacheStations(stations: List<StationDto>) {
        stationsDb.transaction {
            stationsDb.stationsQueries.deleteAll()
            stations.forEach { station ->
                stationsDb.stationsQueries.insertStation(
                    id = station.id,
                    latitude = station.lat,
                    longitude = station.lng,
                    name = station.name,
                    state = station.state
                )
            }
            stationsDb.lastupdatedQueries.insertOrUpdateLastUpdated(System.currentTimeMillis())
        }
    }

    private fun shouldUpdateStationList(): Boolean {
        return stationsDb.lastupdatedQueries.getLastUpdated()
            .executeAsOneOrNull()?.lastUpdated?.let { lastTimeUpdated ->
                (System.currentTimeMillis() - THIRTY_DAYS_IN_MILLISECONDS) > lastTimeUpdated
            } ?: true
    }

    companion object {
        private const val THIRTY_DAYS_IN_MILLISECONDS = 2592000000L
    }
}