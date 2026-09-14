package com.diabad.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.VibrationEffect
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.diabad.MainActivity
import com.diabad.R
import com.diabad.alarm.AlarmActionReceiver
import com.diabad.core.glucose.formatMmol
import com.diabad.domain.model.AppSettings
import com.diabad.domain.model.GlucoseAlarmKind
import com.diabad.domain.model.GlucoseReading
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmNotificationFactory @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        // New id: channel vibration/sound cannot change after first create.
        manager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_alarm),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_alarm_desc)
            enableVibration(true)
            vibrationPattern = STRONG_VIBE_PATTERN
            setBypassDnd(true)
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    fun showRinging(latest: GlucoseReading, settings: AppSettings, kind: GlucoseAlarmKind) {
        ensureChannel()
        val titleRes = when (kind) {
            GlucoseAlarmKind.HYPO -> R.string.alarm_notification_title_hypo
            GlucoseAlarmKind.HYPER -> R.string.alarm_notification_title_hyper
        }
        val threshold = when (kind) {
            GlucoseAlarmKind.HYPO -> settings.hypoThresholdMmol
            GlucoseAlarmKind.HYPER -> settings.hyperThresholdMmol
        }
        val bodyRes = when (kind) {
            GlucoseAlarmKind.HYPO -> R.string.alarm_notification_body_hypo
            GlucoseAlarmKind.HYPER -> R.string.alarm_notification_body_hyper
        }
        val title = context.getString(titleRes, formatMmol(latest.mmol))
        val body = context.getString(bodyRes, formatMmol(threshold), settings.snoozeMinutes)
        val contentIntent = activityPendingIntent()
        val dismiss = dismissAction()
        val snooze = snoozeAction(settings)

        // Phone: ongoing + localOnly so the loud AlarmPlayer card stays on the phone.
        val phoneNotification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_glucose)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setLocalOnly(true)
            .setAutoCancel(false)
            .setVibrate(settings.alarmVibrationId.waveform())
            .addAction(dismiss)
            .addAction(snooze)
            .build()

        // Watch: must NOT be ongoing — Wear OS does not bridge ongoing notifications.
        // Mirrors Clock-style alert: strong vibe, no sound, Stop / Snooze on the watch.
        val wearExtender = NotificationCompat.WearableExtender()
            .addAction(dismiss)
            .addAction(snooze)
            .setContentAction(0)
            .setDismissalId(DISMISSAL_ID)

        val watchNotification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_glucose)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(false)
            .setLocalOnly(false)
            .setAutoCancel(false)
            .setOnlyAlertOnce(false)
            .setVibrate(settings.alarmVibrationId.waveform())
            .setSilent(false)
            .extend(wearExtender)
            .build()

        NotificationManagerCompat.from(context).apply {
            notify(PHONE_NOTIFICATION_ID, phoneNotification)
            notify(WATCH_BRIDGE_NOTIFICATION_ID, watchNotification)
        }
    }

    fun showSnoozed(minutes: Int) {
        ensureChannel()
        cancelWatchBridge()
        val dismiss = NotificationCompat.Action.Builder(
            0,
            context.getString(R.string.alarm_action_dismiss),
            actionPendingIntent(AlarmActionReceiver.ACTION_DISMISS, 13),
        ).build()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_glucose)
            .setContentTitle(context.getString(R.string.alarm_snoozed_title))
            .setContentText(context.getString(R.string.alarm_snoozed_body, minutes))
            .setContentIntent(activityPendingIntent())
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(false)
            .setAutoCancel(true)
            .setLocalOnly(false)
            .addAction(dismiss)
            .extend(
                NotificationCompat.WearableExtender()
                    .addAction(dismiss)
                    .setDismissalId(DISMISSAL_ID),
            )
            .build()

        NotificationManagerCompat.from(context).notify(PHONE_NOTIFICATION_ID, notification)
    }

    fun cancelAll() {
        NotificationManagerCompat.from(context).apply {
            cancel(PHONE_NOTIFICATION_ID)
            cancel(WATCH_BRIDGE_NOTIFICATION_ID)
        }
    }

    private fun cancelWatchBridge() {
        NotificationManagerCompat.from(context).cancel(WATCH_BRIDGE_NOTIFICATION_ID)
    }

    private fun dismissAction(): NotificationCompat.Action =
        NotificationCompat.Action.Builder(
            0,
            context.getString(R.string.alarm_action_dismiss),
            actionPendingIntent(AlarmActionReceiver.ACTION_DISMISS, 11),
        ).build()

    private fun snoozeAction(settings: AppSettings): NotificationCompat.Action =
        NotificationCompat.Action.Builder(
            0,
            context.getString(R.string.alarm_action_snooze, settings.snoozeMinutes),
            actionPendingIntent(AlarmActionReceiver.ACTION_SNOOZE, 12),
        ).build()

    private fun activityPendingIntent(): PendingIntent =
        PendingIntent.getActivity(
            context,
            10,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun actionPendingIntent(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, AlarmActionReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        const val CHANNEL_ID = "diabad_hypo_alarm_watch"
        const val LEGACY_CHANNEL_ID = "diabad_hypo_alarm"
        const val PHONE_NOTIFICATION_ID = 2001
        const val WATCH_BRIDGE_NOTIFICATION_ID = 2002
        const val DISMISSAL_ID = "diabad_hypo_alarm"
        /** Same cadence as a strong Galaxy Watch Clock alarm pulse. */
        val STRONG_VIBE_PATTERN = longArrayOf(
            0,
            900, 200, 900, 200, 900,
            400,
            900, 200, 900, 200, 900,
        )

        @Suppress("unused")
        fun strongVibrationEffect(): VibrationEffect =
            VibrationEffect.createWaveform(STRONG_VIBE_PATTERN, 0)
    }
}
