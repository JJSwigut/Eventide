package com.jjswigut.eventide.repository

import com.jjswigut.eventide.data.models.Station
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    suspend fun addFavorite(station: Station)

    suspend fun removeFavorite(id: String)

    fun isFavorite(id: String): Flow<Boolean>

    fun getFavorites(): Flow<List<Station>>
}
