package com.diabad.core.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmSnoozeSlotsTest {

    @Test
    fun slotsAreFifteenThirtySixty() {
        assertEquals(listOf(15, 30, 60), AlarmSnoozeSlots.MINUTES)
        assertEquals(15, AlarmSnoozeSlots.DEFAULT_MINUTES)
    }

    @Test
    fun normalizeMapsLegacyFiveAndTenToFifteen() {
        assertEquals(15, AlarmSnoozeSlots.normalize(5))
        assertEquals(15, AlarmSnoozeSlots.normalize(10))
        assertEquals(15, AlarmSnoozeSlots.normalize(15))
        assertEquals(30, AlarmSnoozeSlots.normalize(30))
        assertEquals(60, AlarmSnoozeSlots.normalize(60))
        assertEquals(60, AlarmSnoozeSlots.normalize(90))
    }

    @Test
    fun remainingMinutesRoundsUpAndStaysSilentWhileLow() {
        val now = 1_000_000L
        assertEquals(0, AlarmSnoozeSlots.remainingMinutes(now, now))
        assertEquals(1, AlarmSnoozeSlots.remainingMinutes(now + 1_000L, now))
        assertEquals(15, AlarmSnoozeSlots.remainingMinutes(now + 15 * 60_000L, now))
        assertTrue(AlarmSnoozeSlots.isActive(now + 15 * 60_000L, now))
        assertFalse(
            AlarmSnoozeSlots.shouldRing(
                outOfRange = true,
                dismissedUntilRecovery = false,
                snoozedUntilMillis = now + 15 * 60_000L,
                nowMillis = now,
            ),
        )
        assertTrue(
            AlarmSnoozeSlots.shouldRing(
                outOfRange = true,
                dismissedUntilRecovery = false,
                snoozedUntilMillis = 0L,
                nowMillis = now,
            ),
        )
    }
}
