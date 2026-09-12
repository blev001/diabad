package com.diabad.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaActionSound
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.diabad.R
import com.diabad.domain.model.AlarmSoundId
import com.diabad.domain.model.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.sin
import javax.inject.Inject
import javax.inject.Singleton

data class SoundTestResult(
    val ok: Boolean,
    val method: String,
    val detail: String,
)

/**
 * Tries multiple Samsung-resistant playback paths until one succeeds.
 */
@Singleton
class AlarmPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var audioTrack: AudioTrack? = null
    private var tone: ToneGenerator? = null

    @Volatile
    var lastResult: SoundTestResult = SoundTestResult(false, "—", "ещё не запускали")
        private set

    fun start(settings: AppSettings, loop: Boolean = true) {
        mainHandler.post {
            val result = runAllStrategies(settings, loop)
            lastResult = result
            Log.e(TAG, "start $result")
        }
    }

    fun preview(settings: AppSettings): SoundTestResult {
        // Run synchronously on caller thread if already main, else block via latch-less postAndWait pattern
        if (Looper.myLooper() == Looper.getMainLooper()) {
            val result = runAllStrategies(settings, loop = false)
            lastResult = result
            Log.e(TAG, "preview $result")
            return result
        }
        var result = SoundTestResult(false, "thread", "not main")
        val lock = Object()
        mainHandler.post {
            result = runAllStrategies(settings, loop = false)
            lastResult = result
            Log.e(TAG, "preview $result")
            synchronized(lock) { lock.notifyAll() }
        }
        synchronized(lock) {
            try {
                lock.wait(4000)
            } catch (_: InterruptedException) {
            }
        }
        return result
    }

    fun stop() {
        mainHandler.post { stopAll() }
    }

    fun isPlaying(): Boolean =
        player?.isPlaying == true || audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING

    private fun runAllStrategies(settings: AppSettings, loop: Boolean): SoundTestResult {
        stopAll()
        forceVolumes()

        val attempts = listOf(
            "MediaActionSound" to { playMediaActionSound() },
            "Tone/NOTIFICATION" to { playTone(AudioManager.STREAM_NOTIFICATION) },
            "Tone/RING" to { playTone(AudioManager.STREAM_RING) },
            "Tone/MUSIC" to { playTone(AudioManager.STREAM_MUSIC) },
            "Tone/ALARM" to { playTone(AudioManager.STREAM_ALARM) },
            "AudioTrack/MEDIA" to { playAudioTrack(AudioAttributes.USAGE_MEDIA, loop) },
            "AudioTrack/ALARM" to { playAudioTrack(AudioAttributes.USAGE_ALARM, loop) },
            "AudioTrack/NOTIFY" to { playAudioTrack(AudioAttributes.USAGE_NOTIFICATION, loop) },
            "MediaPlayer/file" to { playWavViaMediaPlayer(settings, loop) },
            "NotificationSound" to { playViaNotification() },
        )

        val errors = mutableListOf<String>()
        for ((name, block) in attempts) {
            try {
                if (block()) {
                    return SoundTestResult(true, name, "ок")
                }
                errors += "$name: false"
            } catch (t: Throwable) {
                errors += "$name: ${t.javaClass.simpleName}: ${t.message}"
                Log.e(TAG, "strategy $name failed", t)
                stopAll()
            }
        }
        return SoundTestResult(false, "none", errors.joinToString(" | ").take(180))
    }

    private fun playMediaActionSound(): Boolean {
        val mas = MediaActionSound()
        mas.load(MediaActionSound.SHUTTER_CLICK)
        mas.load(MediaActionSound.START_VIDEO_RECORDING)
        // Small delay not available sync — play shutter which is preloaded on many OEMs
        mas.play(MediaActionSound.SHUTTER_CLICK)
        mas.play(MediaActionSound.START_VIDEO_RECORDING)
        mainHandler.postDelayed({ runCatching { mas.release() } }, 1500)
        return true
    }

    private fun playTone(stream: Int): Boolean {
        val tg = ToneGenerator(stream, 100)
        tone = tg
        tg.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 900)
        mainHandler.postDelayed({
            runCatching { tg.release() }
            if (tone === tg) tone = null
        }, 1000)
        return true
    }

    private fun playAudioTrack(usage: Int, loop: Boolean): Boolean {
        val sampleRate = 44100
        val durationSec = 1.2
        val n = (sampleRate * durationSec).toInt()
        val buf = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / sampleRate
            val f = if ((i / (sampleRate / 4)) % 2 == 0) 880.0 else 1175.0
            val env = (sin(Math.PI * i / n)).coerceIn(0.0, 1.0)
            buf[i] = (sin(2 * Math.PI * f * t) * env * 0.95 * Short.MAX_VALUE).toInt().toShort()
        }
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(usage)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(maxOf(minBuf, n * 2))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        val written = track.write(buf, 0, buf.size)
        if (written <= 0) {
            track.release()
            return false
        }
        if (loop && Build.VERSION.SDK_INT >= 23) {
            track.setLoopPoints(0, buf.size, -1)
        }
        track.play()
        audioTrack = track
        if (!loop) {
            mainHandler.postDelayed({ stopAll() }, 1300)
        }
        return track.playState == AudioTrack.PLAYSTATE_PLAYING || written > 0
    }

    private fun playWavViaMediaPlayer(settings: AppSettings, loop: Boolean): Boolean {
        val file = copyRawToCache(builtInRes(settings.alarmSoundId))
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
        mp.prepare()
        mp.start()
        player = mp
        if (!loop) {
            mp.setOnCompletionListener { stopAll() }
        }
        return mp.isPlaying
    }

    private fun playViaNotification(): Boolean {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return false
        val channelId = "diabad_sound_test"
        val channel = NotificationChannel(
            channelId,
            "Тест звука DiaBAD",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            setSound(
                android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 200, 100, 200)
        }
        nm.createNotificationChannel(channel)
        // Delete+recreate to force sound update on Samsung
        nm.deleteNotificationChannel(channelId)
        nm.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_glucose)
            .setContentTitle("DiaBAD — тест звука")
            .setContentText("Если слышите — канал уведомлений работает")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        nm.notify(9091, notification)
        mainHandler.postDelayed({ nm.cancel(9091) }, 2500)
        return true
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
        listOf(
            AudioManager.STREAM_MUSIC,
            AudioManager.STREAM_ALARM,
            AudioManager.STREAM_NOTIFICATION,
            AudioManager.STREAM_RING,
            AudioManager.STREAM_SYSTEM,
        ).forEach { stream ->
            runCatching {
                val max = am.getStreamMaxVolume(stream).coerceAtLeast(1)
                am.setStreamVolume(stream, max, 0)
                if (am.isStreamMute(stream)) {
                    am.adjustStreamVolume(stream, AudioManager.ADJUST_UNMUTE, 0)
                }
            }
        }
        // Show media slider once
        runCatching {
            am.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                am.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                AudioManager.FLAG_SHOW_UI,
            )
        }
        runCatching { am.mode = AudioManager.MODE_NORMAL }
        runCatching { am.isSpeakerphoneOn = true }
    }

    private fun stopAll() {
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
        runCatching { tone?.release() }
        tone = null
    }

    private companion object {
        const val TAG = "DiaBAD_SOUND"
    }
}
