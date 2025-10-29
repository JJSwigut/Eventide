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
        gridY = response.properties.gridY
      )
    }.fold(
      onSuccess = { grid -> Either.success(grid) },
      onFailure = { throwable -> Either.failure(handleError(throwable)) }
    )
  }

  override suspend fun getWeatherForecast(
    grid: WeatherGrid,
  ): Either<List<Weather>, GenericError> {
    return runCatching {
      val response = client.getForecast(
        office = grid.office,
        gridX = grid.gridX,
        gridY = grid.gridY
      ).body<WeatherForecastResponse>()

      convertPeriodsToDaily(response.properties.periods)
    }.fold(
      onSuccess = { weather -> Either.success(weather) },
      onFailure = { throwable -> Either.failure(handleError(throwable)) }
    )
  }

  /**
   * Converts 12-hour periods into daily weather summaries.
   * Combines day and night periods for each day.
   */
  private fun convertPeriodsToDaily(periods: List<WeatherPeriod>): List<Weather> {
    val dailyWeather = mutableListOf<Weather>()

    // Group periods by day (every 2 periods = 1 day)
    periods.chunked(2).forEach { dayPeriods ->
      val dayPeriod = dayPeriods.firstOrNull { it.isDaytime }
      val nightPeriod = dayPeriods.firstOrNull { !it.isDaytime }

      // Use day period as primary, fall back to night if day not available
      val primary = dayPeriod ?: nightPeriod ?: return@forEach

      dailyWeather.add(
        Weather(
          date = primary.name,
          highTemp = dayPeriod?.temperature ?: primary.temperature,
          lowTemp = nightPeriod?.temperature ?: primary.temperature,
          conditions = primary.shortForecast,
          iconUrl = primary.icon,
          windSpeed = primary.windSpeed
        )
      )
    }

    return dailyWeather.take(7) // Ensure we only return 7 days
  }

  private fun handleError(throwable: Throwable): GenericError {
    return if (throwable is ResponseException) {
      NetworkError(code = throwable.response.status.value)
    } else {
      UnknownError()
    }
  }
}
