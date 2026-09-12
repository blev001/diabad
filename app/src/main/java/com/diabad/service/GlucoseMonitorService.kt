package com.diabad.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import com.diabad.alarm.ConnectionLossMonitor
import com.diabad.alarm.HypoAlarmController
import com.diabad.core.di.ApplicationScope
import com.diabad.domain.model.GlucoseReading
import com.diabad.domain.repository.GlucoseRepository
import com.diabad.notification.GlucoseNotificationFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GlucoseMonitorService : Service() {

    @Inject lateinit var glucoseRepository: GlucoseRepository
    @Inject lateinit var notificationFactory: GlucoseNotificationFactory
    @Inject lateinit var hypoAlarmController: HypoAlarmController
    @Inject lateinit var connectionLossMonitor: ConnectionLossMonitor
    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    private var observeJob: Job? = null
    private var refreshJob: Job? = null

    @Volatile private var lastLatest: GlucoseReading? = null
    @Volatile private var lastPrevious: GlucoseReading? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationFactory.ensureChannel()
        val initial = notificationFactory.build(latest = null, previous = null)
        ServiceCompat.startForeground(
            this,
            GlucoseNotificationFactory.NOTIFICATION_ID,
            initial,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
        startObserving()
        startPeriodicRefresh()
        hypoAlarmController.start()
        connectionLossMonitor.start()
        Log.i(TAG, "Glucose monitor started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        observeJob?.cancel()
        refreshJob?.cancel()
        hypoAlarmController.stop()
        connectionLossMonitor.stop()
        super.onDestroy()
    }

    private fun startObserving() {
        observeJob?.cancel()
        observeJob = applicationScope.launch {
            combine(
                glucoseRepository.observeLatest(),
                glucoseRepository.observeHistory(),
            ) { latest, history ->
                latest to previousOf(latest, history)
            }.collect { (latest, previous) ->
                updateNotification(latest, previous)
            }
        }
    }

    /** Keep "updated N min ago" fresh even when glucose value is unchanged. */
    private fun startPeriodicRefresh() {
        refreshJob?.cancel()
        refreshJob = applicationScope.launch {
            while (isActive) {
                delay(60_000L)
                updateNotification(lastLatest, lastPrevious)
            }
        }
    }

    private fun updateNotification(latest: GlucoseReading?, previous: GlucoseReading?) {
        lastLatest = latest
        lastPrevious = previous
        val notification = notificationFactory.build(latest, previous)
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(GlucoseNotificationFactory.NOTIFICATION_ID, notification)
    }

    private fun previousOf(
        latest: GlucoseReading?,
        history: List<GlucoseReading>,
    ): GlucoseReading? {
        if (latest == null || history.size < 2) return null
        val index = history.indexOfLast { it.timestampMillis == latest.timestampMillis }
        return when {
            index > 0 -> history[index - 1]
            else -> history.getOrNull(history.lastIndex - 1)
        }
    }

    companion object {
        private const val TAG = "GlucoseMonitorService"

        fun start(context: Context) {
            val intent = Intent(context, GlucoseMonitorService::class.java)
            context.startForegroundService(intent)
        }
    }
}
