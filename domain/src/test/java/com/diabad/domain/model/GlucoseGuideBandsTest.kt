package com.diabad.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GlucoseGuideBandsTest {

    @Test
    fun defaultAdaThresholdsMatchFiveClinicalBands() {
        val bands = glucoseGuideBands(
            hypoThresholdMmol = AppSettings.DEFAULT_HYPO_THRESHOLD_MMOL,
            hyperThresholdMmol = AppSettings.DEFAULT_HYPER_THRESHOLD_MMOL,
        )
        assertEquals(5, bands.size)
        assertBand(bands[0], GlucoseZone.VERY_LOW, GlucoseGuideBand.Kind.BELOW, 3.0, null)
        assertBand(bands[1], GlucoseZone.LOW, GlucoseGuideBand.Kind.BETWEEN, 3.0, 3.9)
        assertBand(bands[2], GlucoseZone.IN_RANGE, GlucoseGuideBand.Kind.BETWEEN, 3.9, 10.0)
        assertBand(bands[3], GlucoseZone.HIGH, GlucoseGuideBand.Kind.BETWEEN, 10.0, 13.9)
        assertBand(bands[4], GlucoseZone.VERY_HIGH, GlucoseGuideBand.Kind.ABOVE, 13.9, null)
    }

    @Test
    fun customAlarmEdgesMoveTheMiddleThreeBands() {
        val bands = glucoseGuideBands(hypoThresholdMmol = 4.2, hyperThresholdMmol = 11.5)
        assertBand(bands[1], GlucoseZone.LOW, GlucoseGuideBand.Kind.BETWEEN, 3.0, 4.2)
        assertBand(bands[2], GlucoseZone.IN_RANGE, GlucoseGuideBand.Kind.BETWEEN, 4.2, 11.5)
        assertBand(bands[3], GlucoseZone.HIGH, GlucoseGuideBand.Kind.BETWEEN, 11.5, 13.9)
    }

    @Test
    fun swappedThresholdsAreSortedBeforeBuildingRanges() {
        val bands = glucoseGuideBands(hypoThresholdMmol = 12.0, hyperThresholdMmol = 3.5)
        assertBand(bands[1], GlucoseZone.LOW, GlucoseGuideBand.Kind.BETWEEN, 3.0, 3.5)
        assertBand(bands[2], GlucoseZone.IN_RANGE, GlucoseGuideBand.Kind.BETWEEN, 3.5, 12.0)
        assertBand(bands[3], GlucoseZone.HIGH, GlucoseGuideBand.Kind.BETWEEN, 12.0, 13.9)
    }

    @Test
    fun classifyUsesTheSameEdgesAsTheGuide() {
        assertEquals(
            GlucoseZone.VERY_LOW,
            GlucoseZone.classify(2.9, 3.9, 10.0),
        )
        assertEquals(
            GlucoseZone.LOW,
            GlucoseZone.classify(3.4, 3.9, 10.0),
        )
        assertEquals(
            GlucoseZone.IN_RANGE,
            GlucoseZone.classify(6.1, 3.9, 10.0),
        )
        assertEquals(
            GlucoseZone.HIGH,
            GlucoseZone.classify(11.2, 3.9, 10.0),
        )
        assertEquals(
            GlucoseZone.VERY_HIGH,
            GlucoseZone.classify(14.0, 3.9, 10.0),
        )
    }

    private fun assertBand(
        band: GlucoseGuideBand,
        zone: GlucoseZone,
        kind: GlucoseGuideBand.Kind,
        first: Double,
        second: Double?,
    ) {
        assertEquals(zone, band.zone)
        assertEquals(kind, band.kind)
        assertEquals(first, band.firstMmol, 0.0001)
        if (second == null) {
            assertNull(band.secondMmol)
        } else {
            assertEquals(second, band.secondMmol!!, 0.0001)
        }
    }
}
