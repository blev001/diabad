package com.diabad.alarm

import android.util.Log
import com.diabad.core.di.ApplicationScope
import com.diabad.domain.model.AlarmAlertMode
import com.diabad.domain.model.AppSettings
import com.diabad.domain.model.GlucoseAlarmKind
import com.diabad.domain.model.GlucoseReading
import com.diabad.domain.model.GlucoseSource
import com.diabad.domain.model.TrendArrow
import com.diabad.domain.model.alarmKindFor
import com.diabad.domain.model.isOutOfAlarmRange
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
    private val strongVibrator: StrongVibrator,
    private val alarmNotificationFactory: AlarmNotificationFactory,
    private val phoneAlarmLauncher: PhoneAlarmLauncher,
    private val watchAlarmBridge: WatchAlarmBridge,
    private val connectionLossMonitor: dagger.Lazy<ConnectionLossMonitor>,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private var job: Job? = null
    private var snoozeJob: Job? = null

    private val _uiState = MutableStateFlow(HypoAlarmUiState.IDLE)
    val uiState: StateFlow<HypoAlarmUiState> = _uiState.asStateFlow()

    private val _alarmKind = MutableStateFlow<GlucoseAlarmKind?>(null)
    val alarmKind: StateFlow<GlucoseAlarmKind?> = _alarmKind.asStateFlow()

    private val _isTestAlarm = MutableStateFlow(false)
    val isTestAlarm: StateFlow<Boolean> = _isTestAlarm.asStateFlow()

    private val _ringingReading = MutableStateFlow<GlucoseReading?>(null)
    val ringingReading: StateFlow<GlucoseReading?> = _ringingReading.asStateFlow()

    @Volatile private var snoozedUntilMillis: Long = 0L
    @Volatile private var dismissedUntilRecovery: Boolean = false
    @Volatile private var testAlarmActive: Boolean = false
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
                    if (!testAlarmActive) {
                        evaluate(latest, settings)
                    }
                }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        snoozeJob?.cancel()
        silence(clearNotification = true)
        clearTestFlags()
        _uiState.value = HypoAlarmUiState.IDLE
        _alarmKind.value = null
        _ringingReading.value = null
    }

    fun dismiss() {
        snoozeJob?.cancel()
        dismissedUntilRecovery = !testAlarmActive
        snoozedUntilMillis = 0L
        silence(clearNotification = true)
        connectionLossMonitor.get().clearAlarm()
        clearTestFlags()
        _ringingReading.value = null
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
        clearTestFlags()
        _ringingReading.value = null
        _uiState.value = HypoAlarmUiState.SNOOZED
        scheduleSnoozeWake(mins)
        Log.i(TAG, "Alarm snoozed for $mins min")
    }

    fun testSound(settings: AppSettings = lastSettings) {
        when (settings.alarmAlertMode) {
            AlarmAlertMode.VIBRATION_ONLY -> strongVibrator.previewAlarm()
            AlarmAlertMode.SOUND -> alarmPlayer.preview(settings)
        }
    }

    fun previewVibration() {
        strongVibrator.previewAlarm()
    }

    /**
     * Full alarm rehearsal on phone + Galaxy Watch, even if glucose is in range.
     */
    fun startTestAlarm(
        settings: AppSettings = lastSettings,
        latest: GlucoseReading? = lastLatest,
    ) {
        lastSettings = settings
        if (latest != null) lastLatest = latest
        testAlarmActive = true
        _isTestAlarm.value = true
        dismissedUntilRecovery = false
        snoozedUntilMillis = 0L
        snoozeJob?.cancel()
        val demo = testReading(settings, latest)
        val kind = settings.alarmKindFor(demo.mmol) ?: GlucoseAlarmKind.HYPO
        start()
        ring(demo, settings, kind)
        Log.i(TAG, "Test alarm started mmol=${demo.mmol} kind=$kind")
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
        val kind = latest?.let { settings.alarmKindFor(it.mmol) }
        val outOfRange = latest != null && settings.isOutOfAlarmRange(latest.mmol)

        if (!outOfRange) {
            dismissedUntilRecovery = false
            snoozeJob?.cancel()
            snoozedUntilMillis = 0L
            _alarmKind.value = null
            _ringingReading.value = null
            if (_uiState.value != HypoAlarmUiState.IDLE || isAlerting()) {
                silence(clearNotification = true)
                _uiState.value = HypoAlarmUiState.IDLE
            }
            return
        }

        _alarmKind.value = kind

        if (dismissedUntilRecovery) return

        if (now < snoozedUntilMillis) {
            _uiState.value = HypoAlarmUiState.SNOOZED
            return
        }

        if (_uiState.value != HypoAlarmUiState.RINGING || !isAlerting()) {
            ring(latest!!, settings, kind!!)
        }
    }

    private fun isAlerting(): Boolean = alarmPlayer.isPlaying() || strongVibrator.isRunning()

    private fun ring(latest: GlucoseReading, settings: AppSettings, kind: GlucoseAlarmKind) {
        _alarmKind.value = kind
        _ringingReading.value = latest
        _uiState.value = HypoAlarmUiState.RINGING
        // Post the notification first: its channel must not drive the motor,
        // or Samsung cancels the max-amplitude alarm waveform.
        alarmNotificationFactory.showRinging(latest, settings, kind)
        when (settings.alarmAlertMode) {
            AlarmAlertMode.SOUND -> {
                strongVibrator.stop()
                alarmPlayer.start(settings, loop = true)
            }
            AlarmAlertMode.VIBRATION_ONLY -> {
                alarmPlayer.stop()
                strongVibrator.startAlarmLoop()
            }
        }
        phoneAlarmLauncher.launch(latest, settings, kind)
        val test = testAlarmActive
        scope.launch {
            val watchReached = watchAlarmBridge.ring(latest, settings, kind, test)
            if (watchReached) {
                alarmNotificationFactory.cancelWatchBridge()
            }
        }
        Log.i(TAG, "Glucose alarm ringing kind=$kind mode=${settings.alarmAlertMode} mmol=${latest.mmol} test=$test")
    }

    private fun silence(clearNotification: Boolean) {
        alarmPlayer.stop()
        strongVibrator.stop()
        phoneAlarmLauncher.cancel()
        if (clearNotification) {
            alarmNotificationFactory.cancelAll()
        }
        scope.launch { watchAlarmBridge.clear() }
    }

    private fun clearTestFlags() {
        testAlarmActive = false
        _isTestAlarm.value = false
    }

    private fun testReading(settings: AppSettings, latest: GlucoseReading?): GlucoseReading {
        val liveKind = latest?.let { settings.alarmKindFor(it.mmol) }
        if (latest != null && liveKind != null) return latest
        return GlucoseReading(
            mmol = (settings.hypoThresholdMmol - 0.7).coerceAtLeast(2.2),
            timestampMillis = System.currentTimeMillis(),
            trend = TrendArrow.SINGLE_DOWN,
            source = latest?.source ?: GlucoseSource.OTTAI,
        )
    }

    private companion object {
        const val TAG = "HypoAlarmController"
    }
}
