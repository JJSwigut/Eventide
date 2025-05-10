package com.jjswigut.eventide

enum class EventideBuildType(
    val isMinifyEnabled: Boolean,
    val isDebuggable: Boolean,
    val isCrunchPngs: Boolean,
) {
    release(
        isMinifyEnabled = true,
        isDebuggable = false,
        isCrunchPngs = true
    ),

    debug(
        isMinifyEnabled = false,
        isDebuggable = true,
        isCrunchPngs = false
    )
}