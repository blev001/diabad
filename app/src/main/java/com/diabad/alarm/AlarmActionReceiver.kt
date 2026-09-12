package com.diabad.alarm

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
    }
}
