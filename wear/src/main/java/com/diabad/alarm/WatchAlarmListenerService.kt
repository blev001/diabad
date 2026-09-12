package com.diabad.alarm

import android.content.Intent
import android.util.Log
import com.diabad.core.wear.WearAlarmPaths
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

/** Phone → watch: start / clear Clock-like hypo alarm. */
class WatchAlarmListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            WearAlarmPaths.RING -> {
                val payload = WearAlarmPaths.decodeRing(messageEvent.data) ?: run {
                    Log.w(TAG, "Bad ring payload")
                    return
                }
                Log.i(TAG, "Ring from phone mmol=${payload.mmol}")
                val intent = Intent(this, WatchAlarmService::class.java)
                    .setAction(WatchAlarmService.ACTION_RING)
                    .putExtra(WatchAlarmService.EXTRA_MMOL, payload.mmol)
                    .putExtra(WatchAlarmService.EXTRA_THRESHOLD, payload.thresholdMmol)
                    .putExtra(WatchAlarmService.EXTRA_SNOOZE, payload.snoozeMinutes)
                startForegroundService(intent)
            }
            WearAlarmPaths.CLEAR -> {
                Log.i(TAG, "Clear from phone")
                startService(
                    Intent(this, WatchAlarmService::class.java)
                        .setAction(WatchAlarmService.ACTION_CLEAR),
                )
            }
            else -> Log.d(TAG, "Ignored ${messageEvent.path}")
        }
    }

    private companion object {
        const val TAG = "WatchAlarmListener"
    }
}
