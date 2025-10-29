package com.jjswigut.eventide.map.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

// Design System Constants
private object TideCardDesign {
  val cardHeight = 680.dp
  val cardCornerRadius = 16.dp
  val sectionCornerRadius = 12.dp

  val cardPadding = 20.dp
  val sectionSpacing = 12.dp
  val contentPadding = 16.dp

  val tidesWeight = 0.4f
  val weatherWeight = 0.6f

  val cardBorderWidth = 2.dp
  val cardBackgroundAlpha = 0.85f
  val cardBorderAlpha = 0.6f

  val tidesBackgroundAlpha = 0.9f
  val weatherBackgroundAlpha = 0.95f
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

  Box(
    modifier = Modifier
      .height(TideCardDesign.cardHeight)
      .width(boxWidth.dp)
      .background(
        color = BackgroundDark.copy(alpha = TideCardDesign.cardBackgroundAlpha),
        shape = RoundedCornerShape(TideCardDesign.cardCornerRadius)
      )
      .border(
        width = TideCardDesign.cardBorderWidth,
        color = PrimaryLight.copy(alpha = TideCardDesign.cardBorderAlpha),
        shape = RoundedCornerShape(TideCardDesign.cardCornerRadius)
      ),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(TideCardDesign.cardPadding),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      DateHeader(date = day.date)

      Spacer(Modifier.height(TideCardDesign.sectionSpacing))

      TidesSection(
        modifier = Modifier.weight(TideCardDesign.tidesWeight),
        day = day
      )

      Spacer(Modifier.height(TideCardDesign.sectionSpacing))

      WeatherSection(
        modifier = Modifier.weight(TideCardDesign.weatherWeight),
        day = day
      )
    }
  }
}

@Composable
private fun DateHeader(date: String) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .background(
        color = PrimaryLight,
        shape = RoundedCornerShape(TideCardDesign.sectionCornerRadius)
      ),
    contentAlignment = Alignment.Center,
  ) {
    DateHeaderText(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      text = date
    )
  }
}

@Composable
private fun TidesSection(
  modifier: Modifier,
  day: TideDay,
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(
        color = PrimaryLight.copy(alpha = TideCardDesign.tidesBackgroundAlpha),
        shape = RoundedCornerShape(TideCardDesign.sectionCornerRadius)
      ),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    SectionTitleText(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      text = "Tides"
    )

    if (day.isTidesLoading) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .padding(horizontal = TideCardDesign.contentPadding, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        ShimmerLoading(modifier = Modifier.fillMaxWidth())
      }
    } else {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.SpaceEvenly
      ) {
        day.tides.forEach { tide ->
          TideRow(tide = tide)
        }
      }
    }
  }
}

@Composable
private fun WeatherSection(
  modifier: Modifier,
  day: TideDay,
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(
        color = PrimaryLight.copy(alpha = TideCardDesign.weatherBackgroundAlpha),
        shape = RoundedCornerShape(TideCardDesign.sectionCornerRadius)
      ),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    SectionTitleText(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      text = "Weather"
    )

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
        .padding(horizontal = TideCardDesign.contentPadding, vertical = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceEvenly
    ) {
      when {
        day.isWeatherLoading -> {
          ShimmerLoading(modifier = Modifier.fillMaxWidth())
        }

        day.weather != null -> {
          WeatherContent(weather = day.weather)
        }

        else -> {
          BodyText(
            text = "Weather unavailable",
            textAlign = TextAlign.Center
          )
        }
      }
    }
  }
}

@Composable
private fun TideRow(tide: Tide) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 2.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Icon(
      modifier = Modifier.size(24.dp),
      painter = painterResource(id = tide.tideValue.iconRes),
      contentDescription = null,
      tint = BackgroundDark
    )

    Spacer(Modifier.width(8.dp))

    EnhancedBodyText(
      modifier = Modifier.weight(1f),
      text = tide.time,
      textAlign = TextAlign.Start,
      fontWeight = FontWeight.Medium
    )

    EnhancedBodyText(
      text = tide.height,
      textAlign = TextAlign.End,
      fontWeight = FontWeight.Bold
    )
  }
}

@Composable
private fun WeatherContent(weather: Weather) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(10.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Text(
      text = weather.getWeatherEmoji(),
      fontSize = 48.sp
    )

    LargeBodyText(
      text = weather.conditions,
      textAlign = TextAlign.Center,
      fontWeight = FontWeight.SemiBold,
      maxLines = 2
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically
    ) {
      TemperatureBox(
        label = "LOW",
        temperature = weather.lowTemp,
        arrow = "↓",
        backgroundColor = TemperatureColors.lowBackground,
        textColor = TemperatureColors.lowText
      )

      TemperatureBox(
        label = "HIGH",
        temperature = weather.highTemp,
        arrow = "↑",
        backgroundColor = TemperatureColors.highBackground,
        textColor = TemperatureColors.highText
      )
    }

    WindInfo(windSpeed = weather.windSpeed)
  }
}

@Composable
private fun TemperatureBox(
  label: String,
  temperature: Int,
  arrow: String,
  backgroundColor: Color,
  textColor: Color,
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(6.dp),
    modifier = Modifier
      .background(
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp)
      )
      .padding(horizontal = 20.dp, vertical = 12.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
      Text(
        text = label,
        fontSize = 13.sp,
        fontWeight = FontWeight.Black,
        color = TemperatureColors.labelText,
        letterSpacing = 1.5.sp
      )
      Text(
        text = arrow,
        fontSize = 16.sp,
        color = TemperatureColors.labelText,
        fontWeight = FontWeight.Bold
      )
    }

    Text(
      text = "$temperature°",
      fontSize = 32.sp,
      fontWeight = FontWeight.Black,
      color = textColor
    )
  }
}

@Composable
private fun WindInfo(windSpeed: String) {
  Row(
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth(0.85f)
      .background(
        color = PrimaryLight.copy(alpha = 0.3f),
        shape = RoundedCornerShape(8.dp)
      )
      .padding(horizontal = 12.dp, vertical = 8.dp)
  ) {
    Text(
      text = "Wind:",
      fontSize = 15.sp,
      fontWeight = FontWeight.Bold,
      color = BackgroundDark
    )
    Spacer(Modifier.width(6.dp))
    EnhancedBodyText(
      text = windSpeed,
      textAlign = TextAlign.Center,
      fontWeight = FontWeight.Medium,
      maxLines = 1
    )
  }
}
