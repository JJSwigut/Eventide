package com.jjswigut.eventide.ui.components


import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
