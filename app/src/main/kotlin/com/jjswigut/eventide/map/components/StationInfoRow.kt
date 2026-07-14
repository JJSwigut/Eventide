package com.jjswigut.eventide.map.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jjswigut.eventide.R
import com.jjswigut.eventide.data.models.BuoyConditions
import com.jjswigut.eventide.data.models.MarineConditions
import com.jjswigut.eventide.data.models.MarineObservation
import com.jjswigut.eventide.data.models.Station
import com.jjswigut.eventide.data.models.TideDay
import com.jjswigut.eventide.map.MapAction
import com.jjswigut.eventide.settings.AppSettings
import com.jjswigut.eventide.ui.components.Action
import com.jjswigut.eventide.ui.components.BodyText
import com.jjswigut.eventide.ui.components.EnhancedBodyText
import com.jjswigut.eventide.ui.components.EventideMotion
import com.jjswigut.eventide.ui.components.SectionTitleText
import com.jjswigut.eventide.ui.components.ShimmerLoading
import com.jjswigut.eventide.ui.components.rememberEventideEntranceProgress
import com.jjswigut.eventide.ui.components.rememberEventideMotionEnabled
import com.jjswigut.eventide.ui.theme.BackgroundDark
import com.jjswigut.eventide.ui.theme.Primary
import com.jjswigut.eventide.ui.theme.PrimaryDark
import com.jjswigut.eventide.ui.theme.PrimaryLight
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
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
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val detailScrollState = rememberScrollState()
    val entranceProgress = rememberEventideEntranceProgress(station?.id ?: list.firstOrNull()?.date)
    val usableScreenHeight = configuration.screenHeightDp.dp - statusBarPadding
    val detailViewportHeight = (usableScreenHeight - 72.dp).coerceAtLeast(480.dp)
    val tideCardHeight = usableScreenHeight * 0.72f

    LaunchedEffect(station?.id) {
        detailScrollState.scrollTo(0)
    }

    Column(
        modifier = modifier
            .padding(top = statusBarPadding)
            .graphicsLayer {
                alpha = entranceProgress
                scaleX = DETAIL_MIN_SCALE + ((1f - DETAIL_MIN_SCALE) * entranceProgress)
                scaleY = DETAIL_MIN_SCALE + ((1f - DETAIL_MIN_SCALE) * entranceProgress)
                translationY = with(density) { DETAIL_ENTRANCE_OFFSET.toPx() } * (1f - entranceProgress)
            },
    ) {
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

        Column(
            modifier = Modifier
                .height(detailViewportHeight)
                .verticalScroll(detailScrollState),
        ) {
            TideCardList(
                list = list,
                settings = settings,
                cardHeight = tideCardHeight,
            )

            MarineSummary(
                marineConditions = marineConditions,
                isLoading = isMarineConditionsLoading,
            )

            Spacer(Modifier.height(StationInfoDesign.cardSpacing))
        }
    }
}

@Composable
private fun MarineSummary(
    marineConditions: MarineConditions?,
    isLoading: Boolean,
) {
    val boxWidth = LocalConfiguration.current.screenWidthDp - 48

    Column(
        modifier = Modifier
            .padding(
                start = StationInfoDesign.cardPadding,
                end = StationInfoDesign.cardPadding,
                top = StationInfoDesign.cardSpacing,
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

        when {
            isLoading -> {
                ShimmerLoading(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }

            marineConditions == null -> {
                BodyText(
                    modifier = Modifier.padding(top = 8.dp),
                    text = "Marine conditions unavailable for this station.",
                    color = Color.White.copy(alpha = 0.82f),
                )
            }

            else -> {
                MarineConditionsContent(marineConditions)
            }
        }
    }
}

@Composable
private fun MarineConditionsContent(conditions: MarineConditions) {
    val now = remember(conditions) { Instant.now() }
    var hasPreviousSection = false

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
        BodyText(
            text = alert.sourceAttribution(),
            color = Color.White.copy(alpha = 0.72f),
            maxLines = 1,
        )
        hasPreviousSection = true
    }

    conditions.buoy?.let { buoy ->
        MarineSectionDivider(visible = hasPreviousSection)
        MarineBuoySection(buoy = buoy, now = now)
        hasPreviousSection = true
    }

    conditions.observations.take(4).forEach { observation ->
        MarineSectionDivider(visible = hasPreviousSection)
        MarineObservationSection(observation = observation, now = now)
        hasPreviousSection = true
    }

    if (conditions.alerts.size > 1) {
        MarineSectionDivider(visible = hasPreviousSection)
        BodyText(
            text = "${conditions.alerts.size} active marine alerts",
            color = Color.White.copy(alpha = 0.82f),
        )
    }
}

@Composable
private fun MarineBuoySection(buoy: BuoyConditions, now: Instant) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            EnhancedBodyText(
                text = "Nearby buoy ${buoy.stationId}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            BodyText(
                text = buoy.stationName,
                color = Color.White.copy(alpha = 0.76f),
                maxLines = 2,
            )
        }
        EnhancedBodyText(
            modifier = Modifier.padding(start = 12.dp),
            text = "${String.format(Locale.US, "%.0f", buoy.distanceMiles)} mi",
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
    }

    listOfNotNull(
        buoy.wind?.let { "Wind" to it },
        buoy.waveHeight?.let { "Waves" to it },
        buoy.wavePeriod?.let { "Period" to it },
        buoy.waterTemperature?.let { "Water" to it },
        buoy.pressure?.let { "Pressure" to it },
    ).forEach { (label, value) ->
        MarineDataRow(label = label, value = value)
    }

    MarineMetadata(
        source = buoy.source,
        freshness = buoy.observedAt.marineFreshnessLabel(now, BUOY_STALE_AFTER),
    )
}

