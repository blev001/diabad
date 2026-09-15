package com.diabad.alarm

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.diabad.R
import com.diabad.core.alarm.AlarmSnoozeSlots
import com.diabad.core.alarm.AlarmVibrationId
import com.diabad.core.wear.WearAlarmPaths
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Foreground alarm on the watch — vibration loop + full-screen Clock-like UI.
 * Phone keeps the audible siren; this service never plays sound.
 */
class WatchAlarmService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var vibrator: WatchAlarmVibrator
    private var wakeLock: PowerManager.WakeLock? = null

    private var mmol: Double = 0.0
    private var threshold: Double = 3.9
    private var snoozeMinutes: Int = AlarmSnoozeSlots.DEFAULT_MINUTES
    private var kind: String = WearAlarmPaths.KIND_HYPO
    private var vibrationId: AlarmVibrationId = AlarmVibrationId.CLOCK
    private var test: Boolean = false

    override fun onCreate() {
        super.onCreate()
        vibrator = WatchAlarmVibrator(this)
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISMISS -> {
                scope.launch { notifyPhone(WearAlarmPaths.DISMISS) }
                stopAlarm()
                return START_NOT_STICKY
            }
            ACTION_SNOOZE -> {
                val minutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, snoozeMinutes)
                scope.launch {
                    notifyPhone(WearAlarmPaths.SNOOZE, WearAlarmPaths.encodeSnooze(minutes))
                }
                stopAlarm()
                return START_NOT_STICKY
            }
            ACTION_CLEAR -> {
                stopAlarm()
                return START_NOT_STICKY
            }
            else -> {
                mmol = intent?.getDoubleExtra(EXTRA_MMOL, mmol) ?: mmol
                threshold = intent?.getDoubleExtra(EXTRA_THRESHOLD, threshold) ?: threshold
                snoozeMinutes = intent?.getIntExtra(EXTRA_SNOOZE, snoozeMinutes) ?: snoozeMinutes
                kind = intent?.getStringExtra(EXTRA_KIND) ?: WearAlarmPaths.KIND_HYPO
                vibrationId = AlarmVibrationId.fromName(intent?.getStringExtra(EXTRA_VIBRATION))
                test = intent?.getBooleanExtra(EXTRA_TEST, false) ?: false
                acquireWakeLock()
                startAsForegroundAlarm()
                vibrator.start(vibrationId.waveform())
                openFullScreen()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        vibrator.stop()
        releaseWakeLock()
        cancelAlarmClock()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startAsForegroundAlarm() {
        val notification = buildAlarmNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun activityIntent(): Intent =
        Intent(this, WatchAlarmActivity::class.java)
            .putExtra(WatchAlarmActivity.EXTRA_MMOL, mmol)
            .putExtra(WatchAlarmActivity.EXTRA_THRESHOLD, threshold)
            .putExtra(WatchAlarmActivity.EXTRA_SNOOZE, snoozeMinutes)
            .putExtra(WatchAlarmActivity.EXTRA_KIND, kind)
            .putExtra(WatchAlarmActivity.EXTRA_TEST, test)
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION,
            )

    private fun fullScreenPendingIntent(): PendingIntent =
        PendingIntent.getActivity(
            this,
            1,
            activityIntent(),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun buildAlarmNotification(): Notification {
        val fullScreen = fullScreenPendingIntent()
        val dismiss = PendingIntent.getService(
            this,
            2,
            Intent(this, WatchAlarmService::class.java).setAction(ACTION_DISMISS),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val titleRes = if (kind == WearAlarmPaths.KIND_HYPER) {
            R.string.watch_alarm_title_hyper
        } else {
            R.string.watch_alarm_title
        }
        val title = getString(titleRes)
        val body = getString(
            R.string.watch_alarm_mmol,
            String.format(java.util.Locale.US, "%.1f", mmol),
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_glucose)
            .setContentTitle(title)
            .setContentText(body)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setSound(null)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .addAction(0, getString(R.string.watch_alarm_dismiss), dismiss)
        AlarmSnoozeSlots.MINUTES.forEachIndexed { index, minutes ->
            builder.addAction(
                0,
                getString(R.string.watch_alarm_snooze_slot, minutes),
                snoozePending(minutes, requestCode = 3 + index),
            )
        }
        return builder.build()
    }

    private fun snoozePending(minutes: Int, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            this,
            requestCode,
            Intent(this, WatchAlarmService::class.java)
                .setAction(ACTION_SNOOZE)
                .putExtra(EXTRA_SNOOZE_MINUTES, minutes),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun openFullScreen() {
        val fullScreen = fullScreenPendingIntent()
        try {
            val alarmManager = getSystemService(AlarmManager::class.java)
            alarmManager?.setAlarmClock(
                AlarmManager.AlarmClockInfo(
                    System.currentTimeMillis() + 200L,
                    fullScreen,
                ),
                fullScreen,
            )
        } catch (e: Exception) {
            Log.w(TAG, "Watch setAlarmClock failed: ${e.message}")
        }
        try {
            startActivity(activityIntent())
        } catch (e: Exception) {
            Log.w(TAG, "Could not open watch alarm UI: ${e.message}")
        }
    }

    private fun cancelAlarmClock() {
        val alarmManager = getSystemService(AlarmManager::class.java) ?: return
        try {
            alarmManager.cancel(fullScreenPendingIntent())
        } catch (e: Exception) {
            Log.w(TAG, "Watch cancel AlarmClock failed: ${e.message}")
        }
    }

    private fun stopAlarm() {
        vibrator.stop()
        releaseWakeLock()
        cancelAlarmClock()
        closeFullScreenUi()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun closeFullScreenUi() {
        val finish = Intent(this, WatchAlarmActivity::class.java)
            .setAction(WatchAlarmActivity.ACTION_FINISH)
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP,
            )
        try {
            startActivity(finish)
        } catch (e: Exception) {
            Log.w(TAG, "Could not close watch alarm UI: ${e.message}")
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(PowerManager::class.java) ?: return
        @Suppress("DEPRECATION")
        wakeLock = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "diabad:watchAlarm",
        ).apply {
            setReferenceCounted(false)
            acquire(5 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

    private suspend fun notifyPhone(path: String, payload: ByteArray = ByteArray(0)) {
        try {
            val nodes = Wearable.getNodeClient(this).connectedNodes.await()
            val client = Wearable.getMessageClient(this)
            for (node in nodes) {
                client.sendMessage(node.id, path, payload).await()
                Log.i(TAG, "Sent $path → phone via ${node.displayName}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to notify phone ($path): ${e.message}")
        }
    }

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
        manager.deleteNotificationChannel(LEGACY_FS_CHANNEL_ID)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.watch_alarm_channel),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = getString(R.string.watch_alarm_channel_desc)
            enableVibration(false)
            setSound(null, null)
            setBypassDnd(true)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_RING = "com.diabad.wear.RING"
        const val ACTION_CLEAR = "com.diabad.wear.CLEAR"
        const val ACTION_DISMISS = "com.diabad.wear.DISMISS"
        const val ACTION_SNOOZE = "com.diabad.wear.SNOOZE"
        const val EXTRA_MMOL = "mmol"
        const val EXTRA_THRESHOLD = "threshold"
        const val EXTRA_SNOOZE = "snooze"
        const val EXTRA_SNOOZE_MINUTES = "snooze_minutes"
        const val EXTRA_KIND = "kind"
        const val EXTRA_VIBRATION = "vibration"
        const val EXTRA_TEST = "test"
        const val CHANNEL_ID = "diabad_watch_hypo_alarm_motor"
        const val LEGACY_CHANNEL_ID = "diabad_watch_hypo_alarm"
        const val LEGACY_FS_CHANNEL_ID = "diabad_watch_hypo_alarm_fs"
        const val NOTIFICATION_ID = 3001
        private const val TAG = "WatchAlarmService"
    }
}
