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
import com.jjswigut.eventide.data.models.SunMoonData
import com.jjswigut.eventide.data.models.Tide
import com.jjswigut.eventide.data.models.TideDay
import com.jjswigut.eventide.data.models.Weather
import com.jjswigut.eventide.settings.AppSettings
import com.jjswigut.eventide.settings.displayHeight
import com.jjswigut.eventide.settings.displayTemperature
import com.jjswigut.eventide.settings.displayTime
import com.jjswigut.eventide.ui.components.BodyText
import com.jjswigut.eventide.ui.components.DateHeaderText
import com.jjswigut.eventide.ui.components.EnhancedBodyText
import com.jjswigut.eventide.ui.components.LargeBodyText
import com.jjswigut.eventide.ui.components.SectionTitleText
import com.jjswigut.eventide.ui.components.ShimmerLoading
import com.jjswigut.eventide.ui.theme.BackgroundDark
import com.jjswigut.eventide.ui.theme.PrimaryLight
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
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
    val astroFontSize: TextUnit = (12 * scaleFactor).sp
    val letterSpacing: TextUnit = (1.0 * scaleFactor).sp
    val weatherSpacing: Dp = (4 * scaleFactor).dp
    val sectionHeaderPadding: Dp = (6 * scaleFactor).dp
    val astroPadding: Dp = (8 * scaleFactor).dp
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
fun TideCard(
    day: TideDay,
    settings: AppSettings,
) {
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

            day.sunMoonData?.let { sunMoonData ->
                Spacer(Modifier.height(adaptiveSize.sectionSpacing))
                SunMoonRow(
                    sunMoonData = sunMoonData,
                    adaptiveSize = adaptiveSize,
                    settings = settings,
                )
            }

            Spacer(Modifier.height(adaptiveSize.sectionSpacing))

            TidesSection(
                modifier = Modifier.weight(0.5f),
                day = day,
                adaptiveSize = adaptiveSize,
                settings = settings,
            )

            Spacer(Modifier.height(adaptiveSize.sectionSpacing))

            WeatherSection(
                modifier = Modifier.weight(0.5f),
                day = day,
                adaptiveSize = adaptiveSize,
                settings = settings,
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
private fun SunMoonRow(
    sunMoonData: SunMoonData,
    adaptiveSize: AdaptiveSize,
    settings: AppSettings,
) {
    val sunriseText = sunMoonData.sunrise?.displayTime(settings) ?: "--"
    val sunsetText = sunMoonData.sunset?.displayTime(settings) ?: "--"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = PrimaryLight.copy(alpha = 0.65f),
                shape = RoundedCornerShape(TideCardDesign.sectionCornerRadius),
            )
            .padding(horizontal = adaptiveSize.astroPadding, vertical = adaptiveSize.sectionHeaderPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = "☀ $sunriseText / $sunsetText",
            fontSize = adaptiveSize.astroFontSize,
            fontWeight = FontWeight.Bold,
            color = BackgroundDark,
            maxLines = 1,
        )

        Text(
            text = "${sunMoonData.moonPhase.icon} ${sunMoonData.moonIlluminationPercent}%",
            fontSize = adaptiveSize.astroFontSize,
            fontWeight = FontWeight.Bold,
            color = BackgroundDark,
            maxLines = 1,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun TidesSection(
    modifier: Modifier = Modifier,
    day: TideDay,
    adaptiveSize: AdaptiveSize,
    settings: AppSettings,
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
        } else if (day.tides.isEmpty()) {
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
                BodyText(
                    text = "Tides unavailable",
                    textAlign = TextAlign.Center,
                )
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
                verticalArrangement = Arrangement.spacedBy(adaptiveSize.weatherSpacing),
            ) {
                TideGraph(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    tides = day.tides,
                    settings = settings,
                )

                day.tides.forEach { tide ->
                    TideRow(tide = tide, adaptiveSize = adaptiveSize, settings = settings)
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
    settings: AppSettings,
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
                    WeatherContent(weather = day.weather, adaptiveSize = adaptiveSize, settings = settings)
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
    settings: AppSettings,
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
            text = tide.displayTime(settings),
            textAlign = TextAlign.Start,
            fontWeight = FontWeight.Medium,
        )

        EnhancedBodyText(
            text = tide.displayHeight(settings),
            textAlign = TextAlign.End,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun WeatherContent(
    weather: Weather,
    adaptiveSize: AdaptiveSize,
    settings: AppSettings,
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
                temperature = weather.lowTemp.displayTemperature(settings),
                arrow = "↓",
                backgroundColor = TemperatureColors.lowBackground,
                textColor = TemperatureColors.lowText,
                adaptiveSize = adaptiveSize,
            )

            TemperatureBox(
                label = "HIGH",
                temperature = weather.highTemp.displayTemperature(settings),
                arrow = "↑",
                backgroundColor = TemperatureColors.highBackground,
                textColor = TemperatureColors.highText,
                adaptiveSize = adaptiveSize,
            )
        }

        WindInfo(windSpeed = weather.windSpeed, adaptiveSize = adaptiveSize)

        BodyText(
            text = weather.sourceLabel(),
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

private fun Weather.sourceLabel(): String {
    val validity = forecastStart?.let { start ->
        val end = forecastEnd
        if (end == null) {
            "valid ${start.shortLabel()}"
        } else {
            "valid ${start.shortLabel()}-${end.shortLabel()}"
        }
    }
    val issued = forecastIssuedAt?.let { "issued ${it.shortLabel()}" }
    return listOfNotNull(
        "$source ${sourceType.lowercase(Locale.US)}",
        validity,
        issued,
    ).joinToString(" - ")
}

private fun OffsetDateTime.shortLabel(): String {
    return forecastTimeFormatter.format(this)
}

private val forecastTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d h:mma", Locale.US)

@Composable
private fun TemperatureBox(
    label: String,
    temperature: String,
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
            text = temperature,
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
