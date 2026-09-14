package com.diabad.alarm

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.diabad.core.alarm.AlarmVibrationId

/**
 * Clock-like strong repeating vibration on Galaxy Watch. No audio.
 * Uses max amplitude + alarm usage so One UI does not treat it as a tap.
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
        val amps = AlarmVibrationId.maxAmplitudes(pattern)
        val effect = VibrationEffect.createWaveform(pattern, amps, 1)
        if (Build.VERSION.SDK_INT >= 33) {
            vib.vibrate(
                effect,
                VibrationAttributes.Builder()
                    .setUsage(VibrationAttributes.USAGE_ALARM)
                    .build(),
            )
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

    fun stop() {
        vibrator?.cancel()
    }

    companion object {
        /** Aggressive pulse similar to Samsung Clock on Watch Ultra. */
        val PATTERN = longArrayOf(
            0,
            1200, 80, 1200, 80, 1200,
            200,
            1200, 80, 1200, 80, 1200,
        )
    }
}
