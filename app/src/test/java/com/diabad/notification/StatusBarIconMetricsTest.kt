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
        assertTrue(metrics.valueTextSize > 96 * 0.50f)
    }

    @Test
    fun arrowSitsUnderAFullWidthNumber() {
        val metrics = computeStatusBarIconMetrics(sizePx = 96, value = "5.4", hasArrow = true)
        assertTrue(metrics.hasArrow)
        assertEquals(48f, metrics.valueCenterX, 0.1f)
        assertTrue(metrics.valueTextSize > 96 * 0.42f)
        assertTrue(metrics.arrowWidth > 96 * 0.80f)
        assertTrue(metrics.arrowHeight in 96 * 0.20f..96 * 0.38f)
        assertTrue(metrics.arrowTop > metrics.valueBaselineY - metrics.valueTextSize)
        assertTrue(metrics.arrowLeft + metrics.arrowWidth <= 96f)
        assertTrue(metrics.arrowTop + metrics.arrowHeight <= 96f)
    }

    @Test
    fun longerValuesShrinkToStayOnTheBadge() {
        val shortValue = computeStatusBarIconMetrics(96, "5.4", hasArrow = true)
        val longValue = computeStatusBarIconMetrics(96, "12.4", hasArrow = true)
        assertTrue(longValue.valueTextSize < shortValue.valueTextSize)
        assertTrue(longValue.valueTextSize > 28f)
    }

    @Test
    fun fitTextSizeKeepsFourDigitsInsideTheLane() {
        val size = fitTextSize("12.4", maxWidth = 80f, maxHeight = 50f)
        assertTrue(size <= 80f / (4 * 0.48f) + 0.01f)
        assertTrue(size <= 50f)
    }
}
