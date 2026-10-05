package com.diabad.alarm

import android.util.Log
import com.diabad.core.alarm.AlarmSnoozeSlots
import com.diabad.core.di.ApplicationScope
import com.diabad.domain.alarm.AlarmCondition
import com.diabad.domain.alarm.AlarmOutput
import com.diabad.domain.alarm.AlarmPolicy
import com.diabad.domain.alarm.AlarmStatus
import com.diabad.domain.model.AlarmAlertMode
import com.diabad.domain.model.AlarmReason
import com.diabad.domain.model.AlarmState
import com.diabad.domain.model.AppSettings
import com.diabad.domain.model.GlucoseAlarmKind
import com.diabad.domain.model.GlucoseReading
import com.diabad.domain.model.GlucoseSource
import com.diabad.domain.model.TrendArrow
import com.diabad.domain.model.alarmKindFor
import com.diabad.domain.repository.AlarmStateRepository
import com.diabad.domain.repository.GlucoseRepository
import com.diabad.domain.repository.SettingsRepository
import com.diabad.notification.AlarmNotificationFactory
import com.diabad.wear.WatchAlarmBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalTime
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
    private val alarmStateRepository: AlarmStateRepository,
    private val wakeScheduler: AlarmWakeScheduler,
    private val alarmPlayer: AlarmPlayer,
    private val strongVibrator: StrongVibrator,
    private val alarmNotificationFactory: AlarmNotificationFactory,
    private val phoneAlarmLauncher: PhoneAlarmLauncher,
    private val watchAlarmBridge: WatchAlarmBridge,
    private val connectionLossMonitor: dagger.Lazy<ConnectionLossMonitor>,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val mutex = Mutex()
    private var job: Job? = null

    private val _uiState = MutableStateFlow(HypoAlarmUiState.IDLE)
    val uiState: StateFlow<HypoAlarmUiState> = _uiState.asStateFlow()

    private val _alarmKind = MutableStateFlow<GlucoseAlarmKind?>(null)
    val alarmKind: StateFlow<GlucoseAlarmKind?> = _alarmKind.asStateFlow()

    private val _alarmReason = MutableStateFlow<AlarmReason?>(null)
    val alarmReason: StateFlow<AlarmReason?> = _alarmReason.asStateFlow()

    private val _ringingCondition = MutableStateFlow<AlarmCondition?>(null)
    val ringingCondition: StateFlow<AlarmCondition?> = _ringingCondition.asStateFlow()

    private val _isTestAlarm = MutableStateFlow(false)
    val isTestAlarm: StateFlow<Boolean> = _isTestAlarm.asStateFlow()

    private val _ringingReading = MutableStateFlow<GlucoseReading?>(null)
    val ringingReading: StateFlow<GlucoseReading?> = _ringingReading.asStateFlow()

    private val _snoozedUntilMillis = MutableStateFlow(0L)
    val snoozedUntilMillis: StateFlow<Long> = _snoozedUntilMillis.asStateFlow()

    @Volatile private var snoozedUntilDeadline: Long = 0L
    @Volatile private var testAlarmActive: Boolean = false
    @Volatile private var lastSettings: AppSettings = AppSettings()
    @Volatile private var lastLatest: GlucoseReading? = null
    @Volatile private var lastReadings: List<GlucoseReading> = emptyList()
    @Volatile private var lastReason: AlarmReason? = null
    private var appliedOutput: AlarmOutput = AlarmOutput.NONE

    fun start() {
        if (job?.isActive == true) return
        alarmNotificationFactory.ensureChannel()
        job = scope.launch {
            combine(
                glucoseRepository.observeRecent(RECENT_LIMIT),
                settingsRepository.observe(),
            ) { readings, settings -> readings to settings }
                .collect { (readings, settings) ->
                    lastReadings = readings
                    lastLatest = readings.lastOrNull()
                    lastSettings = settings
                    if (!testAlarmActive) {
                        mutex.withLock { evaluate(readings, settings) }
                    }
                }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        wakeScheduler.cancel()
        silence(clearNotification = true)
        clearTestFlags()
        _uiState.value = HypoAlarmUiState.IDLE
        _alarmKind.value = null
        _alarmReason.value = null
        _ringingCondition.value = null
        _ringingReading.value = null
    }

    fun dismiss() {
        scope.launch {
            mutex.withLock {
                if (!testAlarmActive) {
                    val now = System.currentTimeMillis()
                    val state = AlarmPolicy.dismiss(
                        alarmStateRepository.get(),
                        now,
                        lastReason,
                        lastLatest?.mmol,
                    )
                    alarmStateRepository.save(state)
                    settingsRepository.setAlarmSnoozedUntilMillis(0L)
                }
                setSnoozeDeadline(0L)
                silence(clearNotification = true)
                connectionLossMonitor.get().clearAlarm()
                clearTestFlags()
                _ringingReading.value = null
                _uiState.value = HypoAlarmUiState.IDLE
                Log.i(TAG, "Alarm stopped")
            }
        }
    }

    fun snooze(minutes: Int = lastSettings.snoozeMinutes) {
        val mins = AlarmSnoozeSlots.normalize(minutes)
        scope.launch {
            mutex.withLock {
                val now = System.currentTimeMillis()
                val state = AlarmPolicy.snooze(alarmStateRepository.get(), now, mins, lastReason)
                alarmStateRepository.save(state)
                settingsRepository.setSnoozeMinutes(mins)
                settingsRepository.setAlarmSnoozedUntilMillis(state.snoozedUntilMillis)
                setSnoozeDeadline(state.snoozedUntilMillis)
                silence(clearNotification = false)
                connectionLossMonitor.get().clearAlarm()
                alarmNotificationFactory.showSnoozed(mins)
                clearTestFlags()
                _ringingReading.value = null
                _uiState.value = HypoAlarmUiState.SNOOZED
                wakeScheduler.schedule(state.snoozedUntilMillis)
                Log.i(TAG, "Alarm snoozed for $mins min until ${state.snoozedUntilMillis}")
            }
        }
    }

    /** Re-check after an AlarmManager wake, even if the process was restarted. */
    fun evaluateNow(onDone: () -> Unit = {}) {
        scope.launch {
            try {
                mutex.withLock {
                    if (lastReadings.isEmpty()) {
                        lastReadings = glucoseRepository.observeRecent(RECENT_LIMIT).first()
                        lastSettings = settingsRepository.observe().first()
                        lastLatest = lastReadings.lastOrNull()
                    }
                    if (!testAlarmActive) evaluate(lastReadings, lastSettings)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "evaluateNow failed", t)
            } finally {
                onDone()
            }
        }
    }

    fun isSnoozeActive(nowMillis: Long = System.currentTimeMillis()): Boolean =
        AlarmSnoozeSlots.isActive(snoozedUntilDeadline, nowMillis)

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
        setSnoozeDeadline(0L)
        scope.launch { settingsRepository.setAlarmSnoozedUntilMillis(0L) }
        val demo = testReading(settings, latest)
        val kind = settings.alarmKindFor(demo.mmol) ?: GlucoseAlarmKind.HYPO
        val reason = if (kind == GlucoseAlarmKind.HYPER) AlarmReason.HIGH else AlarmReason.LOW
        start()
        ring(demo, settings, kind, reason = reason)
        Log.i(TAG, "Test alarm started mmol=${demo.mmol} kind=$kind reason=$reason")
    }

    private fun setSnoozeDeadline(untilMillis: Long) {
        snoozedUntilDeadline = untilMillis
        _snoozedUntilMillis.value = untilMillis
    }

    private suspend fun evaluate(readings: List<GlucoseReading>, settings: AppSettings) {
        val now = System.currentTimeMillis()
        val minute = LocalTime.now().let { it.hour * 60 + it.minute }
        var state = alarmStateRepository.get()
        if (settings.alarmSnoozedUntilMillis > now && state.snoozedUntilMillis < settings.alarmSnoozedUntilMillis) {
            state = state.copy(
                snoozedUntilMillis = settings.alarmSnoozedUntilMillis,
                snoozedReason = state.snoozedReason ?: AlarmReason.LOW,
            )
        }
        val decision = AlarmPolicy.evaluate(now, minute, readings, settings, state)
        if (decision.state != state) alarmStateRepository.save(decision.state)
        if (decision.state.snoozedUntilMillis != settings.alarmSnoozedUntilMillis) {
            settingsRepository.setAlarmSnoozedUntilMillis(decision.state.snoozedUntilMillis)
        }
        wakeScheduler.schedule(decision.nextWakeMillis)

        val condition = decision.condition
        lastReason = condition?.reason
        val signalLoss = condition?.reason == AlarmReason.SIGNAL_LOSS
        if (signalLoss || decision.status != AlarmStatus.RINGING) {
            when (decision.status) {
                AlarmStatus.SNOOZED -> showSnoozed(decision.state, now)
                AlarmStatus.RINGING -> {
                    // ConnectionLossMonitor plays the no-data alert and stays quiet while we ring.
                    if (_uiState.value == HypoAlarmUiState.RINGING || isAlerting()) {
                        silence(clearNotification = true)
                    }
                    _uiState.value = HypoAlarmUiState.IDLE
                    _alarmKind.value = null
                    _alarmReason.value = null
                    _ringingCondition.value = null
                    _ringingReading.value = null
                }
                AlarmStatus.IDLE, AlarmStatus.DISMISSED -> {
                    if (_uiState.value != HypoAlarmUiState.IDLE || isAlerting()) {
                        silence(clearNotification = true)
                    }
                    setSnoozeDeadline(0L)
                    _uiState.value = HypoAlarmUiState.IDLE
                    _alarmKind.value = null
                    _alarmReason.value = null
                    _ringingCondition.value = null
                    _ringingReading.value = null
                }
            }
            return
        }

        val latest = condition?.latest ?: return
        val reason = condition.reason
        val kind = reason.toGlucoseAlarmKind()
        val output = effectiveOutput(settings, decision.output)
        val reasonChanged = _alarmReason.value != reason
        if (_uiState.value != HypoAlarmUiState.RINGING || reasonChanged || !isAlerting()) {
            ring(latest, settings, kind, output, reason, condition)
        } else if (output != appliedOutput) {
            _ringingCondition.value = condition
            applyOutput(settings, output)
        } else {
            _ringingCondition.value = condition
        }
    }

    private fun showSnoozed(state: AlarmState, now: Long) {
        val wasRinging = _uiState.value == HypoAlarmUiState.RINGING || isAlerting()
        if (wasRinging) silence(clearNotification = false)
        setSnoozeDeadline(state.snoozedUntilMillis)
        if (wasRinging || _uiState.value != HypoAlarmUiState.SNOOZED) {
            val mins = AlarmSnoozeSlots.remainingMinutes(state.snoozedUntilMillis, now)
            if (mins > 0) alarmNotificationFactory.showSnoozed(mins)
        }
        _uiState.value = HypoAlarmUiState.SNOOZED
        _alarmKind.value = null
        _alarmReason.value = null
        _ringingCondition.value = null
        _ringingReading.value = null
    }

    private fun effectiveOutput(settings: AppSettings, output: AlarmOutput): AlarmOutput =
        if (settings.alarmAlertMode == AlarmAlertMode.VIBRATION_ONLY && output != AlarmOutput.NONE) {
            AlarmOutput.VIBRATE
        } else {
            output
        }

    private fun isAlerting(): Boolean = alarmPlayer.isPlaying() || strongVibrator.isRunning()

    private fun ring(
        latest: GlucoseReading,
        settings: AppSettings,
        kind: GlucoseAlarmKind,
        output: AlarmOutput = AlarmOutput.SOUND,
        reason: AlarmReason = AlarmReason.LOW,
        condition: AlarmCondition? = null,
    ) {
        _alarmKind.value = kind
        _alarmReason.value = reason
        _ringingCondition.value = condition
        _ringingReading.value = latest
        _uiState.value = HypoAlarmUiState.RINGING
        // Post the notification first: its channel must not drive the motor,
        // or Samsung cancels the max-amplitude alarm waveform.
        alarmNotificationFactory.showRinging(latest, settings, reason, condition)
        applyOutput(settings, effectiveOutput(settings, output))
        phoneAlarmLauncher.launch(latest, settings, kind, reason)
        val test = testAlarmActive
        scope.launch {
            val watchReached = watchAlarmBridge.ring(latest, settings, reason, test)
            if (watchReached) {
                alarmNotificationFactory.cancelWatchBridge()
            }
        }
        Log.i(TAG, "Glucose alarm ringing reason=$reason kind=$kind mode=${settings.alarmAlertMode} mmol=${latest.mmol} test=$test")
    }

    private fun applyOutput(settings: AppSettings, output: AlarmOutput) {
        when (output) {
            AlarmOutput.SOUND -> {
                strongVibrator.stop()
                if (!alarmPlayer.isPlaying()) alarmPlayer.start(settings, loop = true)
            }
            AlarmOutput.VIBRATE -> {
                alarmPlayer.stop()
                if (!strongVibrator.isRunning()) strongVibrator.startAlarmLoop()
            }
            AlarmOutput.NONE -> {
                alarmPlayer.stop()
                strongVibrator.stop()
            }
        }
        appliedOutput = output
    }

    private fun silence(clearNotification: Boolean) {
        alarmPlayer.stop()
        strongVibrator.stop()
        appliedOutput = AlarmOutput.NONE
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
        const val RECENT_LIMIT = 240
    }
}
