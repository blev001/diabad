package com.diabad.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.diabad.MainActivity
import com.diabad.R
import com.diabad.core.glucose.formatMmol
import com.diabad.domain.model.AppSettings
import com.diabad.domain.model.GlucoseReading
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class GlucoseNotificationFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val iconRenderer: StatusBarIconRenderer,
) {

    fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_monitoring),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.notification_channel_monitoring_desc)
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    fun build(
        latest: GlucoseReading?,
        previous: GlucoseReading?,
        settings: AppSettings = AppSettings(),
    ): Notification {
        ensureChannel()

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val icon = IconCompat.createWithBitmap(iconRenderer.render(context, latest))
        val shade = iconRenderer.renderShadeBadge(
            context = context,
            reading = latest,
            hypoThresholdMmol = settings.hypoThresholdMmol,
            hyperThresholdMmol = settings.hyperThresholdMmol,
        )

        val title: String
        val body: String
        val big: String

        if (latest == null) {
            title = context.getString(R.string.notification_waiting_title)
            body = context.getString(R.string.notification_waiting_body)
            big = body
        } else {
            val trend = latest.trend.glyph
            title = buildString {
                append(formatMmol(latest.mmol))
                append(' ')
                append(context.getString(R.string.unit_mmol))
                if (trend.isNotEmpty()) {
                    append(' ')
                    append(trend)
                }
            }
            val deltaPart = formatDelta(latest, previous)
            val timePart = formatUpdatedAgo(latest.timestampMillis)
            body = listOfNotNull(deltaPart, timePart).joinToString(" · ")
            big = buildString {
                append(title)
                append('\n')
                append(body)
                append('\n')
                append(context.getString(R.string.notification_source_ottai))
            }
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(icon)
            .setLargeIcon(shade)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(big))
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun formatDelta(latest: GlucoseReading, previous: GlucoseReading?): String? {
        if (previous == null) return null
        val delta = latest.mmol - previous.mmol
        if (abs(delta) < 0.05) {
            return context.getString(R.string.notification_delta_flat)
        }
        val sign = if (delta > 0) "+" else "−"
        return context.getString(
            R.string.notification_delta,
            sign,
            formatMmol(abs(delta)),
        )
    }

    private fun formatUpdatedAgo(timestampMillis: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(
            (System.currentTimeMillis() - timestampMillis).coerceAtLeast(0L),
        )
        return when {
            minutes < 1L -> context.getString(R.string.notification_updated_just_now)
            minutes < 60L -> context.getString(R.string.notification_updated_minutes, minutes)
            else -> {
                val hours = minutes / 60L
                context.getString(R.string.notification_updated_hours, hours)
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "diabad_monitoring"
        const val NOTIFICATION_ID = 1001
    }
}
