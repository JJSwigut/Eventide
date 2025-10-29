package com.jjswigut.eventide.repository

import com.google.android.gms.maps.model.LatLngBounds
import com.jjswigut.eventide.StationsDb
import com.jjswigut.eventide.data.models.Station
import com.jjswigut.eventide.data.models.TideDay
import com.jjswigut.eventide.data.models.Weather
import com.jjswigut.eventide.db.toModel
import com.jjswigut.eventide.network.responses.StationsResponse.StationDto
import com.jjswigut.eventide.network.service.NoaaService
import com.jjswigut.eventide.network.service.WeatherService
import com.jjswigut.eventide.network.utils.Either
import com.jjswigut.eventide.network.utils.processSuccess
import com.jjswigut.eventide.utils.DbError
import com.jjswigut.eventide.utils.GenericError
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class NoaaRepositoryImpl(
    private val noaaService: NoaaService,
    private val weatherService: WeatherService,
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

    override suspend fun getTidesWithWeather(stationID: String): Either<List<TideDay>, GenericError> =
        coroutineScope {
            // Get the station to access its coordinates
            val station =
                getStationById(stationID) ?: return@coroutineScope Either.failure(DbError())

            // Fetch tides and weather in parallel for better performance
            val tidesDeferred = async { getTidesForStation(stationID) }
            val weatherDeferred = async {
                fetchWeatherForStation(
                    latitude = station.latLng.latitude,
                    longitude = station.latLng.longitude
                )
            }

            val tidesResult = tidesDeferred.await()
            val weatherResult = weatherDeferred.await()

            // Combine tides with weather (gracefully handle weather failures)
            tidesResult.processSuccess { tideDays ->
                val weatherList = weatherResult.getOrNull() ?: emptyList()

                tideDays.mapIndexed { index, tideDay ->
                    tideDay.copy(
                        weather = weatherList.getOrNull(index)
                    )
                }
            }
        }

    override suspend fun getTidesImmediately(
        stationID: String,
        onWeatherUpdate: (List<TideDay>) -> Unit,
    ): Either<List<TideDay>, GenericError> {
        // Fetch tides (this is the minimum we need)
        val tidesResult = getTidesForStation(stationID)

        if (tidesResult is Either.Failure) {
            return tidesResult
        }

        val tideDays = (tidesResult as Either.Success).value

        // Return tides immediately with loading state for weather
        val tidesWithLoading = tideDays.map { it.copy(isWeatherLoading = true) }

        // Launch weather fetch in background WITHOUT blocking
        val station = getStationById(stationID)
        if (station != null) {
            // Use GlobalScope or external scope to keep it alive
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                val weatherResult = fetchWeatherForStation(
                    latitude = station.latLng.latitude,
                    longitude = station.latLng.longitude
                )

                val weatherList = weatherResult.getOrNull() ?: emptyList()

                // Combine tides with weather and invoke callback
                val tidesWithWeather = tideDays.mapIndexed { index, tideDay ->
                    tideDay.copy(
                        weather = weatherList.getOrNull(index),
                        isWeatherLoading = false
                    )
                }

                onWeatherUpdate(tidesWithWeather)
            }
        } else {
            // No station found, return with weather unavailable immediately
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                val tidesWithoutWeather = tideDays.map { it.copy(isWeatherLoading = false) }
                onWeatherUpdate(tidesWithoutWeather)
            }
        }

        return Either.success(tidesWithLoading)
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

    override suspend fun getAllStations(): Either<List<Station>, GenericError> {
        return runCatching {
            stationsDb.stationsQueries.selectAllStations().executeAsList()
        }.fold(
            onSuccess = { list ->
                Either.success(list.toModel())
            },
            onFailure = {
                Either.failure(DbError())
            }
        )
    }

    /**
     * Fetches weather for a station location.
     * Returns Either with weather list or error.
     */
    private suspend fun fetchWeatherForStation(
        latitude: Double,
        longitude: Double,
    ): Either<List<Weather>, GenericError> {
        // First, get grid coordinates
        val gridResult = weatherService.getGridCoordinates(latitude, longitude)

        // If grid fetch fails, return error
        if (gridResult is Either.Failure) {
            return Either.failure(gridResult.error)
        }

        // Then fetch forecast using grid coordinates
        return weatherService.getWeatherForecast((gridResult as Either.Success).value)
    }

    /**
     * Gets a station by its ID from the database.
     */
    private fun getStationById(stationID: String): Station? {
        return runCatching {
            stationsDb.stationsQueries.selectStationById(stationID)
                .executeAsOneOrNull()
                ?.let { listOf(it).toModel().firstOrNull() }
        }.getOrNull()
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

/**
 * Helper extension to get the value or null from an Either.
 */
private fun <S, F> Either<S, F>.getOrNull(): S? {
    return when (this) {
        is Either.Success -> this.value
        is Either.Failure -> null
    }
}
