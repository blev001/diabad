package com.diabad.data.ottai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OttaiBroadcastParserTest {

    private val parser = OttaiBroadcastParser()

    @Test
    fun parsesSgvAndConvertsToMmol() {
        val json = """
            [
              {"type":"sgv","date":1710000000000,"sgv":70.0,"direction":"FortyFiveDown"},
              {"type":"other","date":1}
            ]
        """.trimIndent()

        val readings = parser.parseEntriesJson(json)

        assertEquals(1, readings.size)
        assertEquals(1710000000000L, readings[0].timestampMillis)
        // 70 mg/dL ≈ 3.9 mmol/L
        assertEquals(3.9, readings[0].mmol, 0.05)
        assertEquals("FORTY_FIVE_DOWN", readings[0].trend.name)
    }

    @Test
    fun emptyArrayYieldsEmptyList() {
        assertTrue(parser.parseEntriesJson("[]").isEmpty())
    }
}
