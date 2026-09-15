package com.diabad.alarm

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.diabad.core.alarm.AlarmSnoozeSlots
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AlarmActionReceiver : BroadcastReceiver() {

    @Inject lateinit var hypoAlarmController: HypoAlarmController

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            ACTION_DISMISS -> hypoAlarmController.dismiss()
            ACTION_SNOOZE -> {
                val minutes = if (intent.hasExtra(EXTRA_MINUTES)) {
                    intent.getIntExtra(EXTRA_MINUTES, AlarmSnoozeSlots.DEFAULT_MINUTES)
                } else {
                    null
                }
                if (minutes != null) hypoAlarmController.snooze(minutes)
                else hypoAlarmController.snooze()
            }
        }
    }

    companion object {
        const val ACTION_DISMISS = "com.diabad.alarm.DISMISS"
        const val ACTION_SNOOZE = "com.diabad.alarm.SNOOZE"
        const val EXTRA_MINUTES = "minutes"

        fun dismissPendingIntent(context: Context, requestCode: Int = 11): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                requestCode,
                Intent(context, AlarmActionReceiver::class.java).setAction(ACTION_DISMISS),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        fun snoozePendingIntent(
            context: Context,
            minutes: Int = AlarmSnoozeSlots.DEFAULT_MINUTES,
            requestCode: Int = 12,
        ): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                requestCode,
                Intent(context, AlarmActionReceiver::class.java)
                    .setAction(ACTION_SNOOZE)
                    .putExtra(EXTRA_MINUTES, minutes),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
    }
}
