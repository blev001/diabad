package com.diabad.domain.model

/**
 * User-facing preferences persisted locally.
 *
 * Default alarm thresholds follow ADA CGM guidance:
 * hypo Level 1 &lt; 3.9 mmol/L; Time in Range upper edge 10.0 mmol/L.
 */
data class AppSettings(
    val hypoThresholdMmol: Double = DEFAULT_HYPO_THRESHOLD_MMOL,
    val hyperThresholdMmol: Double = DEFAULT_HYPER_THRESHOLD_MMOL,
    val approachingHypoEnabled: Boolean = true,
    val approachingHypoThresholdMmol: Double = DEFAULT_APPROACHING_HYPO_THRESHOLD_MMOL,
    val connectionLossMode: ConnectionLossMode = ConnectionLossMode.SILENT,
    val connectionLossGraceMinutes: Int = DEFAULT_CONNECTION_LOSS_GRACE_MINUTES,
    val alarmAlertMode: AlarmAlertMode = AlarmAlertMode.SOUND,
    val alarmSoundId: AlarmSoundId = AlarmSoundId.SIREN,
    val customAlarmUri: String? = null,
    val snoozeMinutes: Int = DEFAULT_SNOOZE_MINUTES,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
) {
    companion object {
        const val DEFAULT_HYPO_THRESHOLD_MMOL = 3.9
        const val DEFAULT_HYPER_THRESHOLD_MMOL = 10.0
        /** Early heads-up before the full hypo alarm — not an alarm itself. */
        const val DEFAULT_APPROACHING_HYPO_THRESHOLD_MMOL = 4.3
        const val DEFAULT_CONNECTION_LOSS_GRACE_MINUTES = 10
        const val DEFAULT_SNOOZE_MINUTES = 10
        val SNOOZE_OPTIONS_MINUTES = listOf(5, 10, 15, 30)
    }
}
