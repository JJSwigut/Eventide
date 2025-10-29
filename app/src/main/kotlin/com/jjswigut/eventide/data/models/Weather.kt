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
) {
  /**
   * Gets an emoji representation of the weather conditions.
   */
  fun getWeatherEmoji(): String {
    return when {
      conditions.contains("partly sunny", ignoreCase = true) -> "⛅️"
      conditions.contains("sunny", ignoreCase = true) -> "☀️"
      conditions.contains("clear", ignoreCase = true) -> "☀️"
      conditions.contains("partly cloudy", ignoreCase = true) -> "⛅"
      conditions.contains("mostly cloudy", ignoreCase = true) -> "🌥️"
      conditions.contains("cloudy", ignoreCase = true) -> "☁️"
      conditions.contains("overcast", ignoreCase = true) -> "☁️"
      conditions.contains("rain", ignoreCase = true) -> "🌧️"
      conditions.contains("shower", ignoreCase = true) -> "🌧️"
      conditions.contains("drizzle", ignoreCase = true) -> "🌦️"
      conditions.contains("thunder", ignoreCase = true) -> "⛈️"
      conditions.contains("storm", ignoreCase = true) -> "⛈️"
      conditions.contains("snow", ignoreCase = true) -> "❄️"
      conditions.contains("flurr", ignoreCase = true) -> "🌨️"
      conditions.contains("sleet", ignoreCase = true) -> "🌨️"
      conditions.contains("fog", ignoreCase = true) -> "🌫️"
      conditions.contains("haze", ignoreCase = true) -> "🌫️"
      conditions.contains("mist", ignoreCase = true) -> "🌫️"
      conditions.contains("wind", ignoreCase = true) -> "💨"
      else -> "🌤️"  // Default to partly sunny
    }
  }
}

/**
 * Grid coordinates for a station, used to fetch weather.
 * Cache these to avoid repeated /points API calls.
 */
data class WeatherGrid(
  val office: String,                 // NWS office code
  val gridX: Int,                     // Grid X coordinate
  val gridY: Int,                      // Grid Y coordinate
)
