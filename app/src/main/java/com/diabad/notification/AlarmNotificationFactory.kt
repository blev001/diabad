package com.diabad.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.diabad.MainActivity
import com.diabad.R
import com.diabad.alarm.AlarmActionReceiver
import com.diabad.core.glucose.formatMmol
import com.diabad.domain.model.AppSettings
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
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_alarm),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_alarm_desc)
            enableVibration(true)
            setBypassDnd(true)
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    fun showRinging(latest: GlucoseReading, settings: AppSettings) {
        ensureChannel()
        val contentIntent = activityPendingIntent()
        val dismiss = actionPendingIntent(AlarmActionReceiver.ACTION_DISMISS, 11)
        val snooze = actionPendingIntent(AlarmActionReceiver.ACTION_SNOOZE, 12)

        val title = context.getString(
            R.string.alarm_notification_title,
            formatMmol(latest.mmol),
        )
        val body = context.getString(
            R.string.alarm_notification_body,
            formatMmol(settings.hypoThresholdMmol),
            settings.snoozeMinutes,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(0, context.getString(R.string.alarm_action_dismiss), dismiss)
            .addAction(
                0,
                context.getString(R.string.alarm_action_snooze, settings.snoozeMinutes),
                snooze,
            )
            .build()

        context.getSystemService(NotificationManager::class.java)
            ?.notify(NOTIFICATION_ID, notification)
    }

    fun showSnoozed(minutes: Int) {
        ensureChannel()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.alarm_snoozed_title))
            .setContentText(context.getString(R.string.alarm_snoozed_body, minutes))
            .setContentIntent(activityPendingIntent())
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(false)
            .setAutoCancel(true)
            .addAction(
                0,
                context.getString(R.string.alarm_action_dismiss),
                actionPendingIntent(AlarmActionReceiver.ACTION_DISMISS, 13),
            )
            .build()

        context.getSystemService(NotificationManager::class.java)
            ?.notify(NOTIFICATION_ID, notification)
    }

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
        const val CHANNEL_ID = "diabad_hypo_alarm"
        const val NOTIFICATION_ID = 2001
    }
}
