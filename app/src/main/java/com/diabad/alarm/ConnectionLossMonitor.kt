package com.diabad.alarm

import android.util.Log
import com.diabad.core.di.ApplicationScope
import com.diabad.domain.model.AlarmAlertMode
import com.diabad.domain.model.AppSettings
import com.diabad.domain.model.ConnectionLossMode
import com.diabad.domain.model.GlucoseReading
import com.diabad.domain.repository.GlucoseRepository
import com.diabad.domain.repository.SettingsRepository
import com.diabad.domain.signal.GlucoseSignalClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Applies the user's "no signal" preference when OtTai stops sending readings.
 */
@Singleton
class ConnectionLossMonitor @Inject constructor(
    private val glucoseRepository: GlucoseRepository,
    private val settingsRepository: SettingsRepository,
    private val alarmPlayer: AlarmPlayer,
    private val strongVibrator: StrongVibrator,
    private val hypoAlarmController: HypoAlarmController,
    private val signalClock: GlucoseSignalClock,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private var observeJob: Job? = null
    private var tickJob: Job? = null

    @Volatile private var lastLatest: GlucoseReading? = null
    @Volatile private var lastSettings: AppSettings = AppSettings()
    @Volatile private var lastRemindAt: Long = 0L
    @Volatile private var connectionAlarmActive: Boolean = false

    fun start() {
        if (observeJob?.isActive == true) return
        observeJob = scope.launch {
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
        tickJob = scope.launch {
            while (isActive) {
                delay(nextCheckDelayMs(lastLatest, lastSettings))
                evaluate(lastLatest, lastSettings)
            }
        }
    }

    fun stop() {
        observeJob?.cancel()
        tickJob?.cancel()
        observeJob = null
        tickJob = null
        if (connectionAlarmActive) {
            stopConnectionOutputs()
            connectionAlarmActive = false
        }
    }

    /** Called when user dismisses / snoozes from the hypo alarm UI. */
    fun clearAlarm() {
        if (connectionAlarmActive) {
            stopConnectionOutputs()
            connectionAlarmActive = false
        }
        lastRemindAt = System.currentTimeMillis()
    }

    private fun nextCheckDelayMs(latest: GlucoseReading?, settings: AppSettings): Long {
        if (settings.connectionLossMode == ConnectionLossMode.SILENT) {
            return SILENT_IDLE_MS
        }
        val lastSeen = lastSeenMillis(latest)
        if (lastSeen == 0L) return SILENT_IDLE_MS
        val graceMs = settings.connectionLossGraceMinutes * 60_000L
        val dueIn = lastSeen + graceMs - System.currentTimeMillis()
        return if (dueIn > 0L) {
            dueIn.coerceAtLeast(1_000L)
        } else if (settings.connectionLossMode == ConnectionLossMode.REMIND) {
            REMIND_COOLDOWN_MS
        } else {
            OVERDUE_RETRY_MS
        }
    }

    private fun evaluate(latest: GlucoseReading?, settings: AppSettings) {
        if (hypoAlarmController.uiState.value == HypoAlarmUiState.RINGING) return

        val lastSeen = lastSeenMillis(latest)
        if (lastSeen == 0L) {
            stopConnectionAlarmIfNeeded()
            return
        }

        val ageMin = TimeUnit.MILLISECONDS.toMinutes(
            System.currentTimeMillis() - lastSeen,
        )
        if (ageMin < settings.connectionLossGraceMinutes) {
            stopConnectionAlarmIfNeeded()
            return
        }

        when (settings.connectionLossMode) {
            ConnectionLossMode.SILENT -> stopConnectionAlarmIfNeeded()
            ConnectionLossMode.REMIND -> {
                stopConnectionAlarmIfNeeded()
                val now = System.currentTimeMillis()
                if (now - lastRemindAt > REMIND_COOLDOWN_MS) {
                    lastRemindAt = now
                    when (settings.alarmAlertMode) {
                        AlarmAlertMode.VIBRATION_ONLY -> strongVibrator.pulseWarning()
                        AlarmAlertMode.SOUND -> alarmPlayer.preview(settings)
                    }
                    Log.i(TAG, "Connection-loss reminder after ${ageMin}m")
                }
            }
            ConnectionLossMode.ALARM -> {
                if (hypoAlarmController.uiState.value == HypoAlarmUiState.SNOOZED) return
                if (!connectionAlarmActive) {
                    when (settings.alarmAlertMode) {
                        AlarmAlertMode.VIBRATION_ONLY -> strongVibrator.startAlarmLoop()
                        AlarmAlertMode.SOUND -> alarmPlayer.start(settings, loop = true)
                    }
                    connectionAlarmActive = true
                    Log.i(TAG, "Connection-loss alarm after ${ageMin}m")
                }
            }
        }
    }

    private fun lastSeenMillis(latest: GlucoseReading?): Long {
        val marked = signalClock.lastSignalMillis()
        val reading = latest?.timestampMillis ?: 0L
        return maxOf(marked, reading)
    }

    private fun stopConnectionAlarmIfNeeded() {
        if (connectionAlarmActive) {
            stopConnectionOutputs()
            connectionAlarmActive = false
        }
    }

    private fun stopConnectionOutputs() {
        alarmPlayer.stop()
        strongVibrator.stop()
    }

    private companion object {
        const val TAG = "ConnectionLossMonitor"
        const val REMIND_COOLDOWN_MS = 15 * 60 * 1000L
        const val SILENT_IDLE_MS = 15 * 60 * 1000L
        const val OVERDUE_RETRY_MS = 60_000L
    }
}
