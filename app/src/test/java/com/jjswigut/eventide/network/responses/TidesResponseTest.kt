package com.jjswigut.eventide.network.responses

import org.junit.Assert.assertEquals
import org.junit.Test

class TidesResponseTest {
    @Test
    fun `toListOfTideDays skips malformed predictions`() {
        val tideDays = TidesResponse(
            predictions = listOf(
                TidesResponse.TideDTO(
                    t = "2026-07-07 01:14",
                    type = "H",
                    v = "1.2",
                ),
                TidesResponse.TideDTO(
                    t = "not a timestamp",
                    type = "L",
                    v = "0.1",
                ),
            ),
        ).toListOfTideDays()

        assertEquals(1, tideDays.size)
        assertEquals("Tuesday, Jul 7, 2026", tideDays.first().date)
        assertEquals(1, tideDays.first().tides.size)
        assertEquals("NOAA CO-OPS tide prediction", tideDays.first().tideSource)
    }
}
