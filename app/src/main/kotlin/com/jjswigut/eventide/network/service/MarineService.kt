package com.jjswigut.eventide.network.service

import com.jjswigut.eventide.data.models.MarineConditions
import com.jjswigut.eventide.network.utils.Either
import com.jjswigut.eventide.utils.GenericError

interface MarineService {
    suspend fun getMarineConditions(
        stationId: String,
        latitude: Double,
        longitude: Double,
    ): Either<MarineConditions, GenericError>
}
