package com.jjswigut.eventide.network.service

import com.jjswigut.eventide.data.models.Weather
import com.jjswigut.eventide.data.models.WeatherGrid
import com.jjswigut.eventide.network.client.WeatherServiceClient
import com.jjswigut.eventide.network.responses.WeatherForecastResponse
import com.jjswigut.eventide.network.responses.WeatherPeriod
import com.jjswigut.eventide.network.responses.WeatherPointsResponse
import com.jjswigut.eventide.network.utils.Either
import com.jjswigut.eventide.network.utils.NetworkError
import com.jjswigut.eventide.utils.GenericError
import com.jjswigut.eventide.utils.UnknownError
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException

class WeatherServiceImpl(
    private val client: WeatherServiceClient,
) : WeatherService {

    override suspend fun getGridCoordinates(
        latitude: Double,
        longitude: Double,
    ): Either<WeatherGrid, GenericError> {
        return runCatching {
            val response = client.getGridPoint(latitude, longitude).body<WeatherPointsResponse>()
            WeatherGrid(
                office = response.properties.gridId,
                gridX = response.properties.gridX,
                gridY = response.properties.gridY,
            )
        }.fold(
            onSuccess = { grid -> Either.success(grid) },
            onFailure = { throwable -> Either.failure(handleError(throwable)) },
        )
    }

    override suspend fun getWeatherForecast(
        grid: WeatherGrid,
    ): Either<List<Weather>, GenericError> {
        return runCatching {
            val response = client.getForecast(
                office = grid.office,
                gridX = grid.gridX,
                gridY = grid.gridY,
            ).body<WeatherForecastResponse>()

            convertPeriodsToDaily(response.properties.periods)
        }.fold(
            onSuccess = { weather -> Either.success(weather) },
            onFailure = { throwable -> Either.failure(handleError(throwable)) },
        )
    }

    /**
     * Converts 12-hour periods into daily weather summaries.
     * Starts from the first daytime period and pairs each day with its following night.
     * This ensures correct day/night pairing regardless of what time the forecast is fetched.
     */
    private fun convertPeriodsToDaily(periods: List<WeatherPeriod>): List<Weather> {
        val dailyWeather = mutableListOf<Weather>()

        // Find the first daytime period to ensure proper day/night pairing
        val startIndex = periods.indexOfFirst { it.isDaytime }
        if (startIndex == -1) return emptyList() // No daytime periods found

        // Process from first daytime period onwards, pairing day with night
        var i = startIndex
        while (i < periods.size && dailyWeather.size < 7) {
            val dayPeriod = periods[i]
            val nightPeriod = periods.getOrNull(i + 1)?.takeIf { !it.isDaytime }

            dailyWeather.add(
                Weather(
                    date = dayPeriod.name,
                    highTemp = dayPeriod.temperature,
                    lowTemp = nightPeriod?.temperature ?: dayPeriod.temperature,
                    conditions = dayPeriod.shortForecast,
                    iconUrl = dayPeriod.icon,
                    windSpeed = dayPeriod.windSpeed,
                ),
            )

            // Move to next day period (skip the night period we just processed)
            i += if (nightPeriod != null) 2 else 1
        }

        return dailyWeather
    }

    private fun handleError(throwable: Throwable): GenericError {
        return if (throwable is ResponseException) {
            NetworkError(code = throwable.response.status.value)
        } else {
            UnknownError()
        }
    }
}
