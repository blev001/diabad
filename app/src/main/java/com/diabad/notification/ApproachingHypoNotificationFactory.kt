package com.diabad.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.diabad.MainActivity
import com.diabad.R
import com.diabad.alarm.StrongVibrator
import com.diabad.core.glucose.formatMmol
import com.diabad.domain.model.AppSettings
import com.diabad.domain.model.GlucoseReading
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Heads-up + vibration warning. Not a full alarm (no sound, no stop/snooze). */
@Singleton
class ApproachingHypoNotificationFactory @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_approaching_hypo),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_approaching_hypo_desc)
            enableVibration(true)
            vibrationPattern = StrongVibrator.WARNING_TIMINGS
            setSound(null, null)
            setBypassDnd(false)
        }
        manager.createNotificationChannel(channel)
    }

    fun show(latest: GlucoseReading, settings: AppSettings) {
        ensureChannel()
        val title = context.getString(
            R.string.approaching_hypo_notification_title,
            formatMmol(latest.mmol),
        )
        val body = context.getString(
            R.string.approaching_hypo_notification_body,
            formatMmol(settings.approachingHypoThresholdMmol),
            formatMmol(settings.hypoThresholdMmol),
        )
        val contentIntent = PendingIntent.getActivity(
            context,
            30,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_glucose)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setLocalOnly(false)
            .setVibrate(StrongVibrator.WARNING_TIMINGS)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    fun cancel() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    companion object {
        const val CHANNEL_ID = "diabad_approaching_hypo"
        const val NOTIFICATION_ID = 2101
    }
}
