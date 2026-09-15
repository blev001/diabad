package com.diabad.alarm

import android.util.Log
import com.diabad.core.di.ApplicationScope
import com.diabad.domain.model.AppSettings
import com.diabad.domain.model.GlucoseReading
import com.diabad.domain.model.isApproachingHypo
import com.diabad.domain.repository.GlucoseRepository
import com.diabad.domain.repository.SettingsRepository
import com.diabad.notification.ApproachingHypoNotificationFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Push + one strong vibration when glucose enters the approaching-hypo band.
 * Does not start the full alarm UI / ringtone / watch Clock screen.
 */
@Singleton
class ApproachingHypoMonitor @Inject constructor(
    private val glucoseRepository: GlucoseRepository,
    private val settingsRepository: SettingsRepository,
    private val notificationFactory: ApproachingHypoNotificationFactory,
    private val strongVibrator: StrongVibrator,
    private val hypoAlarmController: HypoAlarmController,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private var job: Job? = null

    @Volatile
    private var warnedForCurrentEpisode: Boolean = false

    fun start() {
        if (job?.isActive == true) return
        notificationFactory.ensureChannel()
        job = scope.launch {
            combine(
                glucoseRepository.observeLatest(),
                settingsRepository.observe(),
                hypoAlarmController.uiState,
            ) { latest, settings, alarmState ->
                Triple(latest, settings, alarmState)
            }.collect { (latest, settings, alarmState) ->
                evaluate(latest, settings, alarmState)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        clearWarning()
    }

    private fun evaluate(
        latest: GlucoseReading?,
        settings: AppSettings,
        alarmState: HypoAlarmUiState,
    ) {
        if (alarmState == HypoAlarmUiState.RINGING || hypoAlarmController.isSnoozeActive()) {
            clearWarning()
            return
        }
        val approaching = latest != null && settings.isApproachingHypo(latest.mmol)
        if (!approaching) {
            clearWarning()
            return
        }
        if (warnedForCurrentEpisode) return
        warnedForCurrentEpisode = true
        notificationFactory.show(latest!!, settings)
        if (alarmState != HypoAlarmUiState.RINGING && !strongVibrator.isRunning()) {
            strongVibrator.pulseWarning()
        }
        Log.i(TAG, "Approaching hypo warning mmol=${latest.mmol}")
    }

    private fun clearWarning() {
        if (warnedForCurrentEpisode) {
            notificationFactory.cancel()
        }
        warnedForCurrentEpisode = false
    }

    private companion object {
        const val TAG = "ApproachingHypo"
    }
}
