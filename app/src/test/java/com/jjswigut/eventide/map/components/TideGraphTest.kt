package com.jjswigut.eventide.map.components

import org.junit.Assert.assertEquals
import org.junit.Test

class TideGraphTest {
    @Test
    fun `visible tide samples clamp progress and keep an initial trace`() {
        assertEquals(0, visibleTideSampleCount(totalSamples = 0, progress = 0f))
        assertEquals(1, visibleTideSampleCount(totalSamples = 1, progress = 0f))
        assertEquals(2, visibleTideSampleCount(totalSamples = 10, progress = -1f))
        assertEquals(6, visibleTideSampleCount(totalSamples = 10, progress = 0.5f))
        assertEquals(10, visibleTideSampleCount(totalSamples = 10, progress = 2f))
    }
}
