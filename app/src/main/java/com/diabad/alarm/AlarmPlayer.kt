package com.diabad.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
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

    @Synchronized
    fun start(settings: AppSettings, loop: Boolean = true) {
        stopInternal(restoreVolume = false)
        boostAlarmVolume()
        val uri = resolveUri(settings) ?: return
        try {
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setDataSource(context, uri)
                isLooping = loop
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error what=$what extra=$extra")
                    stopInternal(restoreVolume = true)
                    true
                }
                prepare()
                start()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to start alarm", t)
            stopInternal(restoreVolume = true)
        }
    }

    /** Short non-looping preview for Settings "Прослушать". */
    @Synchronized
    fun preview(settings: AppSettings) {
        start(settings, loop = false)
        player?.setOnCompletionListener {
            stop()
        }
    }

    @Synchronized
    fun stop() {
        stopInternal(restoreVolume = true)
    }

    fun isPlaying(): Boolean = player?.isPlaying == true

    private fun stopInternal(restoreVolume: Boolean) {
        runCatching {
            player?.stop()
            player?.release()
        }
        player = null
        if (restoreVolume) restoreAlarmVolume()
    }

    private fun resolveUri(settings: AppSettings): Uri? {
        if (settings.alarmSoundId == AlarmSoundId.CUSTOM) {
            val custom = settings.customAlarmUri
            if (!custom.isNullOrBlank()) {
                return Uri.parse(custom)
            }
        }
        val resId = when (settings.alarmSoundId) {
            AlarmSoundId.SIREN -> R.raw.alarm_siren
            AlarmSoundId.MEDICAL -> R.raw.alarm_medical
            AlarmSoundId.CLOCK -> R.raw.alarm_clock
            AlarmSoundId.CARTOON_HORN -> R.raw.alarm_cartoon_horn
            AlarmSoundId.CARTOON_RING -> R.raw.alarm_cartoon_ring
            AlarmSoundId.CARTOON_CHIRP -> R.raw.alarm_cartoon_chirp
            AlarmSoundId.CUSTOM -> R.raw.alarm_siren
        }
        return Uri.parse("android.resource://${context.packageName}/$resId")
    }

    private fun boostAlarmVolume() {
        val am = context.getSystemService(AudioManager::class.java) ?: return
        if (previousAlarmVolume == null) {
            previousAlarmVolume = am.getStreamVolume(AudioManager.STREAM_ALARM)
        }
        val max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        // Aim high but leave one step headroom on some OEMs.
        am.setStreamVolume(AudioManager.STREAM_ALARM, max.coerceAtLeast(1), 0)
    }

    private fun restoreAlarmVolume() {
        val previous = previousAlarmVolume ?: return
        previousAlarmVolume = null
        val am = context.getSystemService(AudioManager::class.java) ?: return
        runCatching { am.setStreamVolume(AudioManager.STREAM_ALARM, previous, 0) }
    }

    private companion object {
        const val TAG = "AlarmPlayer"
    }
}
