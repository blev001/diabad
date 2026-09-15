package com.diabad.widget

import com.diabad.domain.model.GlucoseZone

internal object GlucoseLockPalette {
    const val LOW = 0xFFFF453A.toInt()
    const val HIGH = 0xFFFF9F0A.toInt()
    const val IN_RANGE = 0xFF34C759.toInt()
    const val NEUTRAL = 0xFFFFFFFF.toInt()

    fun contentColor(zone: GlucoseZone, waiting: Boolean): Int {
        if (waiting) return NEUTRAL
        return when (zone) {
            GlucoseZone.VERY_LOW, GlucoseZone.LOW -> LOW
            GlucoseZone.HIGH, GlucoseZone.VERY_HIGH -> HIGH
            GlucoseZone.IN_RANGE -> IN_RANGE
            GlucoseZone.UNKNOWN -> NEUTRAL
        }
    }
}
