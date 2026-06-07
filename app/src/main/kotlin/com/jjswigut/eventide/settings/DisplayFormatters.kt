package com.jjswigut.eventide.settings

import com.jjswigut.eventide.data.models.Tide
import java.time.format.DateTimeFormatter
import java.util.Locale

fun Tide.displayTime(settings: AppSettings): String {
    val dateTime = dateTime ?: return time
    val formatter = when (settings.timeFormat) {
        TimeFormat.TwelveHour -> TWELVE_HOUR_TIME_FORMATTER
        TimeFormat.TwentyFourHour -> TWENTY_FOUR_HOUR_TIME_FORMATTER
    }
    return dateTime.format(formatter).lowercase(Locale.US)
}

fun Tide.displayHeight(settings: AppSettings): String {
    val feet = heightFeet ?: return height
    return when (settings.tideUnit) {
        TideUnit.Feet -> String.format(Locale.US, "%.2fft", feet)
        TideUnit.Meters -> String.format(Locale.US, "%.2fm", feet / METERS_TO_FEET)
    }
}

fun Int.displayTemperature(settings: AppSettings): String {
    val temperature = when (settings.tempUnit) {
        TempUnit.Fahrenheit -> this
        TempUnit.Celsius -> ((this - FAHRENHEIT_FREEZING_POINT) * CELSIUS_RATIO).toInt()
    }
    return "$temperature°"
}

private val TWELVE_HOUR_TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mma", Locale.US)
private val TWENTY_FOUR_HOUR_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
private const val METERS_TO_FEET = 3.28084
private const val FAHRENHEIT_FREEZING_POINT = 32
private const val CELSIUS_RATIO = 5.0 / 9.0
