package com.diabad.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlucoseAlertRulesTest {

    private val settings = AppSettings()

    @Test
    fun approachingHypo_defaultBandIs43DownToAlarmThreshold() {
        assertFalse(settings.isApproachingHypo(4.4))
        assertTrue(settings.isApproachingHypo(4.3))
        assertTrue(settings.isApproachingHypo(4.0))
        assertTrue(settings.isApproachingHypo(3.9))
        assertFalse(settings.isApproachingHypo(3.8))
    }

    @Test
    fun approachingHypo_disabledNeverWarns() {
        val off = settings.copy(approachingHypoEnabled = false)
        assertFalse(off.isApproachingHypo(4.2))
        assertFalse(off.isApproachingHypo(3.9))
    }

    @Test
    fun approachingHypo_emptyIfWarnAtOrBelowAlarm() {
        val collapsed = settings.copy(approachingHypoThresholdMmol = 3.9)
        assertFalse(collapsed.isApproachingHypo(3.9))
        assertFalse(collapsed.isApproachingHypo(4.0))
    }

    @Test
    fun hypoAlarm_stillFiresBelowThreshold() {
        assertEquals(GlucoseAlarmKind.HYPO, settings.alarmKindFor(3.8))
        assertEquals(null, settings.alarmKindFor(3.9))
        assertTrue(settings.isOutOfAlarmRange(3.8))
        assertFalse(settings.isOutOfAlarmRange(4.2))
    }

    @Test
    fun hyperAlarm_unchanged() {
        assertEquals(GlucoseAlarmKind.HYPER, settings.alarmKindFor(10.1))
        assertFalse(settings.isApproachingHypo(10.1))
    }
}
