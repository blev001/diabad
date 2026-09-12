package com.diabad.domain.model

/**
 * Nightscout / AAPS direction strings mapped to UI trend arrows.
 */
enum class TrendArrow(val nightscoutName: String) {
    NONE("NONE"),
    DOUBLE_UP("DoubleUp"),
    SINGLE_UP("SingleUp"),
    FORTY_FIVE_UP("FortyFiveUp"),
    FLAT("Flat"),
    FORTY_FIVE_DOWN("FortyFiveDown"),
    SINGLE_DOWN("SingleDown"),
    DOUBLE_DOWN("DoubleDown"),
    NOT_COMPUTABLE("NOT COMPUTABLE"),
    RATE_OUT_OF_RANGE("RATE OUT OF RANGE");

    /** Compact glyph for status bar / notification title. */
    val glyph: String
        get() = when (this) {
            DOUBLE_UP -> "⇈"
            SINGLE_UP -> "↑"
            FORTY_FIVE_UP -> "↗"
            FLAT -> "→"
            FORTY_FIVE_DOWN -> "↘"
            SINGLE_DOWN -> "↓"
            DOUBLE_DOWN -> "⇊"
            NONE, NOT_COMPUTABLE, RATE_OUT_OF_RANGE -> ""
        }

    companion object {
        fun fromNightscout(direction: String?): TrendArrow {
            if (direction.isNullOrBlank()) return NONE
            return entries.firstOrNull {
                it.nightscoutName.equals(direction, ignoreCase = true)
            } ?: NONE
        }
    }
}
