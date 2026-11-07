package com.diabad.car

import android.app.Service
import android.content.Intent
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Both read and reply acknowledge only this card; they never change glucose alarms. */
@AndroidEntryPoint
class CarNotificationActionService : Service() {
    @Inject lateinit var notifications: CarGlucoseNotifications

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_READ || intent?.action == ACTION_REPLY) {
            notifications.dismiss(intent.getLongExtra(EXTRA_SAMPLE, -1L))
        }
        stopSelf(startId)
        return START_NOT_STICKY
    }

    companion object {
        const val ACTION_READ = "com.diabad.car.READ"
        const val ACTION_REPLY = "com.diabad.car.REPLY"
        const val EXTRA_SAMPLE = "sample_timestamp"
    }
}
