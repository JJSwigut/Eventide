package com.jjswigut.eventide.data.models

import androidx.annotation.DrawableRes
import com.jjswigut.eventide.R

data class Tide(
    val time: String,
    val tideValue: TideValue,
    val height: String,
)

data class TideDay(
    val date: String,
    val tides: List<Tide>,
    val weather: Weather? = null,
    val isWeatherLoading: Boolean = false,
    val isTidesLoading: Boolean = false,
)

enum class TideValue(@DrawableRes val iconRes: Int) {
    Low(R.drawable.arrow_down),
    High(R.drawable.arrow_up),
}
