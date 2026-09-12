package com.diabad.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.diabad.R
import com.diabad.domain.model.AlarmSoundId
import com.diabad.domain.model.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class SoundTestResult(
    val ok: Boolean,
    val method: String,
    val detail: String,
)

/**
 * Plays the *selected* ringtone via MediaPlayer (full clip / loop).
 * Short diagnostic tones are only a last-resort fallback.
 */
@Singleton
class AlarmPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var stopRunnable: Runnable? = null

    @Volatile
    var lastResult: SoundTestResult = SoundTestResult(false, "—", "ещё не запускали")
        private set

    fun start(settings: AppSettings, loop: Boolean = true) {
        mainHandler.post {
            val result = playSelected(
                soundId = settings.alarmSoundId,
                customUri = settings.customAlarmUri,
                loop = loop,
                previewSeconds = null,
            )
            lastResult = result
            Log.i(TAG, "start $result")
        }
    }

    fun preview(settings: AppSettings): SoundTestResult =
        preview(settings.alarmSoundId, settings.customAlarmUri)

    fun preview(soundId: AlarmSoundId, customUri: String? = null): SoundTestResult {
        fun run(): SoundTestResult {
            val result = playSelected(
                soundId = soundId,
                customUri = customUri,
                loop = false,
                previewSeconds = PREVIEW_MIN_MS,
            )
            lastResult = result
            Log.i(TAG, "preview $result id=$soundId")
            return result
        }
        if (Looper.myLooper() == Looper.getMainLooper()) return run()
        var result = SoundTestResult(false, "thread", "not main")
        val lock = Object()
        mainHandler.post {
            result = run()
            synchronized(lock) { lock.notifyAll() }
        }
        synchronized(lock) {
            try {
                lock.wait(6_000)
            } catch (_: InterruptedException) {
            }
        }
        return result
    }

    fun stop() {
        mainHandler.post { stopAll() }
    }

    fun isPlaying(): Boolean = player?.isPlaying == true

    private fun playSelected(
        soundId: AlarmSoundId,
        customUri: String?,
        loop: Boolean,
        previewSeconds: Int?,
    ): SoundTestResult {
        stopAll()
        forceVolumes()
        return try {
            val mp = MediaPlayer()
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            if (soundId == AlarmSoundId.CUSTOM && !customUri.isNullOrBlank()) {
                mp.setDataSource(context, Uri.parse(customUri))
            } else {
                val file = copyRawToCache(builtInRes(soundId))
                mp.setDataSource(file.absolutePath)
            }
            mp.setVolume(1f, 1f)
            mp.prepare()

            val duration = mp.duration.coerceAtLeast(0)
            if (loop) {
                mp.isLooping = true
                mp.start()
            } else if (previewSeconds != null) {
                // Play the real clip; if it's very short, loop until preview window ends.
                val needLoop = duration in 1 until previewSeconds
                mp.isLooping = needLoop
                mp.start()
                val stopAfter = if (needLoop) previewSeconds else (duration + 200).coerceAtMost(12_000)
                val r = Runnable { stopAll() }
                stopRunnable = r
                mainHandler.postDelayed(r, stopAfter.toLong())
            } else {
                mp.isLooping = false
                mp.setOnCompletionListener { stopAll() }
                mp.start()
            }

            player = mp
            if (!mp.isPlaying) {
                stopAll()
                return SoundTestResult(false, "MediaPlayer", "start() но isPlaying=false")
            }
            SoundTestResult(
                ok = true,
                method = "preset:${soundId.name}",
                detail = if (duration > 0) "${duration}ms" else "playing",
            )
        } catch (t: Throwable) {
            Log.e(TAG, "playSelected failed id=$soundId", t)
            stopAll()
            SoundTestResult(false, "MediaPlayer", "${t.javaClass.simpleName}: ${t.message}")
        }
    }

    private fun copyRawToCache(resId: Int): File {
        val name = context.resources.getResourceEntryName(resId) + ".wav"
        val out = File(context.cacheDir, name)
        context.resources.openRawResource(resId).use { input ->
            FileOutputStream(out).use { output -> input.copyTo(output) }
        }
        return out
    }

    private fun builtInRes(id: AlarmSoundId): Int = when (id) {
        AlarmSoundId.SIREN -> R.raw.alarm_siren
        AlarmSoundId.MEDICAL -> R.raw.alarm_medical
        AlarmSoundId.CLOCK -> R.raw.alarm_clock
        AlarmSoundId.TWO_TONE -> R.raw.alarm_two_tone
        AlarmSoundId.KLAXON -> R.raw.alarm_klaxon
        AlarmSoundId.SOS_ROBOT -> R.raw.alarm_sos_robot
        AlarmSoundId.RISING_PANIC -> R.raw.alarm_rising_panic
        AlarmSoundId.COIN_RUSH -> R.raw.alarm_coin_rush
        AlarmSoundId.POWERUP -> R.raw.alarm_powerup
        AlarmSoundId.BOSS_ALERT -> R.raw.alarm_boss_alert
        AlarmSoundId.LASER_ZAP -> R.raw.alarm_laser_zap
        AlarmSoundId.FANFARE_8BIT -> R.raw.alarm_fanfare_8bit
        AlarmSoundId.SPACE_BLIPS -> R.raw.alarm_space_blips
        AlarmSoundId.LEVEL_UP -> R.raw.alarm_level_up
        AlarmSoundId.GAME_OVER -> R.raw.alarm_game_over
        AlarmSoundId.BUMP_BEEP -> R.raw.alarm_bump_beep
        AlarmSoundId.BOING -> R.raw.alarm_boing
        AlarmSoundId.DUCK -> R.raw.alarm_duck
        AlarmSoundId.MEOW -> R.raw.alarm_meow
        AlarmSoundId.GIGGLE -> R.raw.alarm_giggle
        AlarmSoundId.BUBBLES -> R.raw.alarm_bubbles
        AlarmSoundId.RETRO_PHONE -> R.raw.alarm_retro_phone
        AlarmSoundId.XYLOPHONE -> R.raw.alarm_xylophone
        AlarmSoundId.CARTOON_HORN -> R.raw.alarm_cartoon_horn
        AlarmSoundId.CARTOON_RING -> R.raw.alarm_cartoon_ring
        AlarmSoundId.CARTOON_CHIRP -> R.raw.alarm_cartoon_chirp
        AlarmSoundId.CUSTOM -> R.raw.alarm_beep_loud
    }

    private fun forceVolumes() {
        val am = context.getSystemService(AudioManager::class.java) ?: return
        runCatching {
            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            am.setStreamVolume(AudioManager.STREAM_MUSIC, max, AudioManager.FLAG_SHOW_UI)
            if (am.isStreamMute(AudioManager.STREAM_MUSIC)) {
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
            }
        }
        runCatching {
            val max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM).coerceAtLeast(1)
            am.setStreamVolume(AudioManager.STREAM_ALARM, max, 0)
        }
    }

    private fun stopAll() {
        stopRunnable?.let { mainHandler.removeCallbacks(it) }
        stopRunnable = null
        runCatching {
            player?.setOnCompletionListener(null)
            if (player?.isPlaying == true) player?.stop()
            player?.release()
        }
        player = null
    }

    private companion object {
        const val TAG = "DiaBAD_SOUND"
        const val PREVIEW_MIN_MS = 4500
    }
}
