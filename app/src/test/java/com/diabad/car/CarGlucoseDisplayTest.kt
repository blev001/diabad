package com.diabad.car

import com.diabad.domain.model.AppSettings
import com.diabad.domain.model.GlucoseReading
import com.diabad.domain.model.TrendArrow
import org.junit.Assert.*
import org.junit.Test

class CarGlucoseDisplayTest {
    private val now = 1_000_000L
    private val reading = GlucoseReading(5.5, now, TrendArrow.SINGLE_DOWN)

    @Test fun missingDataDoesNotShowGlucose() {
        assertEquals("Нет данных", CarGlucoseDisplay.from(null, AppSettings(), now).value)
    }

    @Test fun staleBoundaryHidesArrowAndOverridesLowStatus() {
        val result = CarGlucoseDisplay.from(reading.copy(mmol = 3.0), AppSettings(), now + 600_000)
        assertFalse(result.value.contains("↓"))
        assertEquals("Нет свежих данных — последнее измерение", result.status)
        assertEquals(10L, result.ageMinutes)
    }

    @Test fun freshDataShowsTrendAndUsesCustomThresholds() {
        val result = CarGlucoseDisplay.from(reading, AppSettings(hypoThresholdMmol = 6.0), now)
        assertTrue(result.value.contains("↓"))
        assertEquals("Низкий сахар", result.status)
    }

    @Test fun gracePeriodFollowsSettings() {
        val result = CarGlucoseDisplay.from(reading, AppSettings(connectionLossGraceMinutes = 5), now + 300_000)
        assertTrue(result.status.startsWith("Нет свежих данных"))
    }

    @Test fun futureTimestampHasNonnegativeAge() {
        assertEquals(0L, CarGlucoseDisplay.from(reading, AppSettings(), now - 60_000).ageMinutes)
    }
}
