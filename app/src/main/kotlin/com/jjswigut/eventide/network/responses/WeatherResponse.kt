package com.jjswigut.eventide.network.responses

import kotlinx.serialization.Serializable

/**
 * Response from /points/{lat},{lon} endpoint.
 * Used to get grid coordinates for a location.
 */
@Serializable
data class WeatherPointsResponse(
    val properties: WeatherPointsProperties,
)

@Serializable
data class WeatherPointsProperties(
    val gridId: String, // Office code (e.g., "LWX")
    val gridX: Int, // Grid X coordinate
    val gridY: Int, // Grid Y coordinate
)

/**
 * Response from /gridpoints/{office}/{gridX},{gridY}/forecast endpoint.
 * Contains the actual 7-day forecast.
 */
@Serializable
data class WeatherForecastResponse(
    val properties: WeatherForecastProperties,
)

@Serializable
data class WeatherForecastProperties(
    val periods: List<WeatherPeriod>,
)

/**
 * A single forecast period (usually 12 hours).
 * The API returns ~14 periods for a 7-day forecast (day/night for each day).
 */
@Serializable
data class WeatherPeriod(
    val number: Int, // Period number (1, 2, 3...)
    val name: String, // "Today", "Tonight", "Monday", etc.
    val startTime: String, // ISO 8601 timestamp
    val endTime: String, // ISO 8601 timestamp
    val isDaytime: Boolean, // true for day periods, false for night
    val temperature: Int, // Temperature in Fahrenheit
    val temperatureUnit: String, // "F" or "C"
    val windSpeed: String, // e.g., "5 to 10 mph"
    val windDirection: String, // e.g., "SW"
    val icon: String, // URL to weather icon
    val shortForecast: String, // e.g., "Partly Cloudy"
    val detailedForecast: String, // Longer description
)
