package com.jjswigut.eventide.network.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.url
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class MarineServiceClient(engine: HttpClientEngine) {
    private val client = HttpClient(engine) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                },
            )
        }
        defaultRequest {
            header("User-Agent", "Eventide/1.0 (jjswigut@gmail.com)")
        }
    }

    suspend fun getCoopsLatestObservation(
        stationId: String,
        product: String,
    ) = client.get {
        url("$COOPS_BASE_URL/api/prod/datagetter")
        parameter("date", "latest")
        parameter("station", stationId)
        parameter("product", product)
        parameter("time_zone", "lst_ldt")
        parameter("units", "english")
        parameter("application", "Eventide")
        parameter("format", "json")
    }

    suspend fun getActiveAlerts(
        latitude: Double,
        longitude: Double,
    ) = client.get("$NWS_BASE_URL/alerts/active?point=$latitude,$longitude")

    suspend fun getNdbcActiveStations() = client.get("$NDBC_BASE_URL/activestations.xml")

    suspend fun getNdbcRealtimeText(stationId: String) = client.get("$NDBC_BASE_URL/data/realtime2/$stationId.txt")

    suspend fun getOpenMeteoMarine(
        latitude: Double,
        longitude: Double,
    ) = client.get {
        url("$OPEN_METEO_MARINE_BASE_URL/v1/marine")
        parameter("latitude", latitude)
        parameter("longitude", longitude)
        parameter("current", "wave_height,wave_period,sea_surface_temperature")
        parameter("length_unit", "imperial")
        parameter("temperature_unit", "fahrenheit")
        parameter("timezone", "auto")
        parameter("cell_selection", "sea")
    }

    companion object {
        private const val COOPS_BASE_URL = "https://api.tidesandcurrents.noaa.gov"
        private const val NWS_BASE_URL = "https://api.weather.gov"
        private const val NDBC_BASE_URL = "https://www.ndbc.noaa.gov"
        private const val OPEN_METEO_MARINE_BASE_URL = "https://marine-api.open-meteo.com"
    }
}
