package com.jjswigut.eventide.map.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jjswigut.eventide.data.models.Tide
import com.jjswigut.eventide.data.models.TideDay
import com.jjswigut.eventide.data.models.Weather
import com.jjswigut.eventide.ui.components.BodyText
import com.jjswigut.eventide.ui.components.DateHeaderText
import com.jjswigut.eventide.ui.components.EnhancedBodyText
import com.jjswigut.eventide.ui.components.LargeBodyText
import com.jjswigut.eventide.ui.components.SectionTitleText
import com.jjswigut.eventide.ui.components.ShimmerLoading
import com.jjswigut.eventide.ui.theme.BackgroundDark
import com.jjswigut.eventide.ui.theme.PrimaryLight
import kotlin.math.max
import kotlin.math.min

// Design System Constants
private object TideCardDesign {
    val cardCornerRadius = 16.dp
    val sectionCornerRadius = 12.dp

    val cardBorderWidth = 2.dp
    val cardBackgroundAlpha = 0.85f
    val cardBorderAlpha = 0.6f

    val tidesBackgroundAlpha = 0.9f
    val weatherBackgroundAlpha = 0.95f

    // Base values for 700dp height (scale proportionally from these)
    const val BASE_HEIGHT = 700f
    const val MIN_SCALE = 0.55f
    const val MAX_SCALE = 1.15f
}

// Adaptive sizing that scales proportionally with available space
private class AdaptiveSize(availableHeightDp: Float) {
    // Calculate scale factor (clamped between min and max)
    private val scaleFactor = min(
        TideCardDesign.MAX_SCALE,
        max(TideCardDesign.MIN_SCALE, availableHeightDp / TideCardDesign.BASE_HEIGHT),
    )

    // All sizes scale proportionally
    val cardPadding: Dp = (16 * scaleFactor).dp
    val sectionSpacing: Dp = (10 * scaleFactor).dp
    val contentPadding: Dp = (12 * scaleFactor).dp
    val emojiSize: TextUnit = (44 * scaleFactor).sp
    val temperatureFontSize: TextUnit = (28 * scaleFactor).sp
    val tempBoxPaddingHorizontal: Dp = (16 * scaleFactor).dp
    val tempBoxPaddingVertical: Dp = (8 * scaleFactor).dp
    val tempLabelFontSize: TextUnit = (10 * scaleFactor).sp
    val tempArrowFontSize: TextUnit = (13 * scaleFactor).sp
    val iconSize: Dp = (20 * scaleFactor).dp
    val tideRowPadding: Dp = (1 * scaleFactor).dp
    val windFontSize: TextUnit = (13 * scaleFactor).sp
    val letterSpacing: TextUnit = (1.0 * scaleFactor).sp
    val weatherSpacing: Dp = (4 * scaleFactor).dp
    val sectionHeaderPadding: Dp = (6 * scaleFactor).dp
}

// Weather Temperature Colors
private object TemperatureColors {
    val lowBackground = Color(0xFF4A90E2).copy(alpha = 0.25f)
    val lowText = Color(0xFF1E4A6F)
    val highBackground = Color(0xFFFF6B35).copy(alpha = 0.25f)
    val highText = Color(0xFFC53D0F)
    val labelText = Color.White
}

@Composable
fun TideCard(day: TideDay) {
    val boxWidth = LocalConfiguration.current.screenWidthDp - 48

    @Suppress("UnusedBoxWithConstraintsScope")
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxHeight(.8f)
            .width(boxWidth.dp)
            .background(
                color = BackgroundDark.copy(alpha = TideCardDesign.cardBackgroundAlpha),
                shape = RoundedCornerShape(TideCardDesign.cardCornerRadius),
            )
            .border(
                width = TideCardDesign.cardBorderWidth,
                color = PrimaryLight.copy(alpha = TideCardDesign.cardBorderAlpha),
                shape = RoundedCornerShape(TideCardDesign.cardCornerRadius),
            ),
        contentAlignment = Alignment.Center,
    ) {
        val availableHeight = maxHeight
        val adaptiveSize = remember(availableHeight) { AdaptiveSize(availableHeight.value) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(adaptiveSize.cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            DateHeader(date = day.date, adaptiveSize = adaptiveSize)

            Spacer(Modifier.height(adaptiveSize.sectionSpacing))

            TidesSection(
                modifier = Modifier.weight(0.4f),
                day = day,
                adaptiveSize = adaptiveSize,
            )

            Spacer(Modifier.height(adaptiveSize.sectionSpacing))

            WeatherSection(
                modifier = Modifier.weight(0.6f),
                day = day,
                adaptiveSize = adaptiveSize,
            )
        }
    }
}

@Composable
private fun DateHeader(
    date: String,
    adaptiveSize: AdaptiveSize,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = PrimaryLight,
                shape = RoundedCornerShape(TideCardDesign.sectionCornerRadius),
            ),
        contentAlignment = Alignment.Center,
    ) {
        DateHeaderText(
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = adaptiveSize.sectionHeaderPadding,
            ),
            text = date,
        )
    }
}

