package com.jjswigut.eventide.map.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jjswigut.eventide.R
import com.jjswigut.eventide.data.models.Station
import com.jjswigut.eventide.ui.theme.BackgroundDark
import com.jjswigut.eventide.ui.theme.LightText
import com.jjswigut.eventide.ui.theme.Primary
import com.jjswigut.eventide.ui.theme.PrimaryDark
import com.jjswigut.eventide.ui.theme.PrimaryLight
import com.jjswigut.eventide.ui.theme.SecondaryLight

private object FavoritesPanelDesign {
    val cornerRadius = 8.dp
    val panelPadding = 16.dp
    val rowPadding = 12.dp
    val rowSpacing = 6.dp
    val maxHeight = 340.dp
    val iconSize = 24.dp
}

@Composable
fun FavoritesPanel(
    favorites: List<Station>,
    onFavoriteClick: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            BackgroundDark.copy(alpha = 0.96f),
            PrimaryDark.copy(alpha = 0.96f),
        ),
    )

    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth()
            .heightIn(max = FavoritesPanelDesign.maxHeight)
            .background(
                brush = gradientBrush,
                shape = RoundedCornerShape(FavoritesPanelDesign.cornerRadius),
            )
            .border(
                width = 1.dp,
                color = PrimaryLight.copy(alpha = 0.45f),
                shape = RoundedCornerShape(FavoritesPanelDesign.cornerRadius),
            )
            .padding(FavoritesPanelDesign.panelPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.star_filled),
                contentDescription = null,
                modifier = Modifier.size(FavoritesPanelDesign.iconSize),
            )
            Text(
                modifier = Modifier.weight(1f),
                text = "Saved stations",
                color = LightText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            ClosePanelButton(onClick = onClose)
        }

        if (favorites.isEmpty()) {
            Text(
                text = "Star a tide station to save it here.",
                color = SecondaryLight,
                fontSize = 14.sp,
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(FavoritesPanelDesign.rowSpacing),
            ) {
                items(favorites, key = { it.id }) { station ->
                    FavoriteStationRow(
                        station = station,
                        onClick = { onFavoriteClick(station.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteStationRow(
    station: Station,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FavoritesPanelDesign.cornerRadius))
            .clickable(onClick = onClick)
            .background(Primary.copy(alpha = 0.22f))
            .padding(FavoritesPanelDesign.rowPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            painter = painterResource(id = R.drawable.map_pin),
            contentDescription = null,
            modifier = Modifier.size(30.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = station.name,
                color = LightText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = station.state,
                color = SecondaryLight,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ClosePanelButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(FavoritesPanelDesign.cornerRadius))
            .clickable(onClick = onClick)
            .background(PrimaryLight.copy(alpha = 0.22f)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.close_icon),
            contentDescription = "Close saved stations",
            modifier = Modifier.size(20.dp),
        )
    }
}
