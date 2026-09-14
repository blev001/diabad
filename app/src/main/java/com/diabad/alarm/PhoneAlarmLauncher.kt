package com.diabad.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.diabad.domain.model.AppSettings
import com.diabad.domain.model.GlucoseAlarmKind
import com.diabad.domain.model.GlucoseReading
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Opens [PhoneAlarmActivity] the same way Clock does: full-screen intent,
 * AlarmClock so the screen wakes, and a best-effort direct start from the FGS.
 */
@Singleton
class PhoneAlarmLauncher @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun fullScreenPendingIntent(
        latest: GlucoseReading,
        settings: AppSettings,
        kind: GlucoseAlarmKind,
    ): PendingIntent =
        PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            activityIntent(latest, settings, kind),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun launch(latest: GlucoseReading, settings: AppSettings, kind: GlucoseAlarmKind) {
        val intent = activityIntent(latest, settings, kind)
        val fullScreen = fullScreenPendingIntent(latest, settings, kind)
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        try {
            alarmManager?.setAlarmClock(
                AlarmManager.AlarmClockInfo(
                    System.currentTimeMillis() + WAKE_DELAY_MS,
                    fullScreen,
                ),
                fullScreen,
            )
        } catch (t: Throwable) {
            Log.w(TAG, "setAlarmClock failed: ${t.message}")
        }
        try {
            context.startActivity(intent)
        } catch (t: Throwable) {
            Log.w(TAG, "Could not open phone alarm UI directly: ${t.message}")
        }
    }

    fun cancel() {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val cancelPi = PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            PhoneAlarmActivity.createIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(cancelPi)
    }

    private fun activityIntent(
        latest: GlucoseReading,
        settings: AppSettings,
        kind: GlucoseAlarmKind,
    ): Intent {
        val threshold = when (kind) {
            GlucoseAlarmKind.HYPO -> settings.hypoThresholdMmol
            GlucoseAlarmKind.HYPER -> settings.hyperThresholdMmol
        }
        return PhoneAlarmActivity.createIntent(
            context = context,
            mmol = latest.mmol,
            threshold = threshold,
            snoozeMinutes = settings.snoozeMinutes,
            kind = kind,
        )
    }

    private companion object {
        const val TAG = "PhoneAlarmLauncher"
        const val REQUEST_CODE = 21
        const val WAKE_DELAY_MS = 200L
    }
}
