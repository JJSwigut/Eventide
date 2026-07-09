package com.jjswigut.eventide.map.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.jjswigut.eventide.data.models.Tide
import com.jjswigut.eventide.data.models.TideValue
import com.jjswigut.eventide.settings.AppSettings
import com.jjswigut.eventide.settings.displayHeight
import com.jjswigut.eventide.settings.displayTime
import com.jjswigut.eventide.ui.components.EventideMotion
import com.jjswigut.eventide.ui.components.rememberEventideMotionEnabled
import com.jjswigut.eventide.ui.theme.BackgroundDark
import com.jjswigut.eventide.ui.theme.Primary
import com.jjswigut.eventide.ui.theme.PrimaryDark
import com.jjswigut.eventide.ui.theme.SecondaryLight
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun TideGraph(
    tides: List<Tide>,
    settings: AppSettings,
    modifier: Modifier = Modifier,
) {
    val points = remember(tides, settings) {
        tides.mapNotNull { tide ->
            val dateTime = tide.dateTime
            val heightFeet = tide.heightFeet
            if (dateTime == null || heightFeet == null) {
                null
            } else {
                TideGraphPoint(
                    dateTime = dateTime,
                    heightFeet = heightFeet,
                    tideValue = tide.tideValue,
                    timeLabel = tide.displayTime(settings),
                    heightLabel = tide.displayHeight(settings),
                )
            }
        }.sortedBy { it.dateTime }
    }
    val motionEnabled = rememberEventideMotionEnabled()
    val revealProgress = remember { Animatable(if (motionEnabled) 0f else 1f) }

    LaunchedEffect(points, motionEnabled) {
        if (!motionEnabled) {
            revealProgress.snapTo(1f)
            return@LaunchedEffect
        }

        revealProgress.snapTo(0f)
        revealProgress.animateTo(1f, EventideMotion.entranceSpring)
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (points.size < MIN_POINT_COUNT) return@Canvas

        val reveal = revealProgress.value.coerceIn(0f, 1f)
        val leftPadding = 32.dp.toPx()
        val rightPadding = 12.dp.toPx()
        val topPadding = 14.dp.toPx()
        val bottomPadding = 28.dp.toPx()
        val graphWidth = size.width - leftPadding - rightPadding
        val graphHeight = size.height - topPadding - bottomPadding
        if (graphWidth <= 0f || graphHeight <= 0f) return@Canvas

        val minTime = points.first().dateTime
        val maxTime = points.last().dateTime
        val minHeight = points.minOf { it.heightFeet }
        val maxHeight = points.maxOf { it.heightFeet }
        val heightPadding = max(MIN_HEIGHT_PADDING, (maxHeight - minHeight) * HEIGHT_PADDING_RATIO)
        val chartMinHeight = minHeight - heightPadding
        val chartMaxHeight = maxHeight + heightPadding
        val totalMinutes = max(1L, Duration.between(minTime, maxTime).toMinutes()).toDouble()

        fun xFor(dateTime: LocalDateTime): Float {
            val minutes = Duration.between(minTime, dateTime).toMinutes().toDouble()
            return leftPadding + (minutes / totalMinutes * graphWidth).toFloat()
        }

        fun yFor(heightFeet: Double): Float {
            val ratio = (heightFeet - chartMinHeight) / (chartMaxHeight - chartMinHeight)
            return topPadding + ((1.0 - ratio) * graphHeight).toFloat()
        }

        drawLine(
            color = BackgroundDark.copy(alpha = AXIS_ALPHA),
            start = Offset(leftPadding, topPadding),
            end = Offset(leftPadding, topPadding + graphHeight),
            strokeWidth = AXIS_WIDTH,
        )
        drawLine(
            color = BackgroundDark.copy(alpha = AXIS_ALPHA),
            start = Offset(leftPadding, topPadding + graphHeight),
            end = Offset(leftPadding + graphWidth, topPadding + graphHeight),
            strokeWidth = AXIS_WIDTH,
        )

        val curvePath = Path()
        val samples = buildCurveSamples(points)
        val visibleSamples = samples.take(visibleTideSampleCount(samples.size, reveal))
        val baselineY = topPadding + graphHeight
        val waterPath = Path()

        visibleSamples.firstOrNull()?.let { firstSample ->
            val firstPoint = Offset(xFor(firstSample.dateTime), yFor(firstSample.heightFeet))
            waterPath.moveTo(firstPoint.x, baselineY)
            waterPath.lineTo(firstPoint.x, firstPoint.y)
        }

        visibleSamples.forEachIndexed { index, sample ->
            val point = Offset(xFor(sample.dateTime), yFor(sample.heightFeet))
            if (index == 0) {
                curvePath.moveTo(point.x, point.y)
            } else {
                curvePath.lineTo(point.x, point.y)
            }
            if (index > 0) {
                waterPath.lineTo(point.x, point.y)
            }
        }
        visibleSamples.lastOrNull()?.let { lastSample ->
            waterPath.lineTo(xFor(lastSample.dateTime), baselineY)
            waterPath.close()
            drawPath(
                path = waterPath,
                color = PrimaryDark.copy(alpha = WATER_FILL_ALPHA * reveal),
            )
        }
        if (visibleSamples.size >= MIN_POINT_COUNT) {
            drawPath(
                path = curvePath,
                color = PrimaryDark,
                style = Stroke(width = CURVE_WIDTH, cap = StrokeCap.Round),
            )
        }

        val visibleEndX = visibleSamples.lastOrNull()?.let { xFor(it.dateTime) } ?: leftPadding
        val labelAlpha = ((reveal - LABEL_REVEAL_START) / (1f - LABEL_REVEAL_START)).coerceIn(0f, 1f)
        points.forEach { point ->
            val center = Offset(xFor(point.dateTime), yFor(point.heightFeet))
            if (center.x <= visibleEndX + POINT_VISIBILITY_PADDING && labelAlpha > 0f) {
                val pointColor = if (point.tideValue == TideValue.High) Primary else BackgroundDark
                drawCircle(
                    color = pointColor.copy(alpha = labelAlpha),
                    radius = EXTREME_RADIUS,
                    center = center,
                )
                drawLabel(
                    text = "${point.timeLabel}\n${point.heightLabel}",
                    x = center.x,
                    y = if (point.tideValue == TideValue.High) {
                        center.y - LABEL_VERTICAL_OFFSET
                    } else {
                        center.y + LABEL_VERTICAL_OFFSET
                    },
                    color = BackgroundDark.copy(alpha = labelAlpha),
                    alignCenter = true,
                )
            }
        }

        val now = LocalDateTime.now()
        if (points.first().dateTime.toLocalDate() == LocalDate.now() &&
            !now.isBefore(minTime) &&
            !now.isAfter(maxTime)
        ) {
            val nowHeight = interpolateHeight(points, now)
            val nowX = xFor(now)
            val nowY = yFor(nowHeight)
            drawLine(
                color = SecondaryLight,
                start = Offset(nowX, topPadding),
                end = Offset(nowX, topPadding + graphHeight),
                strokeWidth = NOW_LINE_WIDTH,
            )
            drawCircle(
                color = SecondaryLight,
                radius = NOW_RADIUS,
                center = Offset(nowX, nowY),
            )
            drawLabel(
                text = "now",
                x = nowX,
                y = max(topPadding + LABEL_TEXT_SIZE, nowY - NOW_LABEL_OFFSET),
                color = BackgroundDark,
                alignCenter = true,
            )
        }

        drawLabel(
            text = points.first().timeLabel,
            x = leftPadding,
            y = size.height - 6.dp.toPx(),
            color = BackgroundDark.copy(alpha = LABEL_ALPHA),
            alignCenter = false,
        )
        drawLabel(
            text = points.last().timeLabel,
            x = leftPadding + graphWidth,
            y = size.height - 6.dp.toPx(),
            color = BackgroundDark.copy(alpha = LABEL_ALPHA),
            alignCenter = true,
        )
    }
}

