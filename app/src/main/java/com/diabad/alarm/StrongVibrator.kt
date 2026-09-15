package com.diabad.alarm

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.diabad.core.alarm.AlarmVibrationId
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Alarm-strength motor vibration for vibration-only mode.
 *
 * Samsung One UI haptic primitives (THUD/CLICK) use the weak "touch"
 * intensity slider. This class drives the motor with a long repeating
 * waveform at amplitude 255 and [VibrationAttributes.USAGE_ALARM].
 */
@Singleton
class StrongVibrator @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val vibrators: List<Vibrator> = collectVibrators(context)

    @Volatile
    private var running = false
    private var stopRunnable: Runnable? = null

    fun isRunning(): Boolean = running

    fun startAlarmLoop() {
        mainHandler.post {
            cancelInternal(keepRunning = false)
            if (vibrators.isEmpty()) return@post
            running = true
            val effect = VibrationEffect.createWaveform(ALARM_TIMINGS, ALARM_AMPS, 1)
            vibrators.forEach { vibrate(it, effect) }
            Log.i(
                TAG,
                "Alarm vibration loop started motors=${vibrators.size} " +
                    "ampControl=${vibrators.any { it.hasAmplitudeControl() }}",
            )
        }
    }

    fun previewAlarm(durationMs: Long = PREVIEW_MS) {
        startAlarmLoop()
        mainHandler.post {
            val stop = Runnable { stop() }
            stopRunnable = stop
            mainHandler.postDelayed(stop, durationMs)
        }
    }

    fun pulseWarning() {
        mainHandler.post {
            if (running) return@post
            if (vibrators.isEmpty()) return@post
            val effect = VibrationEffect.createWaveform(WARNING_TIMINGS, WARNING_AMPS, -1)
            vibrators.forEach { vibrate(it, effect) }
            Log.i(TAG, "Warning vibration pulse")
        }
    }

    fun stop() {
        mainHandler.post { cancelInternal(keepRunning = false) }
    }

    private fun cancelInternal(keepRunning: Boolean) {
        stopRunnable?.let { mainHandler.removeCallbacks(it) }
        stopRunnable = null
        vibrators.forEach { vib -> runCatching { vib.cancel() } }
        if (!keepRunning) running = false
    }

    private fun vibrate(vib: Vibrator, effect: VibrationEffect) {
        if (Build.VERSION.SDK_INT >= 33) {
            val attrs = VibrationAttributes.Builder()
                .setUsage(VibrationAttributes.USAGE_ALARM)
                .build()
            vib.vibrate(effect, attrs)
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(
                effect,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        }
    }

    companion object {
        const val TAG = "StrongVibrator"
        const val PREVIEW_MS = 4500L

        /**
         * Near-continuous max rumble: long 255 bursts, tiny gaps so the
         * actuator does not spin down. Repeats from index 1.
         */
        val ALARM_TIMINGS = longArrayOf(
            0,
            1600, 60,
            1600, 60,
            1600, 90,
        )
        val ALARM_AMPS = AlarmVibrationId.maxAmplitudes(ALARM_TIMINGS)

        val WARNING_TIMINGS = longArrayOf(0, 420, 70, 420, 70, 700)
        val WARNING_AMPS = AlarmVibrationId.maxAmplitudes(WARNING_TIMINGS)

        private fun collectVibrators(context: Context): List<Vibrator> {
            if (Build.VERSION.SDK_INT >= 31) {
                val manager = context.getSystemService(VibratorManager::class.java) ?: return emptyList()
                val ids = manager.vibratorIds
                val fromIds = ids.map { manager.getVibrator(it) }.filter { it.hasVibrator() }
                if (fromIds.isNotEmpty()) return fromIds.distinctBy { System.identityHashCode(it) }
                return listOfNotNull(manager.defaultVibrator.takeIf { it.hasVibrator() })
            }
            @Suppress("DEPRECATION")
            return listOfNotNull(
                context.getSystemService(Vibrator::class.java)?.takeIf { it.hasVibrator() },
            )
        }
    }
}
