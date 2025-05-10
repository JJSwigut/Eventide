package com.jjswigut.eventide.network.service

import com.jjswigut.eventide.network.client.NoaaServiceClient
import com.jjswigut.eventide.network.responses.StationsResponse
import com.jjswigut.eventide.network.responses.TidesResponse
import com.jjswigut.eventide.network.utils.Either
import com.jjswigut.eventide.network.utils.NetworkError
import com.jjswigut.eventide.utils.GenericError
import com.jjswigut.eventide.utils.UnknownError
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class NoaaServiceImpl(
    private val client: NoaaServiceClient
) : NoaaService {
    override suspend fun getStations(): Either<StationsResponse, GenericError> {
        return runCatching {
            client.getStations().body<StationsResponse>()
        }.fold(
            onSuccess = { response ->
                Either.success(response)
            },
            onFailure = { throwable ->
                val error = if (throwable is ResponseException) {
                    NetworkError(code = throwable.response.status.value)
                } else {
                    UnknownError()
                }
                Either.failure(error)
            }
        )
    }

    override suspend fun getTidesForStation(stationID: String): Either<TidesResponse, GenericError> {
        val startDate = LocalDate.now()
        val endDate = startDate.plusMonths(1)

        return runCatching {
            client.getTides(
                startDate = startDate.format(dateFormatter),
                endDate = endDate.format(dateFormatter),
                stationID = stationID,
            ).body<TidesResponse>()
        }.fold(
            onSuccess = { response ->
                Either.success(response)
            },
            onFailure = { throwable ->
                val error = if (throwable is ResponseException) {
                    NetworkError(code = throwable.response.status.value)
                } else {
                    UnknownError()
                }
                Either.failure(error)
            }
        )
    }

    companion object {
        private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.US)
    }
}
