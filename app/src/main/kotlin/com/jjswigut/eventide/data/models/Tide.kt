package com.jjswigut.eventide.data.models

import androidx.annotation.DrawableRes
import com.jjswigut.eventide.R
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class Tide(
    val time: String,
    val tideValue: TideValue,
    val height: String,
    val dateTime: LocalDateTime? = null,
    val heightFeet: Double? = null,
)

data class TideDay(
    val date: String,
    val tides: List<Tide>,
    val dateValue: LocalDate? = null,
    val weather: Weather? = null,
    val sunMoonData: SunMoonData? = null,
    val isWeatherLoading: Boolean = false,
    val isTidesLoading: Boolean = false,
    val tideSource: String = "NOAA CO-OPS tide prediction",
)

data class SunMoonData(
    val sunrise: LocalTime?,
    val sunset: LocalTime?,
    val moonPhase: MoonPhase,
    val moonIlluminationPercent: Int,
)

enum class MoonPhase(
    val label: String,
    val icon: String,
) {
    New("New", "🌑"),
    WaxingCrescent("Waxing crescent", "🌒"),
    FirstQuarter("First quarter", "🌓"),
    WaxingGibbous("Waxing gibbous", "🌔"),
    Full("Full", "🌕"),
    WaningGibbous("Waning gibbous", "🌖"),
    LastQuarter("Last quarter", "🌗"),
    WaningCrescent("Waning crescent", "🌘"),
}

enum class TideValue(@DrawableRes val iconRes: Int) {
    Low(R.drawable.arrow_down),
    High(R.drawable.arrow_up),
}
