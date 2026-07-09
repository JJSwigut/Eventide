package com.jjswigut.eventide.network.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Client for the National Weather Service API.
 *
 * @param engine The HTTP client engine used for network requests.
 */
class WeatherServiceClient(engine: HttpClientEngine) {
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

    /**
     * Get grid coordinates for a latitude/longitude.
     * This is required before fetching the forecast.
     *
     * @param latitude Latitude in decimal degrees
     * @param longitude Longitude in decimal degrees
     */
    suspend fun getGridPoint(
        latitude: Double,
        longitude: Double,
    ) =
        client.get("$BASE_URL/points/$latitude,$longitude")

    /**
     * Get 7-day forecast for a grid location.
     *
     * @param office NWS office code (e.g., "LWX")
     * @param gridX Grid X coordinate
     * @param gridY Grid Y coordinate
     */
    suspend fun getForecast(
        office: String,
        gridX: Int,
        gridY: Int,
    ) =
        client.get("$BASE_URL/gridpoints/$office/$gridX,$gridY/forecast")

    companion object {
        private const val BASE_URL = "https://api.weather.gov"
        private const val USER_AGENT = "Eventide/1.0 (jjswigut@gmail.com)"
        private const val CONNECT_TIMEOUT_MS = 10_000L
        private const val REQUEST_TIMEOUT_MS = 30_000L
        private const val SOCKET_TIMEOUT_MS = 20_000L
    }
}
