package com.diabad.core.wear

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WearAlarmPathsTest {

    @Test
    fun roundTripHypo() {
        val encoded = WearAlarmPaths.encodeRing(3.2, 3.9, 10, WearAlarmPaths.KIND_HYPO)
        val decoded = WearAlarmPaths.decodeRing(encoded)!!
        assertEquals(3.2, decoded.mmol, 0.001)
        assertEquals(3.9, decoded.thresholdMmol, 0.001)
        assertEquals(10, decoded.snoozeMinutes)
        assertEquals(WearAlarmPaths.KIND_HYPO, decoded.kind)
        assertFalse(decoded.test)
    }

    @Test
    fun roundTripHyperTest() {
        val encoded = WearAlarmPaths.encodeRing(
            mmol = 14.2,
            thresholdMmol = 10.0,
            snoozeMinutes = 15,
            kind = WearAlarmPaths.KIND_HYPER,
            test = true,
        )
        val decoded = WearAlarmPaths.decodeRing(encoded)!!
        assertEquals(WearAlarmPaths.KIND_HYPER, decoded.kind)
        assertTrue(decoded.test)
        assertEquals(15, decoded.snoozeMinutes)
    }
}
