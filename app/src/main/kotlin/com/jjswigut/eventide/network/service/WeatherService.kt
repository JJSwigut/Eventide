package com.jjswigut.eventide.network.service

import com.jjswigut.eventide.data.models.Weather
import com.jjswigut.eventide.data.models.WeatherGrid
import com.jjswigut.eventide.network.utils.Either
import com.jjswigut.eventide.utils.GenericError

interface WeatherService {
    /**
     * Get grid coordinates for a location.
     * Results should be cached per station.
     */
    suspend fun getGridCoordinates(
        latitude: Double,
        longitude: Double,
    ): Either<WeatherGrid, GenericError>

    /**
     * Get 7-day weather forecast.
     * Returns one Weather object per day (combining day/night periods).
     */
    suspend fun getWeatherForecast(
        grid: WeatherGrid,
    ): Either<List<Weather>, GenericError>
}
