package com.jjswigut.eventide.settings

internal const val GOVERNMENT_INFORMATION_DISCLAIMER =
    "Eventide is an independent app. It does not represent, and is not affiliated with or endorsed by, NOAA, the National Weather Service, the National Data Buoy Center, or any government entity."

internal data class GovernmentDataSource(
    val name: String,
    val description: String,
    val url: String,
)

internal val governmentDataSources = listOf(
    GovernmentDataSource(
        name = "NOAA Tides & Currents",
        description = "Tide predictions and station observations",
        url = "https://tidesandcurrents.noaa.gov/",
    ),
    GovernmentDataSource(
        name = "National Weather Service",
        description = "Weather forecasts and marine alerts",
        url = "https://www.weather.gov/",
    ),
    GovernmentDataSource(
        name = "National Data Buoy Center",
        description = "Buoy and marine observations",
        url = "https://www.ndbc.noaa.gov/",
    ),
    GovernmentDataSource(
        name = "NOAA nowCOAST",
        description = "Radar and satellite map overlays",
        url = "https://nowcoast.noaa.gov/",
    ),
    GovernmentDataSource(
        name = "NOAA/NWS Map Services",
        description = "Weather map overlays",
        url = "https://mapservices.weather.noaa.gov/",
    ),
)
