package com.diabad.core.glucose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GlucoseLockDisplayTest {

    @Test
    fun titleShowsValueArrowAndDelta() {
        assertEquals("6.2 → +0.3", GlucoseLockDisplay.title(6.2, "→", 0.3))
        assertEquals("3.8 ↓ −0.4", GlucoseLockDisplay.title(3.8, "↓", -0.4))
        assertEquals("5.4", GlucoseLockDisplay.title(5.4, "", null))
        assertEquals("5.4 → 0.0", GlucoseLockDisplay.title(5.4, "→", 0.02))
    }

    @Test
    fun chipStaysCompact() {
        assertEquals("6.2→+0.3", GlucoseLockDisplay.chip(6.2, "→", 0.3))
        assertEquals("12.4↑", GlucoseLockDisplay.chip(12.4, "↑", 1.2))
        assertEquals("5.4", GlucoseLockDisplay.chip(5.4, "", null))
    }

    @Test
    fun deltaCompactUsesMinusSign() {
        assertEquals("+0.3", GlucoseLockDisplay.deltaCompact(0.3))
        assertEquals("−0.4", GlucoseLockDisplay.deltaCompact(-0.4))
        assertEquals("0.0", GlucoseLockDisplay.deltaCompact(0.01))
        assertNull(GlucoseLockDisplay.deltaCompact(null))
    }
}
