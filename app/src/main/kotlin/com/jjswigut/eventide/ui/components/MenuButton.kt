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
import androidx.compose.ui.platform.LocalDensity
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

    Box(
        modifier = modifier
            .animateContentSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
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
            val mainButtonSize = 50.dp
            val spacing = 8.dp
            val angleOffset = 45f

            menuButtons.forEachIndexed { index, menuButton ->
                val angleInRadians = Math.toRadians(angleOffset.toDouble() * index)
                val baseOffset = with(
                    LocalDensity.current,
                ) { mainButtonSize.toPx() + spacing.toPx() }

                val xOffset = (cos(angleInRadians) * baseOffset * index).dp
                val yOffset = (sin(angleInRadians) * baseOffset * index).dp

                val outsideButtonModifier = if (index == 0) {
                    Modifier.offset(y = yOffset + mainButtonSize)
                } else {
                    Modifier.offset(x = xOffset, y = yOffset + mainButtonSize)
                }

                OutsideButton(
                    menuButton = menuButton,
                    modifier = outsideButtonModifier,
                    onClick = actionHandler,
                )
            }
        }
    }
}

private fun calculateMaxExtension(count: Int, buttonSize: Dp, spacing: Dp): Dp {
    return (buttonSize + spacing) * count
}

@Composable
private fun OutsideButton(
    modifier: Modifier,
    menuButton: MenuItem,
    onClick: (Action) -> Unit,
) {
    Column(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable {
                onClick(menuButton.action)
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(id = menuButton.iconRes),
            contentDescription = null,
        )
    }
}

data class MenuItem(
    val text: String,
    @DrawableRes val iconRes: Int,
    val action: Action,
)

interface Action
