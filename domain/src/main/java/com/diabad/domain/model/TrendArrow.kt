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

    companion object {
        fun fromNightscout(direction: String?): TrendArrow {
            if (direction.isNullOrBlank()) return NONE
            return entries.firstOrNull {
                it.nightscoutName.equals(direction, ignoreCase = true)
            } ?: NONE
        }
    }
}
