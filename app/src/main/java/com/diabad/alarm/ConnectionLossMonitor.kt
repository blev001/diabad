package com.diabad.alarm

import android.util.Log
import com.diabad.core.di.ApplicationScope
import com.diabad.domain.model.AppSettings
import com.diabad.domain.model.ConnectionLossMode
import com.diabad.domain.model.GlucoseReading
import com.diabad.domain.repository.GlucoseRepository
import com.diabad.domain.repository.SettingsRepository
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
    private val hypoAlarmController: HypoAlarmController,
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
                delay(30_000L)
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
            alarmPlayer.stop()
            connectionAlarmActive = false
        }
    }

    /** Called when user dismisses / snoozes from the hypo alarm UI. */
    fun clearAlarm() {
        if (connectionAlarmActive) {
            alarmPlayer.stop()
            connectionAlarmActive = false
        }
        lastRemindAt = System.currentTimeMillis()
    }

    private fun evaluate(latest: GlucoseReading?, settings: AppSettings) {
        if (hypoAlarmController.uiState.value == HypoAlarmUiState.RINGING) return

        if (latest == null) {
            stopConnectionAlarmIfNeeded()
            return
        }

        val ageMin = TimeUnit.MILLISECONDS.toMinutes(
            System.currentTimeMillis() - latest.timestampMillis,
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
                    alarmPlayer.preview(settings)
                    Log.i(TAG, "Connection-loss reminder after ${ageMin}m")
                }
            }
            ConnectionLossMode.ALARM -> {
                if (hypoAlarmController.uiState.value == HypoAlarmUiState.SNOOZED) return
                if (!connectionAlarmActive) {
                    alarmPlayer.start(settings, loop = true)
                    connectionAlarmActive = true
                    Log.i(TAG, "Connection-loss alarm after ${ageMin}m")
                }
            }
        }
    }

    private fun stopConnectionAlarmIfNeeded() {
        if (connectionAlarmActive) {
            alarmPlayer.stop()
            connectionAlarmActive = false
        }
    }

    private companion object {
        const val TAG = "ConnectionLossMonitor"
        const val REMIND_COOLDOWN_MS = 15 * 60 * 1000L
    }
}
