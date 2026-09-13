package com.diabad.alarm

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.content.Context

/**
 * Clock-like strong repeating vibration on Galaxy Watch. No audio.
 */
class WatchAlarmVibrator(context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }

    fun start(pattern: LongArray = PATTERN) {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return
        if (pattern.size < 2) return
        val effect = VibrationEffect.createWaveform(pattern, 0)
        vib.vibrate(effect)
    }

    fun stop() {
        vibrator?.cancel()
    }

    companion object {
        /** Aggressive pulse similar to Samsung Clock on Watch Ultra. */
        val PATTERN = longArrayOf(
            0,
            1000, 180, 1000, 180, 1000,
            350,
            1000, 180, 1000, 180, 1000,
        )
    }
}
