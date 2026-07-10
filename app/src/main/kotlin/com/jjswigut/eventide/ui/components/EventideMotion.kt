package com.jjswigut.eventide.ui.components

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

object EventideMotion {
    val entranceSpring: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
    )
    val directSpring: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh,
    )
    val confirmationSpring: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessHigh,
    )
}

@Composable
fun rememberEventideMotionEnabled(): Boolean {
    return ValueAnimator.areAnimatorsEnabled()
}

@Composable
fun rememberEventideEntranceProgress(key: Any?): Float {
    val motionEnabled = rememberEventideMotionEnabled()
    val progress = remember { Animatable(if (motionEnabled) 0f else 1f) }

    LaunchedEffect(key, motionEnabled) {
        if (!motionEnabled) {
            progress.snapTo(1f)
            return@LaunchedEffect
        }

        progress.snapTo(0f)
        progress.animateTo(1f, EventideMotion.entranceSpring)
    }

    return progress.value
}
