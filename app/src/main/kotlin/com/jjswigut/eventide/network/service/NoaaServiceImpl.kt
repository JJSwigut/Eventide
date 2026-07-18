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
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class NoaaServiceImpl(
    private val client: NoaaServiceClient,
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
            },
        )
    }

    override suspend fun getTidesForStation(stationID: String): Either<TidesResponse, GenericError> {
        val startDate = LocalDate.now()
        val endDate = startDate.plusDays(7)
        val formattedStartDate = startDate.format(dateFormatter)
        val formattedEndDate = endDate.format(dateFormatter)
        val requests = listOf<suspend () -> TidesResponse>(
            {
                client.getTides(
                    startDate = formattedStartDate,
                    endDate = formattedEndDate,
                    stationID = stationID,
                ).body()
            },
            {
                client.getTidesByRange(
                    startDate = formattedStartDate,
                    rangeHours = TIDE_RANGE_HOURS,
                    stationID = stationID,
                ).body()
            },
            {
                client.getTides(
                    startDate = formattedStartDate,
                    endDate = formattedEndDate,
                    stationID = stationID,
                ).body()
            },
        )
        var lastError: GenericError = UnknownError()

        requests.forEachIndexed { index, request ->
            val result = runCatching { request() }
            val response = result.getOrNull()
            if (response != null && response.error == null && response.predictions.isNotEmpty()) {
                return Either.success(response)
            }

            result.exceptionOrNull()?.let { throwable ->
                lastError = if (throwable is ResponseException) {
                    NetworkError(code = throwable.response.status.value)
                } else {
                    UnknownError()
                }
            }

            retryDelaysMillis.getOrNull(index)?.let { delayMillis ->
                delay(delayMillis)
            }
        }

        return Either.failure(lastError)
    }

    companion object {
        private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.US)
        private val retryDelaysMillis = listOf(350L, 900L)
        private const val TIDE_RANGE_HOURS = 192
    }
}
