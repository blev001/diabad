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

    @Test
    fun amplitudesAreMaxOnOddSlots() {
        val amps = AlarmVibrationId.STRONG.amplitudes()
        val timings = AlarmVibrationId.STRONG.waveform()
        assertEquals(timings.size, amps.size)
        amps.forEachIndexed { index, amp ->
            val expected = if (index % 2 == 1) AlarmVibrationId.MAX_AMPLITUDE else 0
            assertEquals("slot $index", expected, amp)
        }
        assertTrue(amps.any { it == AlarmVibrationId.MAX_AMPLITUDE })
    }
}
