package com.diabad.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GlucoseHistoryTest {

    @Test
    fun previousOfReturnsSampleBeforeLatest() {
        val first = GlucoseReading(5.0, 1_000L, TrendArrow.FLAT)
        val second = GlucoseReading(5.4, 2_000L, TrendArrow.FORTY_FIVE_UP)
        val history = listOf(first, second)
        assertEquals(first, history.previousOf(second))
        assertNull(history.previousOf(first))
        assertNull(emptyList<GlucoseReading>().previousOf(second))
        assertNull(listOf(second).previousOf(second))
        val other = GlucoseReading(6.0, 9_000L, TrendArrow.SINGLE_UP)
        assertEquals(first, history.previousOf(other))
    }
}