private fun buildCurveSamples(points: List<TideGraphPoint>): List<TideGraphPoint> {
    return points.zipWithNext().flatMap { (start, end) ->
        (0..SAMPLES_PER_SEGMENT).map { sampleIndex ->
            val fraction = sampleIndex.toDouble() / SAMPLES_PER_SEGMENT
            val segmentDuration = Duration.between(start.dateTime, end.dateTime)
            val sampleTime = start.dateTime.plusSeconds((segmentDuration.seconds * fraction).toLong())
            start.copy(
                dateTime = sampleTime,
                heightFeet = interpolateHeight(start.heightFeet, end.heightFeet, fraction),
            )
        }
    }.distinctBy { it.dateTime }
}

internal fun visibleTideSampleCount(totalSamples: Int, progress: Float): Int {
    if (totalSamples <= 0) return 0
    if (totalSamples == 1) return 1

    val clampedProgress = progress.coerceIn(0f, 1f)
    if (clampedProgress >= 1f) return totalSamples

    val revealedSamples = 2 + ((totalSamples - 2) * clampedProgress).roundToInt()
    return revealedSamples.coerceIn(2, totalSamples)
}

private fun interpolateHeight(points: List<TideGraphPoint>, dateTime: LocalDateTime): Double {
    val segment = points.zipWithNext().firstOrNull { (start, end) ->
        !dateTime.isBefore(start.dateTime) && !dateTime.isAfter(end.dateTime)
    } ?: return points.first().heightFeet

    val (start, end) = segment
    val segmentMinutes = max(1L, Duration.between(start.dateTime, end.dateTime).toMinutes()).toDouble()
    val elapsedMinutes = Duration.between(start.dateTime, dateTime).toMinutes().toDouble()
    val fraction = (elapsedMinutes / segmentMinutes).coerceIn(0.0, 1.0)
    return interpolateHeight(start.heightFeet, end.heightFeet, fraction)
}

