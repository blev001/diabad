package com.diabad.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wakes the app through AlarmManager for snooze end, re-alarm, sound escalation and
 * the "no data" deadline. Coroutine timers alone stall while the CPU sleeps, which is
 * exactly when OtTai goes quiet.
 */
@Singleton
class AlarmWakeScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    @Volatile private var scheduledAt: Long = 0L

    fun schedule(atMillis: Long?) {
        val am = alarmManager ?: return
        if (atMillis == null) {
            cancel()
            return
        }
        val at = atMillis.coerceAtLeast(System.currentTimeMillis() + MIN_DELAY_MS)
        if (at == scheduledAt) return
        scheduledAt = at
        val pi = pendingIntent()
        try {
            if (am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm denied, falling back", e)
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    fun cancel() {
        scheduledAt = 0L
        alarmManager?.cancel(pendingIntent())
    }

    private fun pendingIntent(): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, AlarmActionReceiver::class.java).setAction(AlarmActionReceiver.ACTION_EVALUATE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private companion object {
        const val TAG = "AlarmWakeScheduler"
        const val REQUEST_CODE = 40
        const val MIN_DELAY_MS = 1_000L
    }
}
