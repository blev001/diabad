package com.diabad.domain.model

import com.diabad.core.alarm.AlarmSnoozeSlots
import com.diabad.core.alarm.AlarmVibrationId

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
    val alarmVibrationId: AlarmVibrationId = AlarmVibrationId.CLOCK,
    val customAlarmUri: String? = null,
    val snoozeMinutes: Int = DEFAULT_SNOOZE_MINUTES,
    val alarmSnoozedUntilMillis: Long = 0L,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,

    /** Repeat a stopped low alarm after this many minutes while it is still low. 0 = only if it gets worse. */
    val reAlarmMinutes: Int = DEFAULT_RE_ALARM_MINUTES,
    /** Vibration only for the first N seconds, then sound. 0 = sound immediately, as before. */
    val vibrateFirstSeconds: Int = 0,
    val hyperAlarmEnabled: Boolean = true,
    /** High must hold this long before the alarm rings. 0 = at once. */
    val hyperDelayMinutes: Int = 0,
    /** High alarm vibrates but never plays sound. */
    val hyperVibrateOnly: Boolean = false,
    val predictiveLowEnabled: Boolean = false,
    val predictiveLowMinutes: Int = DEFAULT_PREDICTIVE_LOW_MINUTES,
    val fastDropEnabled: Boolean = false,
    val nightProfileEnabled: Boolean = false,
    val nightStartMinute: Int = DEFAULT_NIGHT_START_MINUTE,
    val nightEndMinute: Int = DEFAULT_NIGHT_END_MINUTE,
    val nightHypoThresholdMmol: Double = DEFAULT_HYPO_THRESHOLD_MMOL,
    val nightHyperThresholdMmol: Double = DEFAULT_NIGHT_HYPER_THRESHOLD_MMOL,
    val nightHyperAlarmEnabled: Boolean = true,
    val healthConnectEnabled: Boolean = false,
) {
    fun isNight(minuteOfDay: Int): Boolean {
        if (!nightProfileEnabled) return false
        val start = nightStartMinute
        val end = nightEndMinute
        return if (start <= end) minuteOfDay in start until end
        else minuteOfDay >= start || minuteOfDay < end
    }

    fun effectiveHypoThreshold(minuteOfDay: Int): Double =
        if (isNight(minuteOfDay)) nightHypoThresholdMmol else hypoThresholdMmol

    fun effectiveHyperThreshold(minuteOfDay: Int): Double =
        if (isNight(minuteOfDay)) nightHyperThresholdMmol else hyperThresholdMmol

    fun hyperAlarmActive(minuteOfDay: Int): Boolean =
        hyperAlarmEnabled && (!isNight(minuteOfDay) || nightHyperAlarmEnabled)

    companion object {
        const val DEFAULT_HYPO_THRESHOLD_MMOL = 3.9
        const val DEFAULT_HYPER_THRESHOLD_MMOL = 10.0
        const val DEFAULT_NIGHT_HYPER_THRESHOLD_MMOL = 13.9
        /** Early heads-up before the full hypo alarm — not an alarm itself. */
        const val DEFAULT_APPROACHING_HYPO_THRESHOLD_MMOL = 4.3
        const val DEFAULT_CONNECTION_LOSS_GRACE_MINUTES = 10
        const val DEFAULT_SNOOZE_MINUTES = AlarmSnoozeSlots.DEFAULT_MINUTES
        const val DEFAULT_RE_ALARM_MINUTES = 30
        const val DEFAULT_PREDICTIVE_LOW_MINUTES = 20
        const val DEFAULT_NIGHT_START_MINUTE = 22 * 60
        const val DEFAULT_NIGHT_END_MINUTE = 7 * 60
        val SNOOZE_OPTIONS_MINUTES = AlarmSnoozeSlots.MINUTES
        val RE_ALARM_OPTIONS_MINUTES = listOf(0, 15, 30, 60)
        val PREDICTIVE_LOW_OPTIONS_MINUTES = listOf(10, 15, 20, 30)
    }
}
