package com.diabad.domain.analysis

import com.diabad.domain.model.GlucoseReading
import com.diabad.domain.model.TrendArrow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GlucoseAnalysisTest {

    private val t0 = 1_700_000_000_000L

    private fun r(minute: Int, mmol: Double) =
        GlucoseReading(mmol, t0 + minute * 60_000L, TrendArrow.NONE)

    @Test
    fun `trend needs enough span`() {
        assertNull(GlucoseTrend.from(listOf(r(0, 5.0), r(1, 5.1), r(2, 5.2))))
        assertNotNull(GlucoseTrend.from(listOf(r(0, 5.0), r(5, 5.5), r(10, 6.0))))
    }

    @Test
    fun `trend slope and prediction`() {
        val trend = GlucoseTrend.from(listOf(r(0, 8.0), r(5, 7.0), r(10, 6.0)))!!
        assertEquals(-0.2, trend.ratePerMinute, 1e-9)
        assertEquals(4.0, trend.predictMmol(10), 1e-9)
    }

    @Test
    fun `trend ignores readings older than window`() {
        val trend = GlucoseTrend.from(listOf(r(-60, 20.0), r(0, 6.0), r(5, 6.0), r(10, 6.0)))!!
        assertEquals(0.0, trend.ratePerMinute, 1e-9)
    }

    @Test
    fun `stats time in range buckets`() {
        val values = listOf(2.5, 3.5, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 12.0, 15.0)
        val readings = values.mapIndexed { i, v -> r(i * 5, v) }
        val s = GlucoseStats.compute(readings, periodMillis = 50 * 60_000L)!!
        assertEquals(10.0, s.veryLowPercent, 1e-9)
        assertEquals(10.0, s.lowPercent, 1e-9)
        assertEquals(60.0, s.inRangePercent, 1e-9)
        assertEquals(10.0, s.highPercent, 1e-9)
        assertEquals(10.0, s.veryHighPercent, 1e-9)
        assertEquals(100.0, s.coveragePercent, 1e-9)
        assertEquals(7.8, s.meanMmol, 1e-9)
    }

    @Test
    fun `gmi formula`() {
        // 8.6 mmol/L ≈ 155 mg/dL → 7.0 % (consensus table: 150 mg/dL → 6.9 %)
        assertEquals(7.02, GlucoseStats.gmiFromMeanMmol(8.6), 0.01)
        assertEquals(6.9, GlucoseStats.gmiFromMeanMmol(150 / 18.01559), 0.01)
    }

    @Test
    fun `coverage reflects gaps`() {
        val readings = (0 until 12).map { r(it * 5, 6.0) }
        val s = GlucoseStats.compute(readings, periodMillis = 120 * 60_000L)!!
        assertEquals(50.0, s.coveragePercent, 1e-9)
    }

    @Test
    fun `percentile interpolates`() {
        val sorted = listOf(1.0, 2.0, 3.0, 4.0, 5.0)
        assertEquals(3.0, AgpProfile.percentile(sorted, 0.5), 1e-9)
        assertEquals(1.2, AgpProfile.percentile(sorted, 0.05), 1e-9)
    }

    @Test
    fun `agp groups by hour`() {
        val readings = (0 until 120).map { r(it, if (it < 60) 5.0 else 9.0) }
        val buckets = AgpProfile.compute(readings) { ts -> ((ts - t0) / 3_600_000L).toInt() }
        assertEquals(2, buckets.size)
        assertEquals(5.0, buckets[0].p50, 1e-9)
        assertEquals(9.0, buckets[1].p50, 1e-9)
    }
}
