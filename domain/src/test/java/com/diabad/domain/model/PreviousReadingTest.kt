package com.diabad.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PreviousReadingTest {

    @Test
    fun needsAPreviousSample() {
        assertNull(previousReading(null, emptyList()))
        val only = reading(5.4, 2_000L)
        assertNull(previousReading(only, listOf(only)))
        assertNull(previousReading(only, emptyList()))
    }

    @Test
    fun picksTheSampleJustBeforeLatest() {
        val older = reading(5.1, 1_000L)
        val previous = reading(5.4, 2_000L)
        val latest = reading(5.7, 3_000L)
        assertEquals(previous, previousReading(latest, listOf(older, previous, latest)))
    }

    @Test
    fun fallsBackToSecondNewestWhenLatestIsMissingFromHistory() {
        val older = reading(5.1, 1_000L)
        val previous = reading(5.4, 2_000L)
        val latest = reading(5.7, 3_000L)
        assertEquals(previous, previousReading(latest, listOf(older, previous)))
    }

    private fun reading(mmol: Double, time: Long) = GlucoseReading(
        mmol = mmol,
        timestampMillis = time,
        trend = TrendArrow.FLAT,
    )
}
