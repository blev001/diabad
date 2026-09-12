package com.diabad.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.diabad.service.GlucoseMonitorService

/**
 * Restarts monitoring after device reboot (user chose auto-start).
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED &&
            intent?.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            return
        }
        Log.i(TAG, "Boot completed — starting glucose monitor")
        GlucoseMonitorService.start(context.applicationContext)
    }

    private companion object {
        const val TAG = "BootCompletedReceiver"
    }
}
