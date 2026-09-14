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
import com.diabad.domain.model.GlucoseReading
import android.text.format.DateFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class GlucoseNotificationFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val iconRenderer: StatusBarIconRenderer,
) {
    @Volatile private var channelReady: Boolean = false

    fun ensureChannel() {
        if (channelReady) return
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
        channelReady = true
    }

    fun contentKey(latest: GlucoseReading?, previous: GlucoseReading?): String {
        val copy = notificationCopy(latest, previous)
        return "${copy.title}|${copy.body}"
    }

    fun build(
        latest: GlucoseReading?,
        previous: GlucoseReading?,
        foregroundImmediate: Boolean = false,
    ): Notification {
        ensureChannel()
        val copy = notificationCopy(latest, previous)

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val icon = IconCompat.createWithBitmap(iconRenderer.render(context, latest))

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(copy.title)
            .setContentText(copy.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(copy.big))
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setLocalOnly(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(
                if (foregroundImmediate) {
                    NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
                } else {
                    NotificationCompat.FOREGROUND_SERVICE_DEFERRED
                },
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun notificationCopy(
        latest: GlucoseReading?,
        previous: GlucoseReading?,
    ): NotificationCopy {
        if (latest == null) {
            val waiting = context.getString(R.string.notification_waiting_body)
            return NotificationCopy(
                title = context.getString(R.string.notification_waiting_title),
                body = waiting,
                big = waiting,
            )
        }
        val trend = latest.trend.glyph
        val title = buildString {
            append(formatMmol(latest.mmol))
            append(' ')
            append(context.getString(R.string.unit_mmol))
            if (trend.isNotEmpty()) {
                append(' ')
                append(trend)
            }
        }
        val deltaPart = formatDelta(latest, previous)
        val timePart = formatUpdatedAt(latest.timestampMillis)
        val body = listOfNotNull(deltaPart, timePart).joinToString(" · ")
        val big = buildString {
            append(title)
            append('\n')
            append(body)
            append('\n')
            append(context.getString(R.string.notification_source_ottai))
        }
        return NotificationCopy(title, body, big)
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

    private fun formatUpdatedAt(timestampMillis: Long): String {
        val clock = DateFormat.getTimeFormat(context).format(Date(timestampMillis))
        return context.getString(R.string.notification_updated_at, clock)
    }

    private data class NotificationCopy(
        val title: String,
        val body: String,
        val big: String,
    )

    companion object {
        const val CHANNEL_ID = "diabad_monitoring"
        const val NOTIFICATION_ID = 1001
    }
}
