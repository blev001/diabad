package com.diabad.core.glucose

import kotlin.math.abs

/**
 * Compact lock-screen / Now Bar copy: value, trend arrow, delta.
 *
 * Samsung Now Bar (One UI 8 Live Updates) uses [title] as the pill text,
 * similar to a calendar chip like "Событие в 12:00". The status-bar chip
 * prefers [chip], which should stay very short.
 */
object GlucoseLockDisplay {

    fun deltaCompact(deltaMmol: Double?): String? {
        if (deltaMmol == null) return null
        if (abs(deltaMmol) < 0.05) return "0.0"
        val sign = if (deltaMmol > 0) "+" else "−"
        return sign + formatMmol(abs(deltaMmol))
    }

    fun title(mmol: Double, trendGlyph: String, deltaMmol: Double?): String {
        val parts = buildList {
            add(formatMmol(mmol))
            if (trendGlyph.isNotEmpty()) add(trendGlyph)
            deltaCompact(deltaMmol)?.let { add(it) }
        }
        return parts.joinToString(" ")
    }

    fun waitingTitle(): String = "—"

    /**
     * Status-bar / compact chip. Suggested max ~7 characters on Pixel;
     * Samsung Now Bar is wider and still uses [title].
     */
    fun chip(mmol: Double, trendGlyph: String, deltaMmol: Double?): String {
        val value = formatMmol(mmol)
        val compact = buildString {
            append(value)
            if (trendGlyph.isNotEmpty()) append(trendGlyph)
        }
        val delta = deltaCompact(deltaMmol) ?: return compact
        val withDelta = compact + delta
        return if (withDelta.length <= 8) withDelta else compact
    }
}
