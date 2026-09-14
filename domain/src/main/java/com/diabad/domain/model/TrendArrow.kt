package com.diabad.domain.model

/**
 * Nightscout / AAPS `direction` values mapped to UI trend arrows.
 *
 * OtTai Share with AAPS sends the same strings. DiaBAD does not compute the
 * arrow locally: it displays the CGM rate-of-change class.
 *
 * Dexcom / Nightscout thresholds (mg/dL/min), converted ≈ ÷ 18.0 to mmol/L/min:
 * - DoubleUp:     ≥ +3   (≥ +0.17 ммоль/л·мин)
 * - SingleUp:   +2…+3    (+0.11…0.17)
 * - FortyFiveUp:+1…+2    (+0.06…0.11)
 * - Flat:       −1…+1    (|скорость| < 0.06) — горизонтальная «лежачая» стрелка
 * - FortyFiveDown: −2…−1
 * - SingleDown:    −3…−2
 * - DoubleDown:    ≤ −3
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
        /** Clinical guide order: fastest rise → fastest fall. */
        val GUIDE_ARROWS: List<TrendArrow> = listOf(
            DOUBLE_UP,
            SINGLE_UP,
            FORTY_FIVE_UP,
            FLAT,
            FORTY_FIVE_DOWN,
            SINGLE_DOWN,
            DOUBLE_DOWN,
        )

        fun fromNightscout(direction: String?): TrendArrow {
            if (direction.isNullOrBlank()) return NONE
            return entries.firstOrNull {
                it.nightscoutName.equals(direction, ignoreCase = true)
            } ?: NONE
        }
    }
}
