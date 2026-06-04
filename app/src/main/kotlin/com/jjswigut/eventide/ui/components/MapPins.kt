package com.jjswigut.eventide.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jjswigut.eventide.R
import com.jjswigut.eventide.ui.theme.PrimaryDark
import com.jjswigut.eventide.ui.theme.PrimaryLight

@Composable
fun StationPin(name: String) {
    Column(
        modifier = Modifier.widthIn(max = 100.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = R.drawable.map_pin),
            contentDescription = null,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(2.dp))
        OptimizedStrokedText(text = name)
    }
}

@Composable
fun ClusterPin(size: String) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .background(PrimaryDark, shape = CircleShape)
            .border(width = 2.dp, color = PrimaryLight, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            modifier = Modifier.padding(2.dp),
            text = size,
            color = PrimaryLight,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun OptimizedStrokedText(
    text: String,
    modifier: Modifier = Modifier,
) {
    // Use a layered approach with caching for better performance
    val strokeStyle = remember {
        TextStyle(
            fontSize = 14.sp,
            fontWeight = FontWeight(700),
            drawStyle = Stroke(width = 7f, join = StrokeJoin.Round),
        )
    }

    val fillStyle = remember {
        TextStyle(
            fontSize = 14.sp,
            fontWeight = FontWeight(700),
        )
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        // Stroke layer
        Text(
            text = text,
            textAlign = TextAlign.Center,
            style = strokeStyle,
            overflow = TextOverflow.Ellipsis,
            color = Color.White,
            maxLines = 3,
        )
        // Fill layer
        Text(
            text = text,
            textAlign = TextAlign.Center,
            style = fillStyle,
            overflow = TextOverflow.Ellipsis,
            color = Color.Black,
            maxLines = 3,
        )
    }
}

@Composable
fun StrokedText(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Center,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    style: TextStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight(700)),
    maxLines: Int = 3,
    fillColor: Color = Color.Black,
    strokeColor: Color = Color.White,
    strokeWidth: Dp = 4.dp,
) {
    val strokeStyle = remember(style, strokeWidth) {
        style.copy(
            drawStyle = Stroke(width = strokeWidth.value + 3f, join = StrokeJoin.Round),
        )
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            textAlign = textAlign,
            style = strokeStyle,
            overflow = overflow,
            color = strokeColor,
            maxLines = maxLines,
        )
        Text(
            text = text,
            textAlign = textAlign,
            style = style,
            overflow = overflow,
            color = fillColor,
            maxLines = maxLines,
        )
    }
}
