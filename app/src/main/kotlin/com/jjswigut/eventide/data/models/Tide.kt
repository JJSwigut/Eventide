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
)

enum class TideValue(@DrawableRes val iconRes: Int) {
    Low(R.drawable.arrow_down),
    High(R.drawable.arrow_up)
}
