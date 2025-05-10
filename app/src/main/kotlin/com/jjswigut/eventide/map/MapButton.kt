package com.jjswigut.eventide.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jjswigut.eventide.ui.theme.PrimaryLight
import kotlinx.collections.immutable.ImmutableList

@Composable
fun MenuFab(
    isExpanded: Boolean,
    mainButton: @Composable () -> Unit,
    menuButtons: ImmutableList<@Composable () -> Unit>
) {


    Box(modifier = Modifier.background(color = PrimaryLight, shape = CircleShape)) {

    }
}

