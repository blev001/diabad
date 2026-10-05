package com.diabad.alarm

import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmUiCopyTest {

    @Test
    fun formatRateKeepsTwoDecimalsAndMinusSign() {
        assertEquals("−0.05", formatRate(-0.053))
        assertEquals("+0.20", formatRate(0.2))
        assertEquals("−0.00", formatRate(null))
    }
}
