package com.jjswigut.eventide.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.jjswigut.eventide.StationsDb
import com.jjswigut.eventide.data.models.Station
import com.jjswigut.eventide.db.toFavoriteModel
import com.jjswigut.eventide.dispatchers.Dispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FavoritesRepositoryImpl(
    private val stationsDb: StationsDb,
    private val dispatcher: Dispatcher,
) : FavoritesRepository {

    override suspend fun addFavorite(station: Station) {
        stationsDb.favoritesQueries.insertFavorite(
            id = station.id,
            latitude = station.latLng.latitude,
            longitude = station.latLng.longitude,
            name = station.name,
            state = station.state,
            created_at = System.currentTimeMillis(),
        )
    }

    override suspend fun removeFavorite(id: String) {
        stationsDb.favoritesQueries.deleteFavoriteById(id)
    }

    override fun isFavorite(id: String): Flow<Boolean> {
        return stationsDb.favoritesQueries.selectFavoriteStationById(id)
            .asFlow()
            .mapToOneOrNull(dispatcher.io)
            .map { it != null }
    }

    override fun getFavorites(): Flow<List<Station>> {
        return stationsDb.favoritesQueries.selectAllFavoriteStations()
            .asFlow()
            .mapToList(dispatcher.io)
            .map { it.toFavoriteModel() }
    }
}
