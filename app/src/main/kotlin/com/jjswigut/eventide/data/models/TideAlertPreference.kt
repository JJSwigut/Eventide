package com.jjswigut.eventide.data.models

data class TideAlertPreference(
    val stationId: String,
    val leadTimeMinutes: Int,
    val tideFilter: TideAlertFilter,
    val enabled: Boolean,
)

enum class TideAlertFilter(
    val storageValue: String,
    val label: String,
) {
    High("high", "High"),
    Low("low", "Low"),
    Both("both", "Both"),
}

fun String.toTideAlertFilter(): TideAlertFilter {
    return TideAlertFilter.entries.firstOrNull { it.storageValue == this } ?: TideAlertFilter.Both
}
