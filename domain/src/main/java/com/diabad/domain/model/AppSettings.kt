package com.diabad.domain.model

/**
 * User-facing preferences persisted locally.
 */
data class AppSettings(
    val hypoThresholdMmol: Double = DEFAULT_HYPO_THRESHOLD_MMOL,
    val connectionLossMode: ConnectionLossMode = ConnectionLossMode.SILENT,
    val connectionLossGraceMinutes: Int = DEFAULT_CONNECTION_LOSS_GRACE_MINUTES,
    val alarmSoundId: AlarmSoundId = AlarmSoundId.SIREN,
    val customAlarmUri: String? = null,
    val snoozeMinutes: Int = DEFAULT_SNOOZE_MINUTES,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
) {
    companion object {
        const val DEFAULT_HYPO_THRESHOLD_MMOL = 3.9
        const val DEFAULT_CONNECTION_LOSS_GRACE_MINUTES = 10
        const val DEFAULT_SNOOZE_MINUTES = 10
        val SNOOZE_OPTIONS_MINUTES = listOf(5, 10, 15, 30)
    }
}
