package com.diabad.core.glucose

import kotlin.math.abs

/**
 * Compact lock-screen widget copy: value, trend arrow, delta.
 * Fits Samsung One UI mini widgets under / beside the clock (2×1).
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
