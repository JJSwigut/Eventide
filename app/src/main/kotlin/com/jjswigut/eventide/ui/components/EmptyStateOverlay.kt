package com.jjswigut.eventide.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jjswigut.eventide.R

@Composable
fun EmptyStateOverlay(
  visible: Boolean,
  onNavigateToNearest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  AnimatedVisibility(
    visible = visible,
    enter = fadeIn(tween(300)),
    exit = fadeOut(tween(300))
  ) {
    Box(
      modifier = modifier
        .fillMaxSize()
        .background(color = Color.Black.copy(alpha = 0.75f)),
      contentAlignment = Alignment.Center
    ) {
      Column(
        modifier = Modifier
          .padding(32.dp)
          .background(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
          )
          .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Image(
          painter = painterResource(id = R.drawable.map_pin),
          contentDescription = null,
          modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "No Tide Stations Nearby",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "Tap on a tide station marker to view tides and weather information for that location.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
          onClick = onNavigateToNearest,
          shape = RoundedCornerShape(12.dp)
        ) {
          Text(
            text = "Find Nearest Station",
            style = MaterialTheme.typography.labelLarge
          )
        }
      }
    }
  }
}
