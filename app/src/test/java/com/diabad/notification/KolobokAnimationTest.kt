package com.diabad.notification

import com.diabad.domain.model.GlucoseZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KolobokAnimationTest {

    @Test
    fun hypoAndAlarmTickFasterThanInRange() {
        val inRange = kolobokAnimationIntervalMs(GlucoseZone.IN_RANGE, alarming = false)
        val veryLow = kolobokAnimationIntervalMs(GlucoseZone.VERY_LOW, alarming = false)
        val alarming = kolobokAnimationIntervalMs(GlucoseZone.IN_RANGE, alarming = true)
        assertEquals(700L, inRange)
        assertEquals(280L, veryLow)
        assertEquals(280L, alarming)
        assertTrue(veryLow < inRange)
    }

    @Test
    fun highBandsAreBetweenHypoAndIdle() {
        val high = kolobokAnimationIntervalMs(GlucoseZone.HIGH, alarming = false)
        val veryHigh = kolobokAnimationIntervalMs(GlucoseZone.VERY_HIGH, alarming = false)
        val low = kolobokAnimationIntervalMs(GlucoseZone.LOW, alarming = false)
        assertEquals(420L, high)
        assertEquals(350L, veryHigh)
        assertEquals(360L, low)
    }
}
