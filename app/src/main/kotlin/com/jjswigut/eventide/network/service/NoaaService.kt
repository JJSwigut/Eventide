package com.jjswigut.eventide.network.service

import com.jjswigut.eventide.network.responses.StationsResponse
import com.jjswigut.eventide.network.responses.TidesResponse
import com.jjswigut.eventide.network.utils.Either
import com.jjswigut.eventide.utils.GenericError

interface NoaaService {
    suspend fun getStations(): Either<StationsResponse, GenericError>

    suspend fun getTidesForStation(stationID: String): Either<TidesResponse, GenericError>
}
