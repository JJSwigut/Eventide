package com.jjswigut.eventide.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jjswigut.eventide.R
import com.jjswigut.eventide.ui.theme.BackgroundDark
import com.jjswigut.eventide.ui.theme.LightText
import com.jjswigut.eventide.ui.theme.PrimaryDark
import com.jjswigut.eventide.ui.theme.SecondaryLight
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MenuButton(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    centerButton: MenuItem,
    menuButtons: List<MenuItem>,
    selectedActions: Set<Action> = emptySet(),
    actionHandler: (Action) -> Unit,
) {
    val motionEnabled = rememberEventideMotionEnabled()
    val mainButtonSize = 58.dp
    val menuButtonSize = 48.dp
    val spacing = 10.dp
    val containerSize = mainButtonSize + calculateMaxExtension(menuButtons.size, mainButtonSize, spacing)
    val expansionTransition = updateTransition(targetState = expanded, label = "Menu expansion")

    Box(
        modifier = modifier
            .padding(24.dp)
            .size(containerSize),
        contentAlignment = Alignment.BottomEnd,
    ) {
        val menuRadius = 136.dp
        val centerAlignmentCorrection = (mainButtonSize - menuButtonSize) / 2f

        menuButtons.forEachIndexed { index, menuButton ->
            val animatedProgress = expansionTransition.animateFloat(
                transitionSpec = {
                    spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = if (targetState) {
                            Spring.StiffnessMediumLow
                        } else {
                            Spring.StiffnessMedium
                        },
                    )
                },
                label = "Menu option $index progress",
            ) { isExpanded ->
                if (isExpanded) 1f else 0f
            }
            val progress = if (motionEnabled) {
                animatedProgress.value
            } else if (expanded) {
                1f
            } else {
                0f
            }
            val angle = menuButtonAngle(index, menuButtons.size)
            val scale = 0.8f + (0.2f * progress)

            if (expanded || progress > 0.01f) {
                OutsideButton(
                    menuButton = menuButton,
                    enabled = expanded,
                    selected = selectedActions.contains(menuButton.action),
                    size = menuButtonSize,
                    modifier = Modifier
                        .offset(
                            x = (cos(angle) * menuRadius * progress) - centerAlignmentCorrection,
                            y = (sin(angle) * menuRadius * progress) - centerAlignmentCorrection,
                        )
                        .graphicsLayer {
                            alpha = progress
                            scaleX = scale
                            scaleY = scale
                        },
                    onClick = actionHandler,
                )
            }
        }

        Box(
            modifier = Modifier
                .size(mainButtonSize)
                .shadow(10.dp, CircleShape)
                .background(shape = CircleShape, color = PrimaryDark)
                .clip(CircleShape)
                .clickable(role = Role.Button) { actionHandler(centerButton.action) },
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(
                targetState = expanded,
                animationSpec = tween(durationMillis = if (motionEnabled) 120 else 0),
                label = "Menu icon",
            ) { isExpanded ->
                Icon(
                    painter = painterResource(if (isExpanded) R.drawable.close_icon else centerButton.iconRes),
                    contentDescription = if (isExpanded) "Close menu" else "Open menu",
                    tint = LightText,
                )
            }
        }
    }
}

private fun calculateMaxExtension(count: Int, buttonSize: Dp, spacing: Dp): Dp {
    return (buttonSize + spacing) * count
}

internal fun menuButtonAngle(
    index: Int,
    count: Int,
): Double {
    if (count <= 1) return -PI / 2

    val fraction = index.coerceIn(0, count - 1).toDouble() / (count - 1)
    return Math.toRadians(MENU_START_DEGREES + (MENU_SWEEP_DEGREES * fraction))
}

private operator fun Double.times(dp: Dp): Dp = (this * dp.value).dp

@Composable
private fun OutsideButton(
    modifier: Modifier,
    menuButton: MenuItem,
    enabled: Boolean,
    selected: Boolean,
    size: Dp,
    onClick: (Action) -> Unit,
) {
    val motionEnabled = rememberEventideMotionEnabled()
    val backgroundColor = animateColorAsState(
        targetValue = if (selected) SecondaryLight else PrimaryDark,
        animationSpec = tween(durationMillis = if (motionEnabled) 140 else 0),
        label = "${menuButton.text} background",
    )
    val iconColor = animateColorAsState(
        targetValue = if (selected) BackgroundDark else LightText,
        animationSpec = tween(durationMillis = if (motionEnabled) 140 else 0),
        label = "${menuButton.text} icon",
    )

    Column(
        modifier = modifier
            .size(size)
            .shadow(8.dp, CircleShape)
            .background(shape = CircleShape, color = backgroundColor.value)
            .clip(CircleShape)
            .semantics {
                contentDescription = menuButton.text
                stateDescription = if (selected) "Selected" else "Not selected"
            }
            .clickable(
                enabled = enabled,
                role = Role.Button,
            ) {
                onClick(menuButton.action)
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(id = menuButton.iconRes),
            contentDescription = null,
            tint = iconColor.value,
        )
    }
}

data class MenuItem(
    val text: String,
    @DrawableRes val iconRes: Int,
    val action: Action,
)

interface Action

private const val MENU_START_DEGREES = -90.0
private const val MENU_SWEEP_DEGREES = -90.0