private fun interpolateHeight(
    startHeight: Double,
    endHeight: Double,
    fraction: Double,
): Double {
    return startHeight + (endHeight - startHeight) * (1 - cos(PI * fraction)) / 2
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLabel(
    text: String,
    x: Float,
    y: Float,
    color: Color,
    alignCenter: Boolean,
) {
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        textSize = LABEL_TEXT_SIZE
        this.color = color.toArgb()
        textAlign = if (alignCenter) android.graphics.Paint.Align.CENTER else android.graphics.Paint.Align.LEFT
    }
    val lines = text.split('\n')
    lines.forEachIndexed { index, line ->
        drawContext.canvas.nativeCanvas.drawText(
            line,
            x,
            y + index * LABEL_LINE_HEIGHT,
            paint,
        )
    }
}

private fun Color.toArgb(): Int {
    return android.graphics.Color.argb(
        (alpha * COLOR_COMPONENT_MAX).toInt(),
        (red * COLOR_COMPONENT_MAX).toInt(),
        (green * COLOR_COMPONENT_MAX).toInt(),
        (blue * COLOR_COMPONENT_MAX).toInt(),
    )
}

private data class TideGraphPoint(
    val dateTime: LocalDateTime,
    val heightFeet: Double,
    val tideValue: TideValue,
    val timeLabel: String,
    val heightLabel: String,
)

private const val MIN_POINT_COUNT = 2
private const val SAMPLES_PER_SEGMENT = 18
private const val HEIGHT_PADDING_RATIO = 0.15
private const val MIN_HEIGHT_PADDING = 0.4
private const val AXIS_ALPHA = 0.24f
private const val AXIS_WIDTH = 2f
private const val CURVE_WIDTH = 5f
private const val EXTREME_RADIUS = 5.5f
private const val NOW_RADIUS = 6.5f
private const val NOW_LINE_WIDTH = 2.5f
private const val LABEL_VERTICAL_OFFSET = 12f
private const val NOW_LABEL_OFFSET = 12f
private const val LABEL_TEXT_SIZE = 24f
private const val LABEL_LINE_HEIGHT = 24f
private const val LABEL_ALPHA = 0.78f
private const val LABEL_REVEAL_START = 0.68f
private const val WATER_FILL_ALPHA = 0.16f
private const val POINT_VISIBILITY_PADDING = 1f
private const val COLOR_COMPONENT_MAX = 255
