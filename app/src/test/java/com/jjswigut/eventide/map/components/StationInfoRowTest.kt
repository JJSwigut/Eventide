package com.jjswigut.eventide.map.components

import com.jjswigut.eventide.data.models.BuoyConditions
import com.jjswigut.eventide.data.models.MarineConditions
import com.jjswigut.eventide.data.models.MarineObservation
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class StationInfoRowTest {
    @Test
    fun `toMarineChips includes source freshness and buoy distance with readings`() {
        val chips = MarineConditions(
            observations = listOf(
                MarineObservation(
                    label = "Water",
                    value = "64.1°F",
                    observedAt = Instant.parse("2026-07-07T18:50:00Z"),
                ),
            ),
            buoy = BuoyConditions(
                stationId = "44060",
                stationName = "Eastern Long Island Sound",
                distanceMiles = 12.4,
                waveHeight = "3.9ft",
                observedAt = Instant.parse("2026-07-07T12:00:00Z"),
            ),
        ).toMarineChips(now = Instant.parse("2026-07-07T18:50:00Z"))

        assertTrue(chips.any { it.contains("NOAA CO-OPS observation") && it.contains("current") })
        assertTrue(chips.any { it.contains("Buoy 44060 (12mi): waves 3.9ft") })
        assertTrue(chips.any { it.contains("NDBC buoy observation") && it.contains("stale") })
    }
}
