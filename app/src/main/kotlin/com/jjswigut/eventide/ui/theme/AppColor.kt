package com.jjswigut.eventide.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

object AppColor {

    val surface
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.surface

    val container
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.surfaceContainer

    val onContainer
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onPrimaryContainer

    val darkText
        @Composable
        @ReadOnlyComposable
        get() = DarkText

    val lightText
        @Composable
        @ReadOnlyComposable
        get() = LightText
}