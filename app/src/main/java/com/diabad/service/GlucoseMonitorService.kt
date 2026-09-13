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
import com.diabad.alarm.HypoAlarmUiState
import com.diabad.core.di.ApplicationScope
import com.diabad.domain.model.GlucoseReading
import com.diabad.domain.repository.GlucoseRepository
import com.diabad.domain.repository.SettingsRepository
import com.diabad.notification.GlucoseNotificationFactory
import com.diabad.wear.WatchGlucoseSync
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GlucoseMonitorService : Service() {

    @Inject lateinit var glucoseRepository: GlucoseRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var notificationFactory: GlucoseNotificationFactory
    @Inject lateinit var hypoAlarmController: HypoAlarmController
    @Inject lateinit var connectionLossMonitor: ConnectionLossMonitor
    @Inject lateinit var watchGlucoseSync: WatchGlucoseSync
    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    private var observeJob: Job? = null

    @Volatile private var lastNotificationKey: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationFactory.ensureChannel()
        val initial = notificationFactory.build(
            latest = null,
            previous = null,
            foregroundImmediate = true,
        )
        ServiceCompat.startForeground(
            this,
            GlucoseNotificationFactory.NOTIFICATION_ID,
            initial,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
        lastNotificationKey = notificationFactory.contentKey(null, null)
        startObserving()
        hypoAlarmController.start()
        connectionLossMonitor.start()
        Log.i(TAG, "Glucose monitor started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        observeJob?.cancel()
        hypoAlarmController.stop()
        connectionLossMonitor.stop()
        super.onDestroy()
    }

    private fun startObserving() {
        observeJob?.cancel()
        observeJob = applicationScope.launch {
            combine(
                glucoseRepository.observeLatest(),
                glucoseRepository.observePrevious(),
                settingsRepository.observe(),
                hypoAlarmController.uiState,
            ) { latest, previous, settings, alarmState ->
                ObserveSnapshot(latest, previous, settings, alarmState)
            }
                .distinctUntilChanged()
                .collect { snap ->
                    updateNotification(snap.latest, snap.previous)
                    watchGlucoseSync.push(
                        latest = snap.latest,
                        previous = snap.previous,
                        settings = snap.settings,
                        alarming = snap.alarmState == HypoAlarmUiState.RINGING,
                    )
                }
        }
    }

    private fun updateNotification(latest: GlucoseReading?, previous: GlucoseReading?) {
        val key = notificationFactory.contentKey(latest, previous)
        if (key == lastNotificationKey) return
        lastNotificationKey = key
        val notification = notificationFactory.build(latest, previous)
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(GlucoseNotificationFactory.NOTIFICATION_ID, notification)
    }

    private data class ObserveSnapshot(
        val latest: GlucoseReading?,
        val previous: GlucoseReading?,
        val settings: com.diabad.domain.model.AppSettings,
        val alarmState: HypoAlarmUiState,
    )

    companion object {
        private const val TAG = "GlucoseMonitorService"

        fun start(context: Context) {
            val intent = Intent(context, GlucoseMonitorService::class.java)
            context.startForegroundService(intent)
        }
    }
}
