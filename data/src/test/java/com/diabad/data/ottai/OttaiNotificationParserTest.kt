package com.diabad.data.ottai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class OttaiNotificationParserTest {

    private val parser = OttaiNotificationParser()

    @Test
    fun parsesMmolFromTitle() {
        val reading = parser.parseNotificationText(
            title = "5.4 → mmol/L",
            text = null,
            bigText = null,
            nowMillis = 1000L,
        )
        assertNotNull(reading)
        assertEquals(5.4, reading!!.mmol, 0.01)
        assertEquals(1000L, reading.timestampMillis)
    }

    @Test
    fun parsesMgdlIntegerAsMmol() {
        val reading = parser.parseNotificationText("97", null, null)
        assertNotNull(reading)
        // 97 / 18.01559 ≈ 5.38
        assertEquals(5.38, reading!!.mmol, 0.05)
    }

    @Test
    fun rejectsGarbage() {
        assertNull(parser.parseNotificationText("OtTai running", "Sensor OK", null))
    }
}
