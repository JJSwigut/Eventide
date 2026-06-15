package com.jjswigut.eventide.settings

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setTideUnit(tideUnit: TideUnit)

    suspend fun setTempUnit(tempUnit: TempUnit)

    suspend fun setTimeFormat(timeFormat: TimeFormat)

    suspend fun setHomeStationId(stationId: String?)
}

data class AppSettings(
    val tideUnit: TideUnit = TideUnit.Feet,
    val tempUnit: TempUnit = TempUnit.Fahrenheit,
    val timeFormat: TimeFormat = TimeFormat.TwelveHour,
    val homeStationId: String? = null,
)

enum class TideUnit(
    val storageValue: String,
    val label: String,
) {
    Feet("feet", "ft"),
    Meters("meters", "m"),
}

enum class TempUnit(
    val storageValue: String,
    val label: String,
) {
    Fahrenheit("fahrenheit", "F"),
    Celsius("celsius", "C"),
}

enum class TimeFormat(
    val storageValue: String,
    val label: String,
) {
    TwelveHour("12h", "12h"),
    TwentyFourHour("24h", "24h"),
}

fun String.toTideUnit(): TideUnit {
    return TideUnit.entries.firstOrNull { it.storageValue == this } ?: TideUnit.Feet
}

fun String.toTempUnit(): TempUnit {
    return TempUnit.entries.firstOrNull { it.storageValue == this } ?: TempUnit.Fahrenheit
}

fun String.toTimeFormat(): TimeFormat {
    return TimeFormat.entries.firstOrNull { it.storageValue == this } ?: TimeFormat.TwelveHour
}
