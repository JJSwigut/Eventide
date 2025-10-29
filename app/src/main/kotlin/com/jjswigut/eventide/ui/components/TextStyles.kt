package com.jjswigut.eventide.ui.components


import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.jjswigut.eventide.ui.theme.AppColor


@Composable
fun H1Text(
    text: String,
    maxLines: Int = Int.MAX_VALUE,
    color: Color = AppColor.darkText
) {
    Text(
        text = text,
        maxLines = maxLines,
        color = color,
        fontSize = 28.sp
    )
}

@Composable
fun DateHeaderText(
  modifier: Modifier = Modifier,
  text: String,
  maxLines: Int = Int.MAX_VALUE,
  textAlign: TextAlign = TextAlign.Center,
  color: Color = AppColor.darkText,
) {
  Text(
    modifier = modifier,
    text = text,
    textAlign = textAlign,
    maxLines = maxLines,
    color = color,
    fontSize = 20.sp,
    fontWeight = FontWeight.Bold
  )
}

@Composable
fun SectionTitleText(
  modifier: Modifier = Modifier,
  text: String,
  maxLines: Int = Int.MAX_VALUE,
  textAlign: TextAlign = TextAlign.Start,
  color: Color = AppColor.darkText,
) {
  Text(
    modifier = modifier,
    text = text,
    textAlign = textAlign,
    maxLines = maxLines,
    color = color,
    fontSize = 18.sp,
    fontWeight = FontWeight.SemiBold
  )
}

@Composable
fun TitleText(
    modifier: Modifier = Modifier,
    text: String,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign = TextAlign.Start,
    color: Color = AppColor.darkText
) {
    Text(
        modifier = modifier,
        text = text,
        textAlign = textAlign,
        maxLines = maxLines,
        color = color,
        fontSize = 17.sp
    )
}

@Composable
fun EnhancedBodyText(
  modifier: Modifier = Modifier,
  text: String,
  maxLines: Int = Int.MAX_VALUE,
  textAlign: TextAlign = TextAlign.Start,
  color: Color = AppColor.darkText,
  fontWeight: FontWeight = FontWeight.Normal,
) {
  Text(
    modifier = modifier,
    text = text,
    textAlign = textAlign,
    maxLines = maxLines,
    color = color,
    fontSize = 15.sp,
    fontWeight = fontWeight
  )
}

@Composable
fun BodyText(
    modifier: Modifier = Modifier,
    text: String,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign = TextAlign.Start,
    color: Color = AppColor.darkText
) {
    Text(
        modifier = modifier,
        text = text,
        textAlign = textAlign,
        maxLines = maxLines,
        color = color,
        fontSize = 13.sp
    )
}

@Composable
fun LargeBodyText(
  modifier: Modifier = Modifier,
  text: String,
  maxLines: Int = Int.MAX_VALUE,
  textAlign: TextAlign = TextAlign.Start,
  color: Color = AppColor.darkText,
  fontWeight: FontWeight = FontWeight.Normal,
) {
  Text(
    modifier = modifier,
    text = text,
    textAlign = textAlign,
    maxLines = maxLines,
    color = color,
    fontSize = 16.sp,
    fontWeight = fontWeight
  )
}

@Composable
fun TemperatureText(
  modifier: Modifier = Modifier,
  text: String,
  maxLines: Int = Int.MAX_VALUE,
  textAlign: TextAlign = TextAlign.Center,
  color: Color = AppColor.darkText,
) {
  Text(
    modifier = modifier,
    text = text,
    textAlign = textAlign,
    maxLines = maxLines,
    color = color,
    fontSize = 24.sp,
    fontWeight = FontWeight.Bold
  )
}

@Composable
fun ButtonText(
    text: String,
    maxLines: Int = Int.MAX_VALUE,
    color: Color = AppColor.darkText
) {
    Text(
        text = text,
        maxLines = maxLines,
        color = color,
        fontSize = 11.sp
    )
}
