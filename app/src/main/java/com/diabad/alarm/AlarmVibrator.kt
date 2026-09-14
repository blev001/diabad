package com.diabad.alarm

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
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
            val repeat = if (loop) 0 else -1
            vib.vibrate(VibrationEffect.createWaveform(pattern, repeat))
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

    private companion object {
        const val PREVIEW_MS = 2500L
    }
}
