package com.jjswigut.eventide.astronomy

import com.google.android.gms.maps.model.LatLng
import com.jjswigut.eventide.data.models.MoonPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class AstronomyCalculatorTest {
    @Test
    fun `calculates New York summer solstice sunrise and sunset`() {
        val data = AstronomyCalculator.calculate(
            date = LocalDate.of(2026, 6, 21),
            location = LatLng(40.7128, -74.0060),
        )

        assertNotNull(data.sunrise)
        assertNotNull(data.sunset)
        assertTrue(data.sunrise!!.isBetween(LocalTime.of(5, 15), LocalTime.of(5, 45)))
        assertTrue(data.sunset!!.isBetween(LocalTime.of(20, 20), LocalTime.of(20, 45)))
    }

    @Test
    fun `calculates moon phase near known new and full moons`() {
        assertEquals(MoonPhase.New, AstronomyCalculator.calculateMoonPhase(LocalDate.of(2000, 1, 6)))
        assertEquals(MoonPhase.Full, AstronomyCalculator.calculateMoonPhase(LocalDate.of(2000, 1, 21)))
        assertTrue(AstronomyCalculator.calculateMoonIlluminationPercent(LocalDate.of(2000, 1, 6)) < 5)
        assertTrue(AstronomyCalculator.calculateMoonIlluminationPercent(LocalDate.of(2000, 1, 21)) > 95)
    }

    private fun LocalTime.isBetween(
        start: LocalTime,
        end: LocalTime,
    ): Boolean = this >= start && this <= end
}
