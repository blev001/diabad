package com.diabad.core.glucose

import kotlin.math.abs

/** Nightscout / AAPS convention: mg/dL ÷ this ≈ mmol/L. */
const val MGDL_PER_MMOL = 18.01559

/** Changes smaller than this are shown as flat (Δ 0.0). */
const val FLAT_DELTA_MMOL = 0.05

fun mgdlToMmol(mgdl: Double): Double = mgdl / MGDL_PER_MMOL

fun mmolToMgdl(mmol: Double): Double = mmol * MGDL_PER_MMOL

/** Format for status bar / UI: one decimal place, e.g. "5.4". */
fun formatMmol(mmol: Double): String = "%.1f".format(java.util.Locale.US, mmol)

/** mmol change vs the previous sample, or null if there is no previous. */
fun glucoseDeltaMmol(latestMmol: Double, previousMmol: Double?): Double? {
    if (previousMmol == null) return null
    return latestMmol - previousMmol
}

/**
 * Compact CGM delta, e.g. "Δ +0.3", "Δ −0.2", "Δ 0.0".
 * Null when there is no previous sample to compare.
 */
fun formatDeltaMmol(latestMmol: Double, previousMmol: Double?): String? {
    val delta = glucoseDeltaMmol(latestMmol, previousMmol) ?: return null
    if (abs(delta) < FLAT_DELTA_MMOL) return "Δ 0.0"
    val sign = if (delta > 0) "+" else "−"
    return "Δ $sign${formatMmol(abs(delta))}"
}
