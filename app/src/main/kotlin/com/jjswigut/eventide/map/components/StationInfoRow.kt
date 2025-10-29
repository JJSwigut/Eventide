package com.jjswigut.eventide.map.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.jjswigut.eventide.R
import com.jjswigut.eventide.data.models.TideDay
import com.jjswigut.eventide.map.MapAction
import com.jjswigut.eventide.ui.components.Action
import com.jjswigut.eventide.ui.theme.Primary
import com.jjswigut.eventide.ui.theme.PrimaryDark
import com.jjswigut.eventide.ui.theme.PrimaryLight

private object StationInfoDesign {
  val closeButtonSize = 48.dp
  val closeIconSize = 28.dp
  val closeButtonPadding = 12.dp
  val closeButtonCornerRadius = 12.dp
  val closeButtonBorderWidth = 2.dp

  val cardSpacing = 16.dp
  val cardPadding = 16.dp
}

@Composable
fun StationInfoRow(
    modifier: Modifier = Modifier,
    list: List<TideDay>,
    actionHandler: (Action) -> Unit,
) {
  Column(modifier = modifier) {
    CloseButton(
      modifier = Modifier.align(Alignment.End),
      onClick = { actionHandler(MapAction.CloseTides) }
    )

    TideCardList(list = list)
  }
}

@Composable
private fun CloseButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
  val gradientBrush = Brush.radialGradient(
    colors = listOf(
      PrimaryLight,
      Primary,
      PrimaryDark
    )
  )

  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .padding(
        bottom = StationInfoDesign.closeButtonPadding,
        end = 20.dp
      )
      .size(StationInfoDesign.closeButtonSize)
      .background(
        brush = gradientBrush,
        shape = RoundedCornerShape(StationInfoDesign.closeButtonCornerRadius)
      )
      .border(
        width = StationInfoDesign.closeButtonBorderWidth,
        color = Color.White.copy(alpha = 0.5f),
        shape = RoundedCornerShape(StationInfoDesign.closeButtonCornerRadius)
      )
      .clip(shape = RoundedCornerShape(StationInfoDesign.closeButtonCornerRadius))
      .clickable(onClick = onClick)
  ) {
    Image(
      painter = painterResource(id = R.drawable.close_icon),
      contentDescription = "Close",
      modifier = Modifier.size(StationInfoDesign.closeIconSize)
    )
  }
}

@Composable
private fun TideCardList(list: List<TideDay>) {
  LazyRow(
    contentPadding = PaddingValues(
      start = StationInfoDesign.cardPadding,
      end = StationInfoDesign.cardPadding
    ),
    horizontalArrangement = Arrangement.spacedBy(StationInfoDesign.cardSpacing)
  ) {
    items(list) { day ->
      TideCard(day = day)
    }
  }
}
