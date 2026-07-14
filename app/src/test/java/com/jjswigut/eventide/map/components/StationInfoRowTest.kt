package com.jjswigut.eventide.map.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class StationInfoRowTest {
    @Test
    fun `marine freshness uses concise relative labels and marks stale data`() {
        val now = Instant.parse("2026-07-07T18:50:00Z")

        assertEquals("Updated now", now.marineFreshnessLabel(now, Duration.ofHours(3)))
        assertEquals(
            "Updated 6h ago - Stale",
            Instant.parse("2026-07-07T12:00:00Z").marineFreshnessLabel(now, Duration.ofHours(3)),
        )
        assertTrue(
            Instant.parse("2026-07-07T18:20:00Z")
                .marineFreshnessLabel(now, Duration.ofHours(3))
                ?.contains("30m") == true,
        )
    }
}
