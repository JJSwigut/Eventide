package com.jjswigut.eventide.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ShimmerLoading(
  modifier: Modifier = Modifier,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
  val shimmerTranslate by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1000f,
    animationSpec = infiniteRepeatable(
      animation = tween(
        durationMillis = 1200,
        easing = LinearEasing
      ),
      repeatMode = RepeatMode.Restart
    ),
    label = "shimmer"
  )

  val shimmerColors = listOf(
    Color.White.copy(alpha = 0.3f),
    Color.White.copy(alpha = 0.5f),
    Color.White.copy(alpha = 0.3f)
  )

  val brush = Brush.linearGradient(
    colors = shimmerColors,
    start = Offset(shimmerTranslate - 300f, shimmerTranslate - 300f),
    end = Offset(shimmerTranslate, shimmerTranslate)
  )

  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    // Temperature placeholder
    Box(
      modifier = Modifier
        .fillMaxWidth(0.7f)
        .height(20.dp)
        .background(brush, RoundedCornerShape(4.dp))
    )

    // Conditions placeholder
    Box(
      modifier = Modifier
        .fillMaxWidth(0.85f)
        .height(20.dp)
        .background(brush, RoundedCornerShape(4.dp))
    )

    // Wind placeholder
    Box(
      modifier = Modifier
        .fillMaxWidth(0.6f)
        .height(20.dp)
        .background(brush, RoundedCornerShape(4.dp))
    )
  }
}
