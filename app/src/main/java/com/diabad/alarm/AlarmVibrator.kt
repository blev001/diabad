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
import com.diabad.core.alarm.AlarmVibrationId
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmVibrator @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }
    private var stopRunnable: Runnable? = null

    fun start(id: AlarmVibrationId, loop: Boolean = true) {
        mainHandler.post {
            val pattern = id.waveform()
            val vib = vibrator?.takeIf { it.hasVibrator() } ?: return@post
            if (id == AlarmVibrationId.OFF || pattern.size < 2) {
                vib.cancel()
                return@post
            }
            stopRunnable?.let { mainHandler.removeCallbacks(it) }
            stopRunnable = null
            val repeat = if (loop) 1 else -1
            val effect = VibrationEffect.createWaveform(pattern, id.amplitudes(), repeat)
            vibrate(vib, effect)
        }
    }

    fun preview(id: AlarmVibrationId) {
        start(id, loop = true)
        val r = Runnable { stop() }
        stopRunnable = r
        mainHandler.postDelayed(r, PREVIEW_MS)
    }

    fun stop() {
        mainHandler.post {
            stopRunnable?.let { mainHandler.removeCallbacks(it) }
            stopRunnable = null
            vibrator?.cancel()
        }
    }

    private fun vibrate(vib: Vibrator, effect: VibrationEffect) {
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

    private companion object {
        const val PREVIEW_MS = 2500L
    }
}
