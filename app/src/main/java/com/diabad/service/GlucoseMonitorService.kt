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
import com.diabad.domain.model.AppSettings
import com.diabad.domain.model.GlucoseReading
import com.diabad.domain.model.GlucoseZone
import com.diabad.domain.model.previousReading
import com.diabad.domain.repository.GlucoseRepository
import com.diabad.domain.repository.SettingsRepository
import com.diabad.notification.GlucoseNotificationFactory
import com.diabad.wear.WatchGlucoseSync
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
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var notificationFactory: GlucoseNotificationFactory
    @Inject lateinit var hypoAlarmController: HypoAlarmController
    @Inject lateinit var connectionLossMonitor: ConnectionLossMonitor
    @Inject lateinit var watchGlucoseSync: WatchGlucoseSync
    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    private var observeJob: Job? = null
    private var animationJob: Job? = null

    @Volatile private var lastLatest: GlucoseReading? = null
    @Volatile private var lastPrevious: GlucoseReading? = null
    @Volatile private var lastSettings: AppSettings = AppSettings()
    @Volatile private var lastAlarming: Boolean = false

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
        startKolobokAnimation()
        hypoAlarmController.start()
        connectionLossMonitor.start()
        Log.i(TAG, "Glucose monitor started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        observeJob?.cancel()
        animationJob?.cancel()
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
                settingsRepository.observe(),
                hypoAlarmController.uiState,
            ) { latest, history, settings, alarmState ->
                ObserveSnapshot(latest, previousReading(latest, history), settings, alarmState)
            }.collect { snap ->
                lastLatest = snap.latest
                lastPrevious = snap.previous
                lastSettings = snap.settings
                lastAlarming = snap.alarmState == HypoAlarmUiState.RINGING
                updateNotification()
                watchGlucoseSync.push(
                    latest = snap.latest,
                    previous = snap.previous,
                    settings = snap.settings,
                    alarming = lastAlarming,
                )
            }
        }
    }

    /** Redraw the shade Kolobok so its face keeps blinking / shaking. */
    private fun startKolobokAnimation() {
        animationJob?.cancel()
        animationJob = applicationScope.launch {
            while (isActive) {
                val zone = GlucoseZone.classify(
                    lastLatest?.mmol,
                    lastSettings.hypoThresholdMmol,
                    lastSettings.hyperThresholdMmol,
                )
                delay(notificationFactory.animationIntervalMs(zone, lastAlarming))
                updateNotification()
            }
        }
    }

    private fun updateNotification() {
        val notification = notificationFactory.build(
            latest = lastLatest,
            previous = lastPrevious,
            settings = lastSettings,
            alarming = lastAlarming,
        )
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(GlucoseNotificationFactory.NOTIFICATION_ID, notification)
    }

    private data class ObserveSnapshot(
        val latest: GlucoseReading?,
        val previous: GlucoseReading?,
        val settings: AppSettings,
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
