package com.diabad.alarm

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibrationEffect.Composition.PRIMITIVE_CLICK
import android.os.VibrationEffect.Composition.PRIMITIVE_QUICK_RISE
import android.os.VibrationEffect.Composition.PRIMITIVE_SLOW_RISE
import android.os.VibrationEffect.Composition.PRIMITIVE_THUD
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Strong, high-amplitude haptics for phone alerts.
 *
 * Warning: one vivid burst (approaching hypo).
 * Alarm: repeating heavy bursts — used when the user chose vibration-only.
 */
@Singleton
class StrongVibrator @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }

    @Volatile
    private var running = false
    private var loopRunnable: Runnable? = null
    private var stopRunnable: Runnable? = null

    fun isRunning(): Boolean = running

    fun startAlarmLoop() {
        mainHandler.post {
            cancelInternal(keepRunning = false)
            val vib = vibrator ?: return@post
            if (!vib.hasVibrator()) return@post
            running = true
            if (supportsRichHaptics(vib)) {
                pulseRich(alarmBurstEffect())
                val loop = object : Runnable {
                    override fun run() {
                        if (!running) return
                        pulseRich(alarmBurstEffect())
                        mainHandler.postDelayed(this, ALARM_BURST_PERIOD_MS)
                    }
                }
                loopRunnable = loop
                mainHandler.postDelayed(loop, ALARM_BURST_PERIOD_MS)
            } else {
                vibrate(vib, VibrationEffect.createWaveform(ALARM_TIMINGS, ALARM_AMPS, 0))
            }
            Log.i(TAG, "Alarm vibration loop started rich=${supportsRichHaptics(vib)}")
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
            val vib = vibrator ?: return@post
            if (!vib.hasVibrator()) return@post
            if (supportsRichHaptics(vib)) {
                vibrate(vib, warningBurstEffect())
            } else {
                vibrate(vib, VibrationEffect.createWaveform(WARNING_TIMINGS, WARNING_AMPS, -1))
            }
            Log.i(TAG, "Warning vibration pulse")
        }
    }

    fun stop() {
        mainHandler.post { cancelInternal(keepRunning = false) }
    }

    private fun pulseRich(effect: VibrationEffect) {
        val vib = vibrator ?: return
        if (!running) return
        vibrate(vib, effect)
    }

    private fun cancelInternal(keepRunning: Boolean) {
        loopRunnable?.let { mainHandler.removeCallbacks(it) }
        loopRunnable = null
        stopRunnable?.let { mainHandler.removeCallbacks(it) }
        stopRunnable = null
        runCatching { vibrator?.cancel() }
        if (!keepRunning) running = false
    }

    private fun vibrate(vib: Vibrator, effect: VibrationEffect) {
        if (Build.VERSION.SDK_INT >= 33) {
            val attrs = VibrationAttributes.Builder()
                .setUsage(VibrationAttributes.USAGE_ALARM)
                .build()
            vib.vibrate(effect, attrs)
        } else {
            vib.vibrate(effect)
        }
    }

    private fun supportsRichHaptics(vib: Vibrator): Boolean =
        vib.areAllPrimitivesSupported(
            PRIMITIVE_THUD,
            PRIMITIVE_CLICK,
            PRIMITIVE_QUICK_RISE,
        )

    private fun alarmBurstEffect(): VibrationEffect =
        VibrationEffect.startComposition()
            .addPrimitive(PRIMITIVE_THUD, 1f)
            .addPrimitive(PRIMITIVE_CLICK, 1f, 20)
            .addPrimitive(PRIMITIVE_THUD, 1f, 30)
            .addPrimitive(PRIMITIVE_QUICK_RISE, 1f, 15)
            .addPrimitive(PRIMITIVE_THUD, 1f, 35)
            .addPrimitive(PRIMITIVE_CLICK, 1f, 20)
            .addPrimitive(PRIMITIVE_THUD, 1f, 40)
            .compose()

    private fun warningBurstEffect(): VibrationEffect {
        val builder = VibrationEffect.startComposition()
            .addPrimitive(PRIMITIVE_THUD, 1f)
            .addPrimitive(PRIMITIVE_CLICK, 1f, 25)
            .addPrimitive(PRIMITIVE_THUD, 1f, 35)
            .addPrimitive(PRIMITIVE_QUICK_RISE, 1f, 20)
            .addPrimitive(PRIMITIVE_THUD, 1f, 45)
        if (vibrator?.areAllPrimitivesSupported(PRIMITIVE_SLOW_RISE) == true) {
            builder.addPrimitive(PRIMITIVE_SLOW_RISE, 1f, 30)
            builder.addPrimitive(PRIMITIVE_THUD, 1f, 40)
        }
        return builder.compose()
    }

    companion object {
        private const val TAG = "StrongVibrator"
        private const val PREVIEW_MS = 2800L
        private const val ALARM_BURST_PERIOD_MS = 1100L

        /** Fallback waveform: sharp-sharp-heavy, pause, repeat. Max amplitude. */
        val ALARM_TIMINGS = longArrayOf(
            0,
            80, 35, 80, 35, 420,
            70,
            80, 35, 80, 35, 420,
            180,
        )
        val ALARM_AMPS = intArrayOf(
            0,
            255, 0, 255, 0, 255,
            0,
            255, 0, 255, 0, 255,
            0,
        )

        val WARNING_TIMINGS = longArrayOf(0, 100, 40, 100, 40, 480, 70, 650)
        val WARNING_AMPS = intArrayOf(0, 255, 0, 255, 0, 255, 0, 220)
    }
}
