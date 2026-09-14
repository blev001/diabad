package com.diabad.widget

import com.diabad.domain.model.GlucoseZone
import org.junit.Assert.assertEquals
import org.junit.Test

class GlucoseLockPaletteTest {

    @Test
    fun waitingUsesNeutralColor() {
        assertEquals(
            GlucoseLockPalette.NEUTRAL,
            GlucoseLockPalette.contentColor(GlucoseZone.LOW, waiting = true),
        )
    }

    @Test
    fun zoneColorsMatchLockScreenChips() {
        assertEquals(
            GlucoseLockPalette.LOW,
            GlucoseLockPalette.contentColor(GlucoseZone.VERY_LOW, waiting = false),
        )
        assertEquals(
            GlucoseLockPalette.LOW,
            GlucoseLockPalette.contentColor(GlucoseZone.LOW, waiting = false),
        )
        assertEquals(
            GlucoseLockPalette.HIGH,
            GlucoseLockPalette.contentColor(GlucoseZone.HIGH, waiting = false),
        )
        assertEquals(
            GlucoseLockPalette.HIGH,
            GlucoseLockPalette.contentColor(GlucoseZone.VERY_HIGH, waiting = false),
        )
        assertEquals(
            GlucoseLockPalette.IN_RANGE,
            GlucoseLockPalette.contentColor(GlucoseZone.IN_RANGE, waiting = false),
        )
        assertEquals(
            GlucoseLockPalette.NEUTRAL,
            GlucoseLockPalette.contentColor(GlucoseZone.UNKNOWN, waiting = false),
        )
    }
}

class FaceWidgetCatalogTest {

    @Test
    fun pageIdMatchesFaceWidgetJsonKey() {
        assertEquals("glucose", FaceWidgetCatalog.PAGE_ID)
        assertEquals("app_name", FaceWidgetCatalog.LABEL_RES_NAME)
    }
}