@Composable
private fun TidesSection(
    modifier: Modifier = Modifier,
    day: TideDay,
    adaptiveSize: AdaptiveSize,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = PrimaryLight.copy(alpha = TideCardDesign.tidesBackgroundAlpha),
                shape = RoundedCornerShape(TideCardDesign.sectionCornerRadius),
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SectionTitleText(
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = adaptiveSize.sectionHeaderPadding,
            ),
            text = "Tides",
        )

        if (day.isTidesLoading) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(
                        horizontal = adaptiveSize.contentPadding,
                        vertical = adaptiveSize.sectionHeaderPadding,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                ShimmerLoading(modifier = Modifier.fillMaxWidth())
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(
                        vertical = adaptiveSize.tideRowPadding,
                        horizontal = 8.dp,
                    ),
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                day.tides.forEach { tide ->
                    TideRow(tide = tide, adaptiveSize = adaptiveSize)
                }
            }
        }
    }
}

@Composable
private fun WeatherSection(
    modifier: Modifier = Modifier,
    day: TideDay,
    adaptiveSize: AdaptiveSize,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = PrimaryLight.copy(alpha = TideCardDesign.weatherBackgroundAlpha),
                shape = RoundedCornerShape(TideCardDesign.sectionCornerRadius),
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SectionTitleText(
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = adaptiveSize.sectionHeaderPadding,
            ),
            text = "Weather",
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(
                    horizontal = adaptiveSize.contentPadding,
                    vertical = adaptiveSize.sectionHeaderPadding,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            when {
                day.isWeatherLoading -> {
                    ShimmerLoading(modifier = Modifier.fillMaxWidth())
                }

                day.weather != null -> {
                    WeatherContent(weather = day.weather, adaptiveSize = adaptiveSize)
                }

                else -> {
                    BodyText(
                        text = "Weather unavailable",
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun TideRow(
    tide: Tide,
    adaptiveSize: AdaptiveSize,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = adaptiveSize.contentPadding,
                vertical = adaptiveSize.tideRowPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Icon(
            modifier = Modifier.size(adaptiveSize.iconSize),
            painter = painterResource(id = tide.tideValue.iconRes),
            contentDescription = null,
            tint = BackgroundDark,
        )

        Spacer(Modifier.width(6.dp))

        EnhancedBodyText(
            modifier = Modifier.weight(1f),
            text = tide.time,
            textAlign = TextAlign.Start,
            fontWeight = FontWeight.Medium,
        )

        EnhancedBodyText(
            text = tide.height,
            textAlign = TextAlign.End,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun WeatherContent(
    weather: Weather,
    adaptiveSize: AdaptiveSize,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxSize(),
    ) {
        Text(
            text = weather.getWeatherEmoji(),
            fontSize = adaptiveSize.emojiSize,
        )

        LargeBodyText(
            text = weather.conditions,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TemperatureBox(
                label = "LOW",
                temperature = weather.lowTemp,
                arrow = "↓",
                backgroundColor = TemperatureColors.lowBackground,
                textColor = TemperatureColors.lowText,
                adaptiveSize = adaptiveSize,
            )

            TemperatureBox(
                label = "HIGH",
                temperature = weather.highTemp,
                arrow = "↑",
                backgroundColor = TemperatureColors.highBackground,
                textColor = TemperatureColors.highText,
                adaptiveSize = adaptiveSize,
            )
        }

        WindInfo(windSpeed = weather.windSpeed, adaptiveSize = adaptiveSize)
    }
}

@Composable
private fun TemperatureBox(
    label: String,
    temperature: Int,
    arrow: String,
    backgroundColor: Color,
    textColor: Color,
    adaptiveSize: AdaptiveSize,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(
                horizontal = adaptiveSize.tempBoxPaddingHorizontal,
                vertical = adaptiveSize.tempBoxPaddingVertical,
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                fontSize = adaptiveSize.tempLabelFontSize,
                fontWeight = FontWeight.Black,
                color = TemperatureColors.labelText,
                letterSpacing = adaptiveSize.letterSpacing,
            )
            Text(
                text = arrow,
                fontSize = adaptiveSize.tempArrowFontSize,
                color = TemperatureColors.labelText,
                fontWeight = FontWeight.Bold,
            )
        }

        Text(
            text = "$temperature°",
            fontSize = adaptiveSize.temperatureFontSize,
            fontWeight = FontWeight.Black,
            color = textColor,
        )
    }
}

@Composable
private fun WindInfo(
    windSpeed: String,
    adaptiveSize: AdaptiveSize,
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .background(
                color = PrimaryLight.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = "Wind:",
            fontSize = adaptiveSize.windFontSize,
            fontWeight = FontWeight.Bold,
            color = BackgroundDark,
        )
        Spacer(Modifier.width(6.dp))
        EnhancedBodyText(
            text = windSpeed,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}