@Composable
private fun MarineObservationSection(observation: MarineObservation, now: Instant) {
    MarineDataRow(label = observation.label, value = observation.value)
    MarineMetadata(
        source = observation.source,
        freshness = observation.observedAt.marineFreshnessLabel(now, OBSERVATION_STALE_AFTER),
    )
}

@Composable
private fun MarineDataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BodyText(
            modifier = Modifier.weight(1f),
            text = label,
            color = Color.White.copy(alpha = 0.78f),
        )
        EnhancedBodyText(
            modifier = Modifier.padding(start = 12.dp),
            text = value,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun MarineMetadata(source: String, freshness: String?) {
    BodyText(
        modifier = Modifier.padding(top = 2.dp),
        text = listOfNotNull(source, freshness).joinToString(" - "),
        color = Color.White.copy(alpha = 0.64f),
        maxLines = 1,
    )
}

@Composable
private fun MarineSectionDivider(visible: Boolean) {
    if (!visible) return
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp),
        color = PrimaryLight.copy(alpha = 0.32f),
    )
}

private fun com.jjswigut.eventide.data.models.MarineAlert.sourceAttribution(): String {
    val timing = listOfNotNull(
        effectiveAt?.let { "from ${it.shortUtcLabel()}" },
        expiresAt?.let { "until ${it.shortUtcLabel()}" },
    ).joinToString(" ")
    return listOf(source, timing.takeIf { it.isNotBlank() }).joinToString(" - ")
}

internal fun Instant?.marineFreshnessLabel(now: Instant, staleAfter: Duration): String? {
    val observedAt = this ?: return null
    val age = Duration.between(observedAt, now).coerceAtLeast(Duration.ZERO)
    val ageLabel = when {
        age.toMinutes() < 1 -> "Updated now"
        age.toMinutes() < 60 -> "Updated ${age.toMinutes()}m ago"
        age.toHours() < 24 -> "Updated ${age.toHours()}h ago"
        else -> "Updated ${age.toDays()}d ago"
    }
    return if (age > staleAfter) "$ageLabel - Stale" else ageLabel
}

private fun Instant.shortUtcLabel(): String {
    return utcFormatter.format(this)
}

private val utcFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("MMM d HH:mm", Locale.US)
    .withZone(ZoneOffset.UTC)

private val OBSERVATION_STALE_AFTER: Duration = Duration.ofHours(6)
private val BUOY_STALE_AFTER: Duration = Duration.ofHours(3)

@Composable
private fun FavoriteButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
) {
    val motionEnabled = rememberEventideMotionEnabled()
    val pulseScale = remember { Animatable(1f) }
    var hasObservedState by remember { mutableStateOf(false) }

    LaunchedEffect(isFavorite, motionEnabled) {
        if (!hasObservedState) {
            hasObservedState = true
            pulseScale.snapTo(1f)
            return@LaunchedEffect
        }

        if (!motionEnabled) {
            pulseScale.snapTo(1f)
            return@LaunchedEffect
        }

        pulseScale.snapTo(0.94f)
        pulseScale.animateTo(1.12f, EventideMotion.confirmationSpring)
        pulseScale.animateTo(1f, EventideMotion.directSpring)
    }

    HeaderButton(
        modifier = Modifier.graphicsLayer {
            scaleX = pulseScale.value
            scaleY = pulseScale.value
        },
        iconRes = if (isFavorite) R.drawable.star_filled else R.drawable.star_outline,
        contentDescription = if (isFavorite) "Remove favorite" else "Save favorite",
        hapticFeedback = true,
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
    hapticFeedback: Boolean = false,
    onClick: () -> Unit,
) {
    val view = LocalView.current
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
            .clickable(role = Role.Button) {
                if (hapticFeedback) {
                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                }
                onClick()
            },
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(StationInfoDesign.closeIconSize),
        )
    }
}

private val DETAIL_ENTRANCE_OFFSET = 22.dp
private const val DETAIL_MIN_SCALE = 0.965f

@Composable
private fun TideCardList(
    list: List<TideDay>,
    settings: AppSettings,
    cardHeight: Dp,
) {
    LazyRow(
        contentPadding = PaddingValues(
            start = StationInfoDesign.cardPadding,
            end = StationInfoDesign.cardPadding,
        ),
        horizontalArrangement = Arrangement.spacedBy(StationInfoDesign.cardSpacing),
    ) {
        items(list) { day ->
            TideCard(
                modifier = Modifier.height(cardHeight),
                day = day,
                settings = settings,
            )
        }
    }
}
