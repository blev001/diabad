package com.diabad.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.diabad.R
import com.diabad.domain.model.AlarmSoundId
import com.diabad.domain.model.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var ringtone: Ringtone? = null
    private var toneGenerator: ToneGenerator? = null
    private var previousAlarmVolume: Int? = null
    private var previousMusicVolume: Int? = null
    private var focusRequest: AudioFocusRequest? = null

    /** Looping hypo alarm. */
    fun start(settings: AppSettings, loop: Boolean = true) {
        mainHandler.post {
            stopInternal(restoreVolume = false)
            boostVolumes()
            requestFocus()
            val ok = playPreset(settings, loop = loop, usage = AudioAttributes.USAGE_ALARM)
                || playPreset(settings, loop = loop, usage = AudioAttributes.USAGE_MEDIA)
                || playSystemAlarmRingtone(loop = loop)
            if (!ok) {
                playToneFallback(loopingHint = loop)
            }
            Log.i(TAG, "start ok=$ok loop=$loop sound=${settings.alarmSoundId}")
        }
    }

    /** Foreground "Прослушать" — must always make some audible sound. */
    fun preview(settings: AppSettings) {
        mainHandler.post {
            stopInternal(restoreVolume = false)
            boostVolumes()
            requestFocus()

            val ok = playPreset(settings, loop = false, usage = AudioAttributes.USAGE_MEDIA)
                || playPreset(settings, loop = false, usage = AudioAttributes.USAGE_ALARM)
                || playLoudBeep()
                || playSystemAlarmRingtone(loop = false)

            if (ok) {
                toast("Воспроизведение сигнала…")
            } else {
                playToneFallback(loopingHint = false)
                toast("Тестовый бип (запасной)")
            }
            Log.i(TAG, "preview ok=$ok sound=${settings.alarmSoundId}")
        }
    }

    fun stop() {
        mainHandler.post { stopInternal(restoreVolume = true) }
    }

    fun isPlaying(): Boolean =
        player?.isPlaying == true || ringtone?.isPlaying == true

    private fun playPreset(
        settings: AppSettings,
        loop: Boolean,
        usage: Int,
    ): Boolean {
        return try {
            val attrs = AudioAttributes.Builder()
                .setUsage(usage)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val mp = MediaPlayer()
            mp.setAudioAttributes(attrs)
            mp.setVolume(1f, 1f)
            mp.isLooping = loop

            if (settings.alarmSoundId == AlarmSoundId.CUSTOM &&
                !settings.customAlarmUri.isNullOrBlank()
            ) {
                mp.setDataSource(context, Uri.parse(settings.customAlarmUri))
            } else {
                val afd = context.resources.openRawResourceFd(builtInRes(settings.alarmSoundId))
                mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
            }

            mp.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error what=$what extra=$extra")
                true
            }
            if (!loop) {
                mp.setOnCompletionListener {
                    stopInternal(restoreVolume = true)
                }
            }
            mp.prepare()
            mp.start()
            player = mp
            true
        } catch (t: Throwable) {
            Log.e(TAG, "playPreset failed usage=$usage", t)
            runCatching { player?.release() }
            player = null
            false
        }
    }

    private fun playLoudBeep(): Boolean {
        return try {
            val afd = context.resources.openRawResourceFd(R.raw.alarm_beep_loud)
            val mp = MediaPlayer()
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            mp.setVolume(1f, 1f)
            mp.setOnCompletionListener { stopInternal(restoreVolume = true) }
            mp.prepare()
            mp.start()
            player = mp
            true
        } catch (t: Throwable) {
            Log.e(TAG, "playLoudBeep failed", t)
            false
        }
    }

    private fun playSystemAlarmRingtone(loop: Boolean): Boolean {
        return try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: return false
            val rt = RingtoneManager.getRingtone(context, uri) ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                rt.isLooping = loop
                rt.volume = 1f
            }
            rt.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            ringtone = rt // keep strong reference — otherwise GC kills playback
            rt.play()
            if (!loop) {
                mainHandler.postDelayed({
                    stopInternal(restoreVolume = true)
                }, 3_000)
            }
            true
        } catch (t: Throwable) {
            Log.e(TAG, "playSystemAlarmRingtone failed", t)
            false
        }
    }

    private fun playToneFallback(loopingHint: Boolean) {
        try {
            val tg = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            toneGenerator = tg
            tg.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, if (loopingHint) 2500 else 1200)
            mainHandler.postDelayed({
                runCatching { tg.release() }
                if (toneGenerator === tg) toneGenerator = null
                stopInternal(restoreVolume = true)
            }, if (loopingHint) 2600 else 1400)
        } catch (t: Throwable) {
            Log.e(TAG, "ToneGenerator failed", t)
            restoreVolumes()
            abandonFocus()
        }
    }

    private fun stopInternal(restoreVolume: Boolean) {
        runCatching {
            player?.setOnCompletionListener(null)
            player?.stop()
            player?.release()
        }
        player = null
        runCatching { ringtone?.stop() }
        ringtone = null
        runCatching { toneGenerator?.release() }
        toneGenerator = null
        if (restoreVolume) {
            restoreVolumes()
            abandonFocus()
        }
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

    private fun boostVolumes() {
        val am = context.getSystemService(AudioManager::class.java) ?: return
        if (previousAlarmVolume == null) {
            previousAlarmVolume = am.getStreamVolume(AudioManager.STREAM_ALARM)
        }
        if (previousMusicVolume == null) {
            previousMusicVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        }
        runCatching {
            am.setStreamVolume(
                AudioManager.STREAM_ALARM,
                am.getStreamMaxVolume(AudioManager.STREAM_ALARM).coerceAtLeast(1),
                AudioManager.FLAG_SHOW_UI,
            )
        }
        runCatching {
            am.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1),
                0,
            )
        }
        // Unmute if needed
        runCatching {
            if (am.isStreamMute(AudioManager.STREAM_MUSIC)) {
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
            }
            if (am.isStreamMute(AudioManager.STREAM_ALARM)) {
                am.adjustStreamVolume(AudioManager.STREAM_ALARM, AudioManager.ADJUST_UNMUTE, 0)
            }
        }
    }

    private fun restoreVolumes() {
        val am = context.getSystemService(AudioManager::class.java) ?: return
        previousAlarmVolume?.let {
            runCatching { am.setStreamVolume(AudioManager.STREAM_ALARM, it, 0) }
        }
        previousMusicVolume?.let {
            runCatching { am.setStreamVolume(AudioManager.STREAM_MUSIC, it, 0) }
        }
        previousAlarmVolume = null
        previousMusicVolume = null
    }

    private fun requestFocus() {
        val am = context.getSystemService(AudioManager::class.java) ?: return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attrs)
                .setOnAudioFocusChangeListener { }
                .build()
            focusRequest = req
            am.requestAudioFocus(req)
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        }
    }

    private fun abandonFocus() {
        val am = context.getSystemService(AudioManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { am.abandonAudioFocusRequest(it) }
        }
        focusRequest = null
    }

    private fun toast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    private companion object {
        const val TAG = "AlarmPlayer"
    }
}
