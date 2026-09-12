package com.diabad.core.wear

/**
 * Shared Data Layer contract: phone pushes latest CGM sample for watch tiles / complications.
 */
object WearGlucosePaths {
    const val LATEST = "/diabad/glucose/latest"

    const val KEY_MMOL = "mmol"
    const val KEY_TREND = "trend"
    const val KEY_DELTA = "delta"
    const val KEY_HAS_DELTA = "hasDelta"
    const val KEY_TIMESTAMP = "timestamp"
    const val KEY_THRESHOLD = "threshold"
    const val KEY_HYPER_THRESHOLD = "hyperThreshold"
    const val KEY_ALARMING = "alarming"
}
