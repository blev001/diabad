package com.diabad.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.widget.Toast
import com.diabad.R
import com.diabad.domain.model.AlarmSoundId
import com.diabad.domain.model.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.sin
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Samsung-hardened player:
 * - copies raw WAV to cache (avoids aapt compression / fd issues)
 * - falls back to AudioTrack PCM sine (never depends on codecs)
 * - vibrates so the user always gets feedback on "Прослушать"
 */
@Singleton
class AlarmPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var audioTrack: AudioTrack? = null
    private var previousMusicVolume: Int? = null

    fun start(settings: AppSettings, loop: Boolean = true) {
        mainHandler.post {
            stopInternal(restoreVolume = false)
            ensureAudibleVolume()
            vibratePulse()
            val ok = playWavFile(settings, loop) || playSineAlarm(loop)
            toast(if (ok) "Сигнал тревоги" else "Не удалось включить звук")
            Log.i(TAG, "start ok=$ok loop=$loop id=${settings.alarmSoundId}")
        }
    }

    fun preview(settings: AppSettings) {
        mainHandler.post {
            stopInternal(restoreVolume = false)
            ensureAudibleVolume()
            vibratePulse()
            val ok = playWavFile(settings, loop = false) || playSineAlarm(loop = false)
            toast(
                if (ok) "Звук: ${settings.alarmSoundId.name}"
                else "Ошибка звука — проверьте громкость медиа",
            )
            Log.i(TAG, "preview ok=$ok id=${settings.alarmSoundId}")
        }
    }

    fun stop() {
        mainHandler.post { stopInternal(restoreVolume = true) }
    }

    fun isPlaying(): Boolean =
        player?.isPlaying == true || audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING

    private fun playWavFile(settings: AppSettings, loop: Boolean): Boolean {
        return try {
            val file = if (
                settings.alarmSoundId == AlarmSoundId.CUSTOM &&
                !settings.customAlarmUri.isNullOrBlank()
            ) {
                // Custom content URIs: feed MediaPlayer directly
                return playUri(settings.customAlarmUri!!, loop)
            } else {
                copyRawToCache(builtInRes(settings.alarmSoundId))
            }

            val mp = MediaPlayer()
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            mp.setDataSource(file.absolutePath)
            mp.isLooping = loop
            mp.setVolume(1f, 1f)
            if (!loop) {
                mp.setOnCompletionListener { stopInternal(restoreVolume = true) }
            }
            mp.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error what=$what extra=$extra")
                true
            }
            mp.prepare()
            mp.start()
            player = mp
            true
        } catch (t: Throwable) {
            Log.e(TAG, "playWavFile failed", t)
            runCatching { player?.release() }
            player = null
            false
        }
    }

    private fun playUri(uri: String, loop: Boolean): Boolean {
        return try {
            val mp = MediaPlayer()
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            mp.setDataSource(context, android.net.Uri.parse(uri))
            mp.isLooping = loop
            mp.setVolume(1f, 1f)
            if (!loop) mp.setOnCompletionListener { stopInternal(restoreVolume = true) }
            mp.prepare()
            mp.start()
            player = mp
            true
        } catch (t: Throwable) {
            Log.e(TAG, "playUri failed", t)
            false
        }
    }

    /** Codec-free path — always works if the media stream can output audio. */
    private fun playSineAlarm(loop: Boolean): Boolean {
        return try {
            val sampleRate = 44100
            val durationSec = if (loop) 2.5 else 1.6
            val freq = 880.0
            val n = (sampleRate * durationSec).toInt()
            val buf = ShortArray(n)
            for (i in 0 until n) {
                val t = i.toDouble() / sampleRate
                // Simple envelope so it isn't a click
                val env = when {
                    i < sampleRate / 50 -> i.toDouble() / (sampleRate / 50)
                    i > n - sampleRate / 30 -> (n - i).toDouble() / (sampleRate / 30)
                    else -> 1.0
                }.coerceIn(0.0, 1.0)
                // Two-tone pattern every 0.35s
                val f = if (((i / (sampleRate * 0.35)).toInt() % 2) == 0) freq else freq * 1.25
                buf[i] = (sin(2.0 * Math.PI * f * t) * 0.9 * env * Short.MAX_VALUE).toInt().toShort()
            }

            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(maxOf(minBuf, buf.size * 2))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buf, 0, buf.size)
            if (loop && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                track.setLoopPoints(0, buf.size, -1)
            }
            track.play()
            audioTrack = track
            if (!loop) {
                mainHandler.postDelayed(
                    { stopInternal(restoreVolume = true) },
                    (durationSec * 1000).toLong() + 100,
                )
            }
            true
        } catch (t: Throwable) {
            Log.e(TAG, "playSineAlarm failed", t)
            runCatching { audioTrack?.release() }
            audioTrack = null
            false
        }
    }

    private fun copyRawToCache(resId: Int): File {
        val name = context.resources.getResourceEntryName(resId) + ".wav"
        val out = File(context.cacheDir, name)
        // Always refresh so updates to assets are picked up
        context.resources.openRawResource(resId).use { input ->
            FileOutputStream(out).use { output -> input.copyTo(output) }
        }
        return out
    }

    private fun stopInternal(restoreVolume: Boolean) {
        runCatching {
            player?.setOnCompletionListener(null)
            if (player?.isPlaying == true) player?.stop()
            player?.release()
        }
        player = null
        runCatching {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.release()
        }
        audioTrack = null
        if (restoreVolume) restoreVolume()
    }

    private fun builtInRes(id: AlarmSoundId): Int = when (id) {
        AlarmSoundId.SIREN -> R.raw.alarm_siren
        AlarmSoundId.MEDICAL -> R.raw.alarm_medical
        AlarmSoundId.CLOCK -> R.raw.alarm_clock
        AlarmSoundId.CARTOON_HORN -> R.raw.alarm_cartoon_horn
        AlarmSoundId.CARTOON_RING -> R.raw.alarm_cartoon_ring
        AlarmSoundId.CARTOON_CHIRP -> R.raw.alarm_cartoon_chirp
        AlarmSoundId.CUSTOM -> R.raw.alarm_beep_loud
    }

    private fun ensureAudibleVolume() {
        val am = context.getSystemService(AudioManager::class.java) ?: return
        if (previousMusicVolume == null) {
            previousMusicVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        }
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        // Force media stream up and show the UI slider so user sees it
        runCatching {
            am.setStreamVolume(AudioManager.STREAM_MUSIC, max, AudioManager.FLAG_SHOW_UI)
        }
        runCatching {
            if (am.isStreamMute(AudioManager.STREAM_MUSIC)) {
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
            }
        }
        // Also nudge alarm stream for hypo path
        runCatching {
            am.setStreamVolume(
                AudioManager.STREAM_ALARM,
                am.getStreamMaxVolume(AudioManager.STREAM_ALARM).coerceAtLeast(1),
                0,
            )
        }
    }

    private fun restoreVolume() {
        val am = context.getSystemService(AudioManager::class.java) ?: return
        previousMusicVolume?.let {
            runCatching { am.setStreamVolume(AudioManager.STREAM_MUSIC, it, 0) }
        }
        previousMusicVolume = null
    }

    private fun vibratePulse() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Vibrator::class.java)
            } ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 120, 80, 120), -1),
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(300)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "vibrate failed", t)
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(context.applicationContext, msg, Toast.LENGTH_SHORT).show()
    }

    private companion object {
        const val TAG = "AlarmPlayer"
    }
}
