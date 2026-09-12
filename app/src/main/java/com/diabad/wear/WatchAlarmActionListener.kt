package com.diabad.wear

import android.util.Log
import com.diabad.alarm.HypoAlarmController
import com.diabad.core.wear.WearAlarmPaths
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Receives Stop / Snooze taps from the Galaxy Watch companion (Clock-like UI). */
@AndroidEntryPoint
class WatchAlarmActionListener : WearableListenerService() {

    @Inject lateinit var hypoAlarmController: HypoAlarmController

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            WearAlarmPaths.DISMISS -> {
                Log.i(TAG, "Watch dismissed alarm")
                hypoAlarmController.dismiss()
            }
            WearAlarmPaths.SNOOZE -> {
                Log.i(TAG, "Watch snoozed alarm")
                hypoAlarmController.snooze()
            }
            else -> Log.d(TAG, "Ignored wear path ${messageEvent.path}")
        }
    }

    private companion object {
        const val TAG = "WatchAlarmAction"
    }
}
