package com.diabad.alarm

import android.util.Log
import com.diabad.core.di.ApplicationScope
import com.diabad.domain.model.AppSettings
import com.diabad.domain.model.GlucoseReading
import com.diabad.domain.repository.GlucoseRepository
import com.diabad.domain.repository.SettingsRepository
import com.diabad.notification.AlarmNotificationFactory
import com.diabad.wear.WatchAlarmBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

enum class HypoAlarmUiState {
    IDLE,
    RINGING,
    SNOOZED,
}

@Singleton
class HypoAlarmController @Inject constructor(
    private val glucoseRepository: GlucoseRepository,
    private val settingsRepository: SettingsRepository,
    private val alarmPlayer: AlarmPlayer,
    private val alarmNotificationFactory: AlarmNotificationFactory,
    private val watchAlarmBridge: WatchAlarmBridge,
    private val connectionLossMonitor: dagger.Lazy<ConnectionLossMonitor>,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private var job: Job? = null
    private var snoozeJob: Job? = null

    private val _uiState = MutableStateFlow(HypoAlarmUiState.IDLE)
    val uiState: StateFlow<HypoAlarmUiState> = _uiState.asStateFlow()

    @Volatile private var snoozedUntilMillis: Long = 0L
    @Volatile private var dismissedUntilRecovery: Boolean = false
    @Volatile private var lastSettings: AppSettings = AppSettings()
    @Volatile private var lastLatest: GlucoseReading? = null

    fun start() {
        if (job?.isActive == true) return
        alarmNotificationFactory.ensureChannel()
        job = scope.launch {
            combine(
                glucoseRepository.observeLatest(),
                settingsRepository.observe(),
            ) { latest, settings -> latest to settings }
                .collect { (latest, settings) ->
                    lastLatest = latest
                    lastSettings = settings
                    evaluate(latest, settings)
                }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        snoozeJob?.cancel()
        silence(clearNotification = true)
        _uiState.value = HypoAlarmUiState.IDLE
    }

    fun dismiss() {
        snoozeJob?.cancel()
        dismissedUntilRecovery = true
        snoozedUntilMillis = 0L
        silence(clearNotification = true)
        connectionLossMonitor.get().clearAlarm()
        _uiState.value = HypoAlarmUiState.IDLE
        Log.i(TAG, "Alarm dismissed until glucose recovers")
    }

    fun snooze(minutes: Int = lastSettings.snoozeMinutes) {
        val mins = minutes.coerceIn(1, 120)
        snoozedUntilMillis = System.currentTimeMillis() + mins * 60_000L
        dismissedUntilRecovery = false
        silence(clearNotification = false)
        connectionLossMonitor.get().clearAlarm()
        alarmNotificationFactory.showSnoozed(mins)
        _uiState.value = HypoAlarmUiState.SNOOZED
        scheduleSnoozeWake(mins)
        Log.i(TAG, "Alarm snoozed for $mins min")
    }

    fun testSound(settings: AppSettings = lastSettings) {
        alarmPlayer.preview(settings)
    }

    private fun scheduleSnoozeWake(minutes: Int) {
        snoozeJob?.cancel()
        snoozeJob = scope.launch {
            delay(minutes * 60_000L)
            snoozedUntilMillis = 0L
            evaluate(lastLatest, lastSettings)
        }
    }

    private fun evaluate(latest: GlucoseReading?, settings: AppSettings) {
        val now = System.currentTimeMillis()
        val low = latest != null && latest.mmol < settings.hypoThresholdMmol

        if (!low) {
            dismissedUntilRecovery = false
            snoozeJob?.cancel()
            snoozedUntilMillis = 0L
            if (_uiState.value != HypoAlarmUiState.IDLE || alarmPlayer.isPlaying()) {
                silence(clearNotification = true)
                _uiState.value = HypoAlarmUiState.IDLE
            }
            return
        }

        if (dismissedUntilRecovery) return

        if (now < snoozedUntilMillis) {
            _uiState.value = HypoAlarmUiState.SNOOZED
            return
        }

        if (_uiState.value != HypoAlarmUiState.RINGING || !alarmPlayer.isPlaying()) {
            ring(latest!!, settings)
        }
    }

    private fun ring(latest: GlucoseReading, settings: AppSettings) {
        alarmPlayer.start(settings, loop = true)
        alarmNotificationFactory.showRinging(latest, settings)
        scope.launch { watchAlarmBridge.ring(latest, settings) }
        _uiState.value = HypoAlarmUiState.RINGING
        Log.i(TAG, "Hypo alarm ringing mmol=${latest.mmol}")
    }

    private fun silence(clearNotification: Boolean) {
        alarmPlayer.stop()
        if (clearNotification) {
            alarmNotificationFactory.cancelAll()
        }
        scope.launch { watchAlarmBridge.clear() }
    }

    private companion object {
        const val TAG = "HypoAlarmController"
    }
}
