package com.diabad.alarm

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AlarmActionReceiver : BroadcastReceiver() {

    @Inject lateinit var hypoAlarmController: HypoAlarmController

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            ACTION_DISMISS -> hypoAlarmController.dismiss()
            ACTION_SNOOZE -> hypoAlarmController.snooze()
        }
    }

    companion object {
        const val ACTION_DISMISS = "com.diabad.alarm.DISMISS"
        const val ACTION_SNOOZE = "com.diabad.alarm.SNOOZE"

        fun dismissPendingIntent(context: Context, requestCode: Int = 11): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                requestCode,
                Intent(context, AlarmActionReceiver::class.java).setAction(ACTION_DISMISS),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        fun snoozePendingIntent(context: Context, requestCode: Int = 12): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                requestCode,
                Intent(context, AlarmActionReceiver::class.java).setAction(ACTION_SNOOZE),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
    }
}
