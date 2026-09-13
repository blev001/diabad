package com.diabad.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusBarIconMetricsTest {

    @Test
    fun numberUsesMostOfTheCanvasWhenThereIsNoArrow() {
        val metrics = computeStatusBarIconMetrics(sizePx = 96, value = "5.4", hasArrow = false)
        assertFalse(metrics.hasArrow)
        assertEquals(48f, metrics.valueCenterX, 0.1f)
        assertTrue(metrics.valueTextSize > 96 * 0.45f)
    }

    @Test
    fun arrowGetsAWideLaneBesideALargeNumber() {
        val metrics = computeStatusBarIconMetrics(sizePx = 96, value = "5.4", hasArrow = true)
        assertTrue(metrics.hasArrow)
        assertTrue(metrics.valueTextSize > 96 * 0.34f)
        assertTrue(metrics.arrowWidth > 96 * 0.24f)
        assertTrue(metrics.arrowHeight > 96 * 0.70f)
        assertTrue(metrics.arrowLeft > metrics.valueCenterX)
        assertTrue(metrics.arrowLeft + metrics.arrowWidth <= 96f)
    }

    @Test
    fun longerValuesShrinkToStayOnTheBadge() {
        val shortValue = computeStatusBarIconMetrics(96, "5.4", hasArrow = true)
        val longValue = computeStatusBarIconMetrics(96, "12.4", hasArrow = true)
        assertTrue(longValue.valueTextSize < shortValue.valueTextSize)
        assertTrue(longValue.valueTextSize > 22f)
    }

    @Test
    fun fitTextSizeKeepsFourDigitsInsideTheLane() {
        val size = fitTextSize("12.4", maxWidth = 50f, maxHeight = 80f)
        assertTrue(size <= 50f / (4 * 0.50f) + 0.01f)
        assertTrue(size > 18f)
    }
}
