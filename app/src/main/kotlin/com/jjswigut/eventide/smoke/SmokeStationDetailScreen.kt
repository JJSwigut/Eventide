package com.jjswigut.eventide.smoke

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.LatLng
import com.jjswigut.eventide.data.models.BuoyConditions
import com.jjswigut.eventide.data.models.MarineConditions
import com.jjswigut.eventide.data.models.MarineObservation
import com.jjswigut.eventide.data.models.Station
import com.jjswigut.eventide.data.models.Tide
import com.jjswigut.eventide.data.models.TideDay
import com.jjswigut.eventide.data.models.TideValue
import com.jjswigut.eventide.data.models.Weather
import com.jjswigut.eventide.map.MapAction
import com.jjswigut.eventide.map.components.StationInfoRow
import com.jjswigut.eventide.settings.AppSettings
import com.jjswigut.eventide.ui.theme.BackgroundDark
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime

@Composable
fun SmokeStationDetailScreen() {
    var showDetails by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentAlignment = Alignment.Center,
    ) {
        if (showDetails) {
            StationInfoRow(
                list = smokeTideDays,
                marineConditions = smokeMarineConditions,
                isMarineConditionsLoading = false,
                station = smokeStation,
                isFavorite = false,
                settings = AppSettings(),
                actionHandler = { action ->
                    if (action == MapAction.CloseTides) {
                        showDetails = false
                    }
                },
            )
        } else {
            Button(onClick = { showDetails = true }) {
                Text("Open Smoke Station")
            }
        }
    }
}

private val smokeStation = Station(
    id = "SMOKE",
    latLng = LatLng(41.3279, -71.9895),
    name = "Smoke Harbor",
    state = "RI",
)

private val smokeTideDays = listOf(
    TideDay(
        date = "Friday, Jul 10, 2026",
        dateValue = LocalDate.of(2026, 7, 10),
        tides = listOf(
            Tide(
                time = "1:14am",
                tideValue = TideValue.High,
                height = "3.94ft",
                dateTime = LocalDateTime.of(2026, 7, 10, 1, 14),
                heightFeet = 3.94,
            ),
            Tide(
                time = "7:38am",
                tideValue = TideValue.Low,
                height = "0.28ft",
                dateTime = LocalDateTime.of(2026, 7, 10, 7, 38),
                heightFeet = 0.28,
            ),
        ),
        weather = Weather(
            date = "Friday",
            highTemp = 78,
            lowTemp = 63,
            conditions = "Sunny",
            iconUrl = "https://api.weather.gov/icons/smoke",
            windSpeed = "5 to 10 mph",
            forecastStart = OffsetDateTime.parse("2026-07-10T06:00:00-04:00"),
            forecastEnd = OffsetDateTime.parse("2026-07-10T18:00:00-04:00"),
            forecastIssuedAt = OffsetDateTime.parse("2026-07-09T12:00:00-04:00"),
        ),
    ),
)

private val smokeMarineConditions = MarineConditions(
    observations = listOf(
        MarineObservation(
            label = "Water",
            value = "64.1°F",
            observedAt = Instant.parse("2026-07-10T12:00:00Z"),
        ),
        MarineObservation(
            label = "Wind",
            value = "SW 8.0kt",
            observedAt = Instant.parse("2026-07-10T12:00:00Z"),
        ),
    ),
    buoy = BuoyConditions(
        stationId = "44060",
        stationName = "Eastern Long Island Sound",
        distanceMiles = 12.4,
        wind = "220° 9.7kt gust 13.6kt",
        waveHeight = "3.9ft",
        waterTemperature = "64.6°F",
        observedAt = Instant.parse("2026-07-10T12:00:00Z"),
    ),
)
