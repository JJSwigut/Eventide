package com.jjswigut.eventide.map.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jjswigut.eventide.R
import com.jjswigut.eventide.data.models.MarineConditions
import com.jjswigut.eventide.data.models.Station
import com.jjswigut.eventide.data.models.TideDay
import com.jjswigut.eventide.map.MapAction
import com.jjswigut.eventide.settings.AppSettings
import com.jjswigut.eventide.ui.components.Action
import com.jjswigut.eventide.ui.components.BodyText
import com.jjswigut.eventide.ui.components.SectionTitleText
import com.jjswigut.eventide.ui.components.ShimmerLoading
import com.jjswigut.eventide.ui.theme.BackgroundDark
import com.jjswigut.eventide.ui.theme.Primary
import com.jjswigut.eventide.ui.theme.PrimaryDark
import com.jjswigut.eventide.ui.theme.PrimaryLight
import java.util.Locale

private object StationInfoDesign {
    val closeButtonSize = 48.dp
    val closeIconSize = 28.dp
    val closeButtonPadding = 12.dp
    val closeButtonCornerRadius = 12.dp
    val closeButtonBorderWidth = 2.dp

    val cardSpacing = 16.dp
    val cardPadding = 16.dp
    val marinePanelCornerRadius = 12.dp
    val marinePanelPadding = 12.dp
    val marineChipCornerRadius = 8.dp
}

@Composable
fun StationInfoRow(
    modifier: Modifier = Modifier,
    list: List<TideDay>,
    marineConditions: MarineConditions?,
    isMarineConditionsLoading: Boolean,
    station: Station?,
    isFavorite: Boolean,
    settings: AppSettings,
    actionHandler: (Action) -> Unit,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.align(Alignment.End),
        ) {
            station?.let {
                FavoriteButton(
                    isFavorite = isFavorite,
                    onClick = { actionHandler(MapAction.ToggleFavorite) },
                )
            }
            CloseButton(
                onClick = { actionHandler(MapAction.CloseTides) },
            )
        }

        MarineSummary(
            marineConditions = marineConditions,
            isLoading = isMarineConditionsLoading,
        )

        TideCardList(list = list, settings = settings)
    }
}

@Composable
private fun MarineSummary(
    marineConditions: MarineConditions?,
    isLoading: Boolean,
) {
    if (!isLoading && marineConditions == null) return

    val boxWidth = LocalConfiguration.current.screenWidthDp - 48

    Column(
        modifier = Modifier
            .padding(
                start = StationInfoDesign.cardPadding,
                end = StationInfoDesign.cardPadding,
                bottom = StationInfoDesign.cardSpacing,
            )
            .width(boxWidth.dp)
            .background(
                color = BackgroundDark.copy(alpha = 0.85f),
                shape = RoundedCornerShape(StationInfoDesign.marinePanelCornerRadius),
            )
            .border(
                width = 1.dp,
                color = PrimaryLight.copy(alpha = 0.6f),
                shape = RoundedCornerShape(StationInfoDesign.marinePanelCornerRadius),
            )
            .padding(StationInfoDesign.marinePanelPadding),
    ) {
        SectionTitleText(
            text = "Marine conditions",
            color = Color.White,
        )

        if (isLoading) {
            ShimmerLoading(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
            return@Column
        }

        val conditions = marineConditions ?: return@Column
        val chips = remember(conditions) { conditions.toMarineChips() }

        conditions.alerts.firstOrNull()?.let { alert ->
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = "${alert.event}: ${alert.severity}",
                color = Color(0xFFFFD166),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BodyText(
                text = alert.headline,
                color = Color.White,
                maxLines = 2,
            )
        }

        if (chips.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(chips) { chip ->
                    MarineChip(chip)
                }
            }
        }
    }
}

@Composable
private fun MarineChip(text: String) {
    Text(
        modifier = Modifier
            .background(
                color = PrimaryLight.copy(alpha = 0.25f),
                shape = RoundedCornerShape(StationInfoDesign.marineChipCornerRadius),
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        text = text,
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

private fun MarineConditions.toMarineChips(): List<String> {
    val chips = mutableListOf<String>()
    observations.take(4).forEach { observation ->
        chips += "${observation.label}: ${observation.value}"
    }
    buoy?.let { buoy ->
        val distance = String.format(Locale.US, "%.0f", buoy.distanceMiles)
        val details = listOfNotNull(
            buoy.waveHeight?.let { "waves $it" },
            buoy.wind?.let { "wind $it" },
            buoy.waterTemperature?.let { "water $it" },
        ).joinToString(", ")
        chips += if (details.isBlank()) {
            "Buoy ${buoy.stationId}: ${distance}mi"
        } else {
            "Buoy ${buoy.stationId}: $details"
        }
    }
    waveForecast?.let { forecast ->
        val details = listOfNotNull(
            forecast.waveHeight?.let { "waves $it" },
            forecast.wavePeriod?.let { "period $it" },
            forecast.seaSurfaceTemperature?.let { "SST $it" },
        ).joinToString(", ")
        if (details.isNotBlank()) {
            chips += "${forecast.attribution}: $details"
        }
    }
    if (alerts.size > 1) {
        chips += "${alerts.size} active alerts"
    }
    return chips
}

@Composable
private fun FavoriteButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
) {
    HeaderButton(
        iconRes = if (isFavorite) R.drawable.star_filled else R.drawable.star_outline,
        contentDescription = if (isFavorite) "Remove favorite" else "Save favorite",
        onClick = onClick,
    )
}

@Composable
private fun CloseButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    HeaderButton(
        modifier = modifier,
        iconRes = R.drawable.close_icon,
        contentDescription = "Close",
        onClick = onClick,
    )
}

@Composable
private fun HeaderButton(
    modifier: Modifier = Modifier,
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val gradientBrush = Brush.radialGradient(
        colors = listOf(
            PrimaryLight,
            Primary,
            PrimaryDark,
        ),
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .padding(
                bottom = StationInfoDesign.closeButtonPadding,
                end = StationInfoDesign.closeButtonPadding,
            )
            .size(StationInfoDesign.closeButtonSize)
            .background(
                brush = gradientBrush,
                shape = RoundedCornerShape(StationInfoDesign.closeButtonCornerRadius),
            )
            .border(
                width = StationInfoDesign.closeButtonBorderWidth,
                color = Color.White.copy(alpha = 0.5f),
                shape = RoundedCornerShape(StationInfoDesign.closeButtonCornerRadius),
            )
            .clip(shape = RoundedCornerShape(StationInfoDesign.closeButtonCornerRadius))
            .clickable(onClick = onClick),
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(StationInfoDesign.closeIconSize),
        )
    }
}

@Composable
private fun TideCardList(
    list: List<TideDay>,
    settings: AppSettings,
) {
    LazyRow(
        contentPadding = PaddingValues(
            start = StationInfoDesign.cardPadding,
            end = StationInfoDesign.cardPadding,
        ),
        horizontalArrangement = Arrangement.spacedBy(StationInfoDesign.cardSpacing),
    ) {
        items(list) { day ->
            TideCard(day = day, settings = settings)
        }
    }
}
