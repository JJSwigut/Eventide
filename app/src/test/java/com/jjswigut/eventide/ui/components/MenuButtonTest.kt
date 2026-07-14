package com.jjswigut.eventide.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

class MenuButtonTest {
    @Test
    fun `expanded menu stays in the upper left quadrant`() {
        val angles = (0 until 5).map { menuButtonAngle(index = it, count = 5) }

        assertEquals(-PI / 2, angles.first(), 0.0001)
        assertEquals(-PI, angles.last(), 0.0001)
        assertTrue(angles.all { it in -PI..(-PI / 2) })
    }
}
