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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jjswigut.eventide.R
import com.jjswigut.eventide.data.models.Station
import com.jjswigut.eventide.settings.AppSettings
import com.jjswigut.eventide.settings.GOVERNMENT_INFORMATION_DISCLAIMER
import com.jjswigut.eventide.settings.GovernmentDataSource
import com.jjswigut.eventide.settings.TempUnit
import com.jjswigut.eventide.settings.TideUnit
import com.jjswigut.eventide.settings.TimeFormat
import com.jjswigut.eventide.settings.governmentDataSources
import com.jjswigut.eventide.ui.components.rememberEventideEntranceProgress
import com.jjswigut.eventide.ui.theme.BackgroundDark
import com.jjswigut.eventide.ui.theme.LightText
import com.jjswigut.eventide.ui.theme.PrimaryDark
import com.jjswigut.eventide.ui.theme.PrimaryLight
import com.jjswigut.eventide.ui.theme.SecondaryLight

private object SettingsPanelDesign {
    val cornerRadius = 8.dp
    val panelPadding = 16.dp
    val maxHeight = 420.dp
    val iconSize = 24.dp
    val panelEntranceOffset = 18.dp
}

@Composable
fun SettingsPanel(
    settings: AppSettings,
    homeStation: Station?,
    onTideUnitSelected: (TideUnit) -> Unit,
    onTempUnitSelected: (TempUnit) -> Unit,
    onTimeFormatSelected: (TimeFormat) -> Unit,
    onClearHomeStation: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    val entranceProgress = rememberEventideEntranceProgress("settings")
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            BackgroundDark.copy(alpha = 0.96f),
            PrimaryDark.copy(alpha = 0.96f),
        ),
    )

    Box(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth()
            .heightIn(max = SettingsPanelDesign.maxHeight)
            .graphicsLayer {
                alpha = entranceProgress
                translationY = with(density) {
                    SettingsPanelDesign.panelEntranceOffset.toPx()
                } * (1f - entranceProgress)
            }
            .background(
                brush = gradientBrush,
                shape = RoundedCornerShape(SettingsPanelDesign.cornerRadius),
            )
            .border(
                width = 1.dp,
                color = PrimaryLight.copy(alpha = 0.45f),
                shape = RoundedCornerShape(SettingsPanelDesign.cornerRadius),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(SettingsPanelDesign.panelPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.settings_icon),
                    contentDescription = null,
                    modifier = Modifier.size(SettingsPanelDesign.iconSize),
                )
                Text(
                    modifier = Modifier.weight(1f),
                    text = "Settings & sources",
                    color = LightText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                ClosePanelButton(onClick = onClose)
            }

            Text(
                text = "Independent app - not affiliated with or endorsed by any government entity. Official sources are linked below.",
                color = SecondaryLight,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )

            SettingsGroup(title = "Tide height") {
                TideUnit.entries.forEach { tideUnit ->
                    SelectableChip(
                        text = tideUnit.label,
                        selected = settings.tideUnit == tideUnit,
                        onClick = { onTideUnitSelected(tideUnit) },
                    )
                }
            }

            SettingsGroup(title = "Temperature") {
                TempUnit.entries.forEach { tempUnit ->
                    SelectableChip(
                        text = tempUnit.label,
                        selected = settings.tempUnit == tempUnit,
                        onClick = { onTempUnitSelected(tempUnit) },
                    )
                }
            }

            SettingsGroup(title = "Time") {
                TimeFormat.entries.forEach { timeFormat ->
                    SelectableChip(
                        text = timeFormat.label,
                        selected = settings.timeFormat == timeFormat,
                        onClick = { onTimeFormatSelected(timeFormat) },
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Home station",
                    color = SecondaryLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = homeStation?.name ?: "No home station set",
                    color = LightText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (homeStation != null) {
                    SelectableChip(
                        text = "Clear home",
                        selected = false,
                        onClick = onClearHomeStation,
                    )
                }
            }

            GovernmentDataSourcesSection()
        }
    }
}

@Composable
private fun GovernmentDataSourcesSection() {
    val uriHandler = LocalUriHandler.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Government data sources",
            color = SecondaryLight,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = GOVERNMENT_INFORMATION_DISCLAIMER,
            color = LightText.copy(alpha = 0.82f),
            fontSize = 12.sp,
        )
        governmentDataSources.forEach { source ->
            GovernmentDataSourceLink(
                source = source,
                onClick = { uriHandler.openUri(source.url) },
            )
        }
        Text(
            text = "Conditions can change quickly. Confirm critical navigation and safety information with the official sources above.",
            color = LightText.copy(alpha = 0.72f),
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun GovernmentDataSourceLink(
    source: GovernmentDataSource,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(SettingsPanelDesign.cornerRadius))
            .clickable(
                role = Role.Button,
                onClickLabel = "Open ${source.name} official website",
                onClick = onClick,
            )
            .background(PrimaryDark.copy(alpha = 0.52f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = source.name,
                color = LightText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = source.url.removePrefix("https://").removeSuffix("/"),
                color = SecondaryLight.copy(alpha = 0.9f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = source.description,
                color = LightText.copy(alpha = 0.72f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            painter = painterResource(id = R.drawable.open_in_new),
            contentDescription = null,
            tint = SecondaryLight,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            color = SecondaryLight,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content()
        }
    }
}

@Composable
private fun SelectableChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .widthIn(min = 64.dp)
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(SettingsPanelDesign.cornerRadius))
            .clickable(onClick = onClick)
            .background(
                if (selected) {
                    PrimaryLight.copy(alpha = 0.42f)
                } else {
                    PrimaryDark.copy(alpha = 0.45f)
                },
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = LightText,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ClosePanelButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(SettingsPanelDesign.cornerRadius))
            .clickable(onClick = onClick)
            .background(PrimaryLight.copy(alpha = 0.22f)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.close_icon),
            contentDescription = "Close settings",
            modifier = Modifier.size(20.dp),
        )
    }
}
