package com.jjswigut.eventide.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jjswigut.eventide.R
import com.jjswigut.eventide.ui.theme.LightText
import com.jjswigut.eventide.ui.theme.PrimaryDark
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MenuButton(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    centerButton: MenuItem,
    menuButtons: List<MenuItem>,
    actionHandler: (Action) -> Unit,
) {
    val mainButtonSize = 50.dp
    val spacing = 8.dp
    val containerSize = mainButtonSize + calculateMaxExtension(menuButtons.size, mainButtonSize, spacing)
    val expansionTransition = updateTransition(targetState = expanded, label = "Menu expansion")

    Box(
        modifier = modifier
            .padding(16.dp)
            .size(containerSize),
        contentAlignment = Alignment.BottomEnd,
    ) {
        val angleOffset = 45f
        val baseOffset = mainButtonSize + spacing

        menuButtons.forEachIndexed { index, menuButton ->
            val progress = expansionTransition.animateFloat(
                transitionSpec = {
                    tween(
                        durationMillis = if (targetState) 220 else 140,
                        delayMillis = if (targetState) index * 30 else 0,
                        easing = FastOutSlowInEasing,
                    )
                },
                label = "Menu option $index progress",
            ) { isExpanded ->
                if (isExpanded) 1f else 0f
            }
            val angle = menuButtonAngle(index, angleOffset)
            val scale = 0.8f + (0.2f * progress.value)

            OutsideButton(
                menuButton = menuButton,
                enabled = expanded,
                modifier = Modifier
                    .offset(
                        x = cos(angle) * baseOffset * progress.value,
                        y = sin(angle) * baseOffset * progress.value,
                    )
                    .graphicsLayer {
                        alpha = progress.value
                        scaleX = scale
                        scaleY = scale
                    },
                onClick = actionHandler,
            )
        }

        Box(
            modifier = Modifier
                .size(mainButtonSize)
                .background(shape = CircleShape, color = PrimaryDark)
                .clip(CircleShape)
                .shadow(10.dp, CircleShape)
                .clickable { actionHandler(centerButton.action) },
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(
                targetState = expanded,
                animationSpec = tween(durationMillis = 120),
                label = "Menu icon",
            ) { isExpanded ->
                Icon(
                    painter = painterResource(if (isExpanded) R.drawable.close_icon else centerButton.iconRes),
                    contentDescription = null,
                    tint = LightText,
                )
            }
        }
    }
}

private fun calculateMaxExtension(count: Int, buttonSize: Dp, spacing: Dp): Dp {
    return (buttonSize + spacing) * count
}

private fun menuButtonAngle(
    index: Int,
    angleOffset: Float,
): Double {
    return Math.toRadians((-90f - (angleOffset * index)).toDouble())
}

private operator fun Double.times(dp: Dp): Dp = (this * dp.value).dp

@Composable
private fun OutsideButton(
    modifier: Modifier,
    menuButton: MenuItem,
    enabled: Boolean,
    onClick: (Action) -> Unit,
) {
    Column(
        modifier = modifier
            .size(40.dp)
            .background(shape = CircleShape, color = PrimaryDark)
            .clip(CircleShape)
            .shadow(8.dp, CircleShape)
            .clickable(enabled = enabled) {
                onClick(menuButton.action)
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(id = menuButton.iconRes),
            contentDescription = null,
            tint = LightText,
        )
    }
}

data class MenuItem(
    val text: String,
    @DrawableRes val iconRes: Int,
    val action: Action,
)

interface Action
