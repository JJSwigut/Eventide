package com.jjswigut.eventide.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jjswigut.eventide.R
import com.jjswigut.eventide.ui.theme.AppColor
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
    val isExpanded by remember(expanded) { mutableStateOf(expanded) }
    val mainButtonSize = 50.dp
    val spacing = 8.dp
    val containerSize = if (isExpanded) {
        mainButtonSize + calculateMaxExtension(menuButtons.size, mainButtonSize, spacing)
    } else {
        mainButtonSize
    }

    Box(
        modifier = modifier
            .animateContentSize()
            .padding(16.dp)
            .size(containerSize),
        contentAlignment = Alignment.BottomStart,
    ) {
        Box(
            modifier = Modifier
                .size(mainButtonSize)
                .background(shape = CircleShape, color = AppColor.container)
                .clip(CircleShape)
                .shadow(10.dp, CircleShape)
                .clickable { actionHandler(centerButton.action) },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(if (isExpanded) R.drawable.close_icon else centerButton.iconRes),
                contentDescription = null,
            )
        }

        if (isExpanded) {
            val angleOffset = 45f
            val baseOffset = mainButtonSize + spacing

            menuButtons.forEachIndexed { index, menuButton ->
                OutsideButton(
                    menuButton = menuButton,
                    modifier = Modifier.offset(
                        x = cos(menuButtonAngle(index, angleOffset)) * baseOffset,
                        y = sin(menuButtonAngle(index, angleOffset)) * baseOffset,
                    ),
                    onClick = actionHandler,
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
    return Math.toRadians((-90f + (angleOffset * index)).toDouble())
}

private operator fun Double.times(dp: Dp): Dp = (this * dp.value).dp

@Composable
private fun OutsideButton(
    modifier: Modifier,
    menuButton: MenuItem,
    onClick: (Action) -> Unit,
) {
    Column(
        modifier = modifier
            .size(40.dp)
            .background(shape = CircleShape, color = AppColor.container)
            .clip(CircleShape)
            .shadow(8.dp, CircleShape)
            .clickable {
                onClick(menuButton.action)
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(id = menuButton.iconRes),
            contentDescription = null,
            tint = Color.Unspecified,
        )
    }
}

data class MenuItem(
    val text: String,
    @DrawableRes val iconRes: Int,
    val action: Action,
)

interface Action
