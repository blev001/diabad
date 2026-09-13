package com.diabad.core.glucose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GlucoseUnitsTest {

    @Test
    fun formatDeltaNeedsAPreviousSample() {
        assertNull(formatDeltaMmol(5.4, null))
        assertNull(glucoseDeltaMmol(5.4, null))
    }

    @Test
    fun tinyChangesAreFlat() {
        assertEquals("Δ 0.0", formatDeltaMmol(5.40, 5.38))
        assertEquals("Δ 0.0", formatDeltaMmol(5.40, 5.44))
    }

    @Test
    fun risingAndFallingKeepASign() {
        assertEquals("Δ +0.3", formatDeltaMmol(5.7, 5.4))
        assertEquals("Δ −0.4", formatDeltaMmol(5.0, 5.4))
        assertEquals(0.3, glucoseDeltaMmol(5.7, 5.4)!!, 0.001)
        assertEquals(-0.4, glucoseDeltaMmol(5.0, 5.4)!!, 0.001)
    }
}
