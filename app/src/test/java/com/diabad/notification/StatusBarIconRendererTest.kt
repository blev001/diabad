package com.diabad.notification

import com.diabad.domain.model.GlucoseReading
import com.diabad.domain.model.TrendArrow
import org.junit.Assert.assertEquals
import org.junit.Test

class StatusBarIconRendererTest {

    @Test
    fun shadeBadgeKeepsOneDecimalAboveTen() {
        assertEquals("5.4", StatusBarIconRenderer.statusValue(5.4))
        assertEquals("9.9", StatusBarIconRenderer.statusValue(9.9))
        assertEquals("10.0", StatusBarIconRenderer.statusValue(10.0))
        assertEquals("10.4", StatusBarIconRenderer.statusValue(10.4))
        assertEquals("12.4", StatusBarIconRenderer.statusValue(12.37))
        assertEquals("15.8", StatusBarIconRenderer.statusValue(15.8))
    }

    @Test
    fun liveUpdateChipKeepsOneDecimalAboveTen() {
        val now = 1_700_000_000_000L
        val reading = GlucoseReading(
            mmol = 12.4,
            timestampMillis = now,
            trend = TrendArrow.FLAT,
        )
        assertEquals("12.4→", StatusBarIconRenderer.chipText(reading, now))
    }
}
