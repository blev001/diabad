package com.diabad.alarm

import android.content.Context
import com.diabad.R
import com.diabad.core.glucose.formatMmol
import com.diabad.core.wear.WearAlarmPaths
import com.diabad.domain.alarm.AlarmCondition
import com.diabad.domain.model.AlarmReason
import com.diabad.domain.model.AppSettings
import kotlin.math.abs

fun AlarmReason.toWearKind(): String = when (this) {
    AlarmReason.HIGH -> WearAlarmPaths.KIND_HYPER
    AlarmReason.URGENT_LOW -> WearAlarmPaths.KIND_URGENT_LOW
    AlarmReason.PREDICTED_LOW -> WearAlarmPaths.KIND_PREDICTED_LOW
    AlarmReason.FAST_DROP -> WearAlarmPaths.KIND_FAST_DROP
    AlarmReason.SIGNAL_LOSS -> WearAlarmPaths.KIND_SIGNAL_LOSS
    AlarmReason.LOW -> WearAlarmPaths.KIND_HYPO
}

fun alarmTitleRes(reason: AlarmReason): Int = when (reason) {
    AlarmReason.HIGH -> R.string.phone_alarm_title_hyper
    AlarmReason.URGENT_LOW -> R.string.phone_alarm_title_urgent_low
    AlarmReason.PREDICTED_LOW -> R.string.phone_alarm_title_predicted_low
    AlarmReason.FAST_DROP -> R.string.phone_alarm_title_fast_drop
    AlarmReason.SIGNAL_LOSS -> R.string.phone_alarm_title_signal_loss
    AlarmReason.LOW -> R.string.phone_alarm_title_hypo
}

fun alarmNotificationTitleRes(reason: AlarmReason): Int = when (reason) {
    AlarmReason.HIGH -> R.string.alarm_notification_title_hyper
    AlarmReason.URGENT_LOW -> R.string.alarm_notification_title_urgent_low
    AlarmReason.PREDICTED_LOW -> R.string.alarm_notification_title_predicted_low
    AlarmReason.FAST_DROP -> R.string.alarm_notification_title_fast_drop
    AlarmReason.SIGNAL_LOSS -> R.string.alarm_notification_title_signal_loss
    AlarmReason.LOW -> R.string.alarm_notification_title_hypo
}

fun alarmSubtitle(
    context: Context,
    reason: AlarmReason,
    settings: AppSettings,
    condition: AlarmCondition?,
    thresholdLabel: String,
): String = when (reason) {
    AlarmReason.HIGH -> context.getString(R.string.phone_alarm_threshold_hyper, thresholdLabel)
    AlarmReason.PREDICTED_LOW -> context.getString(
        R.string.phone_alarm_threshold_predicted_low,
        settings.predictiveLowMinutes,
        formatMmol(condition?.predictedMmol ?: 0.0),
        thresholdLabel,
    )
    AlarmReason.FAST_DROP -> context.getString(
        R.string.phone_alarm_threshold_fast_drop,
        formatRate(condition?.ratePerMinute),
        thresholdLabel,
    )
    AlarmReason.SIGNAL_LOSS -> context.getString(
        R.string.phone_alarm_threshold_signal_loss,
        formatMmol(condition?.latest?.mmol ?: 0.0),
    )
    AlarmReason.LOW, AlarmReason.URGENT_LOW ->
        context.getString(R.string.phone_alarm_threshold_hypo, thresholdLabel)
}

fun alarmNotificationBody(
    context: Context,
    reason: AlarmReason,
    settings: AppSettings,
    condition: AlarmCondition?,
    thresholdLabel: String,
): String = when (reason) {
    AlarmReason.HIGH -> context.getString(R.string.alarm_notification_body_hyper, thresholdLabel)
    AlarmReason.PREDICTED_LOW -> context.getString(
        R.string.alarm_notification_body_predicted_low,
        settings.predictiveLowMinutes,
        formatMmol(condition?.predictedMmol ?: 0.0),
        thresholdLabel,
    )
    AlarmReason.FAST_DROP -> context.getString(
        R.string.alarm_notification_body_fast_drop,
        formatRate(condition?.ratePerMinute),
        thresholdLabel,
    )
    AlarmReason.SIGNAL_LOSS -> context.getString(
        R.string.alarm_notification_body_signal_loss,
        formatMmol(condition?.latest?.mmol ?: 0.0),
    )
    AlarmReason.URGENT_LOW -> context.getString(
        R.string.alarm_notification_body_urgent_low,
        thresholdLabel,
    )
    AlarmReason.LOW -> context.getString(R.string.alarm_notification_body_hypo, thresholdLabel)
}

fun formatRate(ratePerMinute: Double?): String {
    val rate = ratePerMinute ?: 0.0
    val sign = if (rate > 0) "+" else "−"
    return "$sign${"%.2f".format(java.util.Locale.US, abs(rate))}"
}
