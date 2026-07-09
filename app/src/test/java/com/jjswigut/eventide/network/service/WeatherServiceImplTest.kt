package com.jjswigut.eventide.network.service

import com.jjswigut.eventide.network.client.WeatherServiceClient
import com.jjswigut.eventide.network.responses.WeatherForecastProperties
import com.jjswigut.eventide.network.responses.WeatherPeriod
import io.ktor.client.engine.mock.MockEngine
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.OffsetDateTime

class WeatherServiceImplTest {
    @Test
    fun `convertPeriodsToDaily preserves forecast validity and issued times`() {
        val service = WeatherServiceImpl(WeatherServiceClient(MockEngine { error("No network expected") }))
        val weather = service.convertPeriodsToDaily(
            WeatherForecastProperties(
                updateTime = "2026-07-07T12:30:00-04:00",
                generatedAt = "2026-07-07T12:00:00-04:00",
                periods = listOf(
                    period(
                        name = "Today",
                        isDaytime = true,
                        temperature = 78,
                        startTime = "2026-07-07T06:00:00-04:00",
                        endTime = "2026-07-07T18:00:00-04:00",
                    ),
                    period(
                        name = "Tonight",
                        isDaytime = false,
                        temperature = 62,
                        startTime = "2026-07-07T18:00:00-04:00",
                        endTime = "2026-07-08T06:00:00-04:00",
                    ),
                ),
            ),
        )

        assertEquals(1, weather.size)
        assertEquals(78, weather.first().highTemp)
        assertEquals(62, weather.first().lowTemp)
        assertEquals(OffsetDateTime.parse("2026-07-07T06:00:00-04:00"), weather.first().forecastStart)
        assertEquals(OffsetDateTime.parse("2026-07-08T06:00:00-04:00"), weather.first().forecastEnd)
        assertEquals(OffsetDateTime.parse("2026-07-07T12:30:00-04:00"), weather.first().forecastIssuedAt)
        assertEquals("National Weather Service", weather.first().source)
        assertEquals("Forecast", weather.first().sourceType)
    }

    private fun period(
        name: String,
        isDaytime: Boolean,
        temperature: Int,
        startTime: String,
        endTime: String,
    ): WeatherPeriod {
        return WeatherPeriod(
            number = 1,
            name = name,
            startTime = startTime,
            endTime = endTime,
            isDaytime = isDaytime,
            temperature = temperature,
            temperatureUnit = "F",
            windSpeed = "5 mph",
            windDirection = "SW",
            icon = "https://api.weather.gov/icons/test",
            shortForecast = "Sunny",
            detailedForecast = "Sunny.",
        )
    }
}
