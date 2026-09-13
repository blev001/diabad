package com.diabad.ottai

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.diabad.core.di.ApplicationScope
import com.diabad.data.ottai.OttaiNotificationParser
import com.diabad.domain.signal.GlucoseSignalClock
import com.diabad.domain.usecase.IngestGlucoseReadingsUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fallback when OtTai does not deliver AAPS broadcasts to third-party apps.
 * Reads glucose from OtTai's status-bar notification (same idea as xDrip Companion).
 */
@AndroidEntryPoint
class OttaiNotificationListener : NotificationListenerService() {

    @Inject lateinit var parser: OttaiNotificationParser
    @Inject lateinit var ingestGlucoseReadings: IngestGlucoseReadingsUseCase
    @Inject lateinit var signalClock: GlucoseSignalClock
    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    @Volatile private var lastFingerprint: String? = null

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        if (sbn.packageName !in OTTAI_PACKAGES) return
        signalClock.mark()
        handle(sbn.notification)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "OtTai notification listener connected")
        // Catch the already-visible OtTai notification.
        runCatching {
            activeNotifications
                ?.filter { it.packageName in OTTAI_PACKAGES }
                ?.forEach {
                    signalClock.mark()
                    handle(it.notification)
                }
        }
    }

    private fun handle(notification: Notification?) {
        if (notification == null) return
        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val big = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val fingerprint = "$title|$text|$big"
        if (fingerprint == lastFingerprint) return
        lastFingerprint = fingerprint
        Log.d(TAG, "OtTai notif title=$title text=$text")

        val reading = parser.parseNotificationText(title, text, big) ?: return
        applicationScope.launch {
            try {
                ingestGlucoseReadings(listOf(reading))
                Log.i(TAG, "Ingested OtTai notification mmol=${reading.mmol}")
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to ingest OtTai notification", t)
            }
        }
    }

    companion object {
        private const val TAG = "OttaiNotifListener"
        val OTTAI_PACKAGES = setOf(
            "com.ottai.seas",
            "com.ottai.tag",
        )

        fun isEnabled(context: Context): Boolean {
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners",
            ) ?: return false
            val cn = ComponentName(context, OttaiNotificationListener::class.java)
            return flat.split(':').any {
                ComponentName.unflattenFromString(it)?.equals(cn) == true ||
                    it.contains(context.packageName)
            }
        }

        fun settingsIntent(): Intent =
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    }
}
