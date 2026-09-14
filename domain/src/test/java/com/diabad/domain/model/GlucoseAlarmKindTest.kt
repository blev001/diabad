package com.diabad.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GlucoseAlarmKindTest {

    private val settings = AppSettings(
        hypoThresholdMmol = 3.9,
        hyperThresholdMmol = 10.0,
    )

    @Test
    fun hypoBelowThreshold() {
        assertEquals(GlucoseAlarmKind.HYPO, settings.alarmKindFor(3.8))
        assertTrue(settings.isOutOfAlarmRange(3.1))
    }

    @Test
    fun hyperAboveThreshold() {
        assertEquals(GlucoseAlarmKind.HYPER, settings.alarmKindFor(10.1))
        assertTrue(settings.isOutOfAlarmRange(13.9))
    }

    @Test
    fun inRangeDoesNotAlarm() {
        assertNull(settings.alarmKindFor(3.9))
        assertNull(settings.alarmKindFor(10.0))
        assertNull(settings.alarmKindFor(6.2))
        assertFalse(settings.isOutOfAlarmRange(5.4))
    }
}
