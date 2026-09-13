package com.diabad.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrendArrowGuideTest {

    @Test
    fun guideArrowsRunFromFastestRiseToFastestFall() {
        assertEquals(
            listOf(
                TrendArrow.DOUBLE_UP,
                TrendArrow.SINGLE_UP,
                TrendArrow.FORTY_FIVE_UP,
                TrendArrow.FLAT,
                TrendArrow.FORTY_FIVE_DOWN,
                TrendArrow.SINGLE_DOWN,
                TrendArrow.DOUBLE_DOWN,
            ),
            TrendArrow.GUIDE_ARROWS,
        )
    }

    @Test
    fun guideArrowsAllHaveAVisibleGlyph() {
        TrendArrow.GUIDE_ARROWS.forEach { arrow ->
            assertTrue(arrow.nightscoutName, arrow.glyph.isNotEmpty())
        }
    }

    @Test
    fun nightscoutDirectionMapsToTheSameArrowShownInTheGuide() {
        assertEquals(TrendArrow.FLAT, TrendArrow.fromNightscout("Flat"))
        assertEquals(TrendArrow.FORTY_FIVE_UP, TrendArrow.fromNightscout("FortyFiveUp"))
        assertEquals(TrendArrow.DOUBLE_DOWN, TrendArrow.fromNightscout("DoubleDown"))
    }
}
