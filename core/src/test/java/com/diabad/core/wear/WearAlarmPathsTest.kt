package com.diabad.core.wear

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WearAlarmPathsTest {

    @Test
    fun decodeKeepsVibrationAndTestAsSeparateFields() {
        val bytes = WearAlarmPaths.encodeRing(
            mmol = 3.2,
            thresholdMmol = 3.9,
            snoozeMinutes = 10,
            kind = WearAlarmPaths.KIND_HYPO,
            vibration = "SOS",
            test = true,
        )
        val payload = WearAlarmPaths.decodeRing(bytes)!!
        assertEquals(3.2, payload.mmol, 0.0001)
        assertEquals(3.9, payload.thresholdMmol, 0.0001)
        assertEquals(10, payload.snoozeMinutes)
        assertEquals(WearAlarmPaths.KIND_HYPO, payload.kind)
        assertEquals("SOS", payload.vibration)
        assertTrue(payload.test)
    }

    @Test
    fun decodeDoesNotTreatTestFlagAsVibrationId() {
        val bytes = "3.2|3.9|10|HYPO|TEST".toByteArray(Charsets.UTF_8)
        val payload = WearAlarmPaths.decodeRing(bytes)!!
        assertEquals("CLOCK", payload.vibration)
        assertTrue(payload.test)
    }

    @Test
    fun liveAlarmOmitsTestFlag() {
        val bytes = WearAlarmPaths.encodeRing(
            mmol = 12.4,
            thresholdMmol = 10.0,
            snoozeMinutes = 15,
            kind = WearAlarmPaths.KIND_HYPER,
            vibration = "PULSE",
            test = false,
        )
        val payload = WearAlarmPaths.decodeRing(bytes)!!
        assertEquals(WearAlarmPaths.KIND_HYPER, payload.kind)
        assertEquals("PULSE", payload.vibration)
        assertFalse(payload.test)
    }

    @Test
    fun snoozePayloadRoundTripsChosenSlot() {
        val bytes = WearAlarmPaths.encodeSnooze(30)
        assertEquals(30, WearAlarmPaths.decodeSnooze(bytes))
        assertEquals(15, WearAlarmPaths.decodeSnooze(ByteArray(0)))
        assertEquals(60, WearAlarmPaths.decodeSnooze("60".toByteArray(Charsets.UTF_8)))
    }
}
