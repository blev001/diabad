package com.diabad.core.glucose

/** Nightscout / AAPS convention: mg/dL ÷ this ≈ mmol/L. */
const val MGDL_PER_MMOL = 18.01559

fun mgdlToMmol(mgdl: Double): Double = mgdl / MGDL_PER_MMOL

fun mmolToMgdl(mmol: Double): Double = mmol * MGDL_PER_MMOL

/** Format for status bar / UI: one decimal place, e.g. "5.4". */
fun formatMmol(mmol: Double): String = "%.1f".format(java.util.Locale.US, mmol)
