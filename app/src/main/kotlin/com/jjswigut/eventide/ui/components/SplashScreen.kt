package com.jjswigut.eventide.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.jjswigut.eventide.R

@Composable
fun SplashScreen(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        exit = fadeOut(tween(500)),
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(color = Color(0xFF65BAC3)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_eventide_foreground),
                contentDescription = "Eventide Logo",
                modifier = Modifier.size(240.dp),
            )
        }
    }
}
