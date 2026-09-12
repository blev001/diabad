package com.diabad.data.ottai

import com.diabad.core.glucose.mgdlToMmol
import com.diabad.domain.model.GlucoseReading
import com.diabad.domain.model.GlucoseSource
import com.diabad.domain.model.TrendArrow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extracts glucose from OtTai notification title/text (xDrip Companion approach).
 * DiaBAD stores mmol/L.
 */
@Singleton
class OttaiNotificationParser @Inject constructor() {

    fun parseNotificationText(
        title: String?,
        text: String?,
        bigText: String?,
        nowMillis: Long = System.currentTimeMillis(),
    ): GlucoseReading? {
        val candidates = listOfNotNull(title, text, bigText)
            .map { sanitize(it) }
            .filter { it.isNotBlank() }

        for (candidate in candidates) {
            parseMmol(candidate)?.let { mmol ->
                return GlucoseReading(
                    mmol = mmol,
                    timestampMillis = nowMillis,
                    trend = detectTrend(candidate),
                    source = GlucoseSource.OTTAI,
                )
            }
        }
        return null
    }

    private fun sanitize(value: String): String =
        value
            .replace('\u00a0', ' ')
            .replace("\u2060", "")
            .replace("当前血糖:", "", ignoreCase = true)
            .replace("Blood Glucose:", "", ignoreCase = true)
            .replace("Glucose:", "", ignoreCase = true)
            .replace("mmol/L", "", ignoreCase = true)
            .replace("mmol/l", "", ignoreCase = true)
            .replace("mg/dL", "", ignoreCase = true)
            .replace("mg/dl", "", ignoreCase = true)

    private fun parseMmol(raw: String): Double? {
        // Prefer decimal mmol values like 5.4 / 5,4
        MMOL_REGEX.find(raw)?.groupValues?.getOrNull(1)?.let { token ->
            val mmol = token.replace(',', '.').toDoubleOrNull() ?: return@let null
            if (mmol in 2.0..25.0) return mmol
        }

        // Integer likely mg/dL (OtTai sometimes shows 97)
        INT_REGEX.find(raw)?.groupValues?.getOrNull(1)?.let { token ->
            val value = token.toIntOrNull() ?: return@let null
            if (value in 40..405) return mgdlToMmol(value.toDouble())
        }
        return null
    }

    private fun detectTrend(raw: String): TrendArrow = when {
        raw.contains("⇈") || raw.contains("↑↑") -> TrendArrow.DOUBLE_UP
        raw.contains("↑") || raw.contains("⬆") -> TrendArrow.SINGLE_UP
        raw.contains("↗") -> TrendArrow.FORTY_FIVE_UP
        raw.contains("→") || raw.contains("➡") || raw.contains("→") -> TrendArrow.FLAT
        raw.contains("↘") -> TrendArrow.FORTY_FIVE_DOWN
        raw.contains("↓") || raw.contains("⬇") -> TrendArrow.SINGLE_DOWN
        raw.contains("⇊") || raw.contains("↓↓") -> TrendArrow.DOUBLE_DOWN
        else -> TrendArrow.NONE
    }

    private companion object {
        val MMOL_REGEX = Regex("""(\d+[.,]\d+)""")
        val INT_REGEX = Regex("""(?<![\d.,])(\d{2,3})(?![\d.,])""")
    }
}
