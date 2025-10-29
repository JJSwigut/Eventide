package com.jjswigut.eventide.data.models

/**
 * Simplified weather model for UI display.
 * Represents weather for a single day.
 */
data class Weather(
  val date: String,                   // e.g., "Monday"
  val highTemp: Int,                  // High temperature
  val lowTemp: Int,                   // Low temperature
  val conditions: String,             // e.g., "Partly Cloudy"
  val iconUrl: String,                // URL to weather icon
  val windSpeed: String,               // e.g., "5 to 10 mph"
)

/**
 * Grid coordinates for a station, used to fetch weather.
 * Cache these to avoid repeated /points API calls.
 */
data class WeatherGrid(
  val office: String,                 // NWS office code
  val gridX: Int,                     // Grid X coordinate
  val gridY: Int,                      // Grid Y coordinate
)
