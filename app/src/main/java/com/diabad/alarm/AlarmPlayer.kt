package com.diabad.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.diabad.R
import com.diabad.domain.model.AlarmSoundId
import com.diabad.domain.model.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plays hypo alerts on the alarm audio stream so they cut through silent/DND
 * (when notification policy access is granted) independently of media volume.
 */
@Singleton
class AlarmPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var player: MediaPlayer? = null
    private var previousAlarmVolume: Int? = null
    private var previousMusicVolume: Int? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    @Synchronized
    fun start(settings: AppSettings, loop: Boolean = true) {
        stopInternal(restoreVolume = false)
        boostVolumes()
        val played = playBuiltInOrCustom(settings, loop)
        if (!played) {
            Log.w(TAG, "MediaPlayer failed — falling back to system tone")
            playFallbackTone()
        }
    }

    /** Short non-looping preview for Settings "Прослушать". */
    @Synchronized
    fun preview(settings: AppSettings) {
        start(settings, loop = false)
    }

    @Synchronized
    fun stop() {
        stopInternal(restoreVolume = true)
    }

    fun isPlaying(): Boolean = player?.isPlaying == true

    private fun playBuiltInOrCustom(settings: AppSettings, loop: Boolean): Boolean {
        return try {
            val customUri = settings.customAlarmUri?.takeIf {
                settings.alarmSoundId == AlarmSoundId.CUSTOM && it.isNotBlank()
            }

            val mp = if (customUri != null) {
                MediaPlayer().apply {
                    setAudioAttributes(alarmAttrs())
                    setDataSource(context, Uri.parse(customUri))
                    isLooping = loop
                    setVolume(1f, 1f)
                    prepare()
                }
            } else {
                val resId = builtInRes(settings.alarmSoundId)
                MediaPlayer.create(context, resId)?.apply {
                    setAudioAttributes(alarmAttrs())
                    isLooping = loop
                    setVolume(1f, 1f)
                } ?: return false
            }

            mp.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error what=$what extra=$extra")
                stopInternal(restoreVolume = true)
                true
            }
            if (!loop) {
                mp.setOnCompletionListener { stop() }
            }
            mp.start()
            player = mp
            true
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to start alarm", t)
            stopInternal(restoreVolume = false)
            false
        }
    }

    private fun playFallbackTone() {
        try {
            val tg = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            tg.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1500)
            mainHandler.postDelayed({
                runCatching { tg.release() }
                restoreVolumes()
            }, 1600)
        } catch (t: Throwable) {
            Log.e(TAG, "Fallback tone failed", t)
            restoreVolumes()
        }
    }

    private fun stopInternal(restoreVolume: Boolean) {
        runCatching {
            player?.stop()
            player?.release()
        }
        player = null
        if (restoreVolume) restoreVolumes()
    }

    private fun builtInRes(id: AlarmSoundId): Int = when (id) {
        AlarmSoundId.SIREN -> R.raw.alarm_siren
        AlarmSoundId.MEDICAL -> R.raw.alarm_medical
        AlarmSoundId.CLOCK -> R.raw.alarm_clock
        AlarmSoundId.CARTOON_HORN -> R.raw.alarm_cartoon_horn
        AlarmSoundId.CARTOON_RING -> R.raw.alarm_cartoon_ring
        AlarmSoundId.CARTOON_CHIRP -> R.raw.alarm_cartoon_chirp
        AlarmSoundId.CUSTOM -> R.raw.alarm_siren
    }

    private fun alarmAttrs(): AudioAttributes =
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

    private fun boostVolumes() {
        val am = context.getSystemService(AudioManager::class.java) ?: return
        if (previousAlarmVolume == null) {
            previousAlarmVolume = am.getStreamVolume(AudioManager.STREAM_ALARM)
        }
        if (previousMusicVolume == null) {
            previousMusicVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        }
        val alarmMax = am.getStreamMaxVolume(AudioManager.STREAM_ALARM).coerceAtLeast(1)
        val musicMax = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        runCatching { am.setStreamVolume(AudioManager.STREAM_ALARM, alarmMax, 0) }
        // Some Samsung builds keep USAGE_ALARM tied to media mute — nudge music too.
        runCatching { am.setStreamVolume(AudioManager.STREAM_MUSIC, musicMax, 0) }
    }

    private fun restoreVolumes() {
        val am = context.getSystemService(AudioManager::class.java) ?: return
        previousAlarmVolume?.let { vol ->
            runCatching { am.setStreamVolume(AudioManager.STREAM_ALARM, vol, 0) }
        }
        previousMusicVolume?.let { vol ->
            runCatching { am.setStreamVolume(AudioManager.STREAM_MUSIC, vol, 0) }
        }
        previousAlarmVolume = null
        previousMusicVolume = null
    }

    private companion object {
        const val TAG = "AlarmPlayer"
    }
}
