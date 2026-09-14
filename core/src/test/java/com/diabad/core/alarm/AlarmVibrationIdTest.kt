package com.diabad.core.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmVibrationIdTest {

    @Test
    fun offHasNoPulse() {
        assertEquals(1, AlarmVibrationId.OFF.waveform().size)
    }

    @Test
    fun presetsHaveOnOffPairs() {
        AlarmVibrationId.entries
            .filter { it != AlarmVibrationId.OFF }
            .forEach { id ->
                assertTrue(id.name, id.waveform().size >= 3)
            }
    }

    @Test
    fun fromNameFallsBackToClock() {
        assertEquals(AlarmVibrationId.SOS, AlarmVibrationId.fromName("SOS"))
        assertEquals(AlarmVibrationId.CLOCK, AlarmVibrationId.fromName(null))
        assertEquals(AlarmVibrationId.CLOCK, AlarmVibrationId.fromName("nope"))
    }
}
