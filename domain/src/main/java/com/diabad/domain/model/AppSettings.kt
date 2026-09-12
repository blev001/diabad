package com.diabad.domain.model

/**
 * User-facing preferences persisted locally.
 */
data class AppSettings(
    val hypoThresholdMmol: Double = DEFAULT_HYPO_THRESHOLD_MMOL,
    val connectionLossMode: ConnectionLossMode = ConnectionLossMode.SILENT,
    val connectionLossGraceMinutes: Int = DEFAULT_CONNECTION_LOSS_GRACE_MINUTES,
) {
    companion object {
        const val DEFAULT_HYPO_THRESHOLD_MMOL = 3.9
        const val DEFAULT_CONNECTION_LOSS_GRACE_MINUTES = 10
    }
}
