package com.jjswigut.eventide.network.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.url
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * A class representing the Noaa service client.
 *
 * @param engine The HTTP client engine used for the network requests.
 */
class NoaaServiceClient(engine: HttpClientEngine) {
    private val client = HttpClient(engine) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                },
            )
        }
        install(HttpTimeout) {
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
            socketTimeoutMillis = SOCKET_TIMEOUT_MS
        }
        defaultRequest {
            header("User-Agent", USER_AGENT)
        }
    }

    suspend fun getStations() = client.get(base_url + stations_url)

    suspend fun getTides(
        startDate: String,
        endDate: String,
        stationID: String,
    ) = client.get {
        url(base_url + tides_url)
        parameter("begin_date", startDate)
        parameter("end_date", endDate)
        parameter("station", stationID)
    }

    companion object {
        private const val base_url = "https://api.tidesandcurrents.noaa.gov/"
        private const val stations_url =
            "mdapi/prod/webapi/stations.json?type=tidepredictions&units=english"
        private const val tides_url =
            "api/prod/datagetter?product=predictions&application=NOS.COOPS.TAC.WL&datum=MLLW&time_zone=lst_ldt&units=metric&interval=hilo&format=json"
        private const val USER_AGENT = "Eventide/1.0 (jjswigut@gmail.com)"
        private const val CONNECT_TIMEOUT_MS = 10_000L
        private const val REQUEST_TIMEOUT_MS = 30_000L
        private const val SOCKET_TIMEOUT_MS = 20_000L
    }
}
