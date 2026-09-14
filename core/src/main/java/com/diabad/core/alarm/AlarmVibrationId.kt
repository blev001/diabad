package com.diabad.core.alarm

/**
 * Phone + Galaxy Watch alarm vibration presets.
 * Waveforms are [delay, on, off, on, …] in milliseconds.
 */
enum class AlarmVibrationId {
    OFF,
    SHORT,
    CLOCK,
    STRONG,
    SOS,
    PULSE,
    ;

    fun waveform(): LongArray = when (this) {
        OFF -> longArrayOf(0)
        SHORT -> longArrayOf(0, 220, 140, 220, 520)
        CLOCK -> longArrayOf(
            0,
            1000, 180, 1000, 180, 1000,
            350,
            1000, 180, 1000, 180, 1000,
        )
        STRONG -> longArrayOf(
            0,
            900, 200, 900, 200, 900,
            400,
            900, 200, 900, 200, 900,
        )
        SOS -> longArrayOf(
            0,
            100, 80, 100, 80, 100,
            220,
            380, 80, 380, 80, 380,
            220,
            100, 80, 100, 80, 100,
            600,
        )
        PULSE -> longArrayOf(0, 90, 70, 260, 420)
    }

    /** Max motor amplitude for each [waveform] slot: on = 255, off/delay = 0. */
    fun amplitudes(): IntArray = maxAmplitudes(waveform())

    companion object {
        const val MAX_AMPLITUDE = 255

        fun fromName(raw: String?): AlarmVibrationId =
            raw?.let { runCatching { valueOf(it) }.getOrNull() } ?: CLOCK

        fun maxAmplitudes(timings: LongArray): IntArray =
            IntArray(timings.size) { index ->
                if (index % 2 == 1) MAX_AMPLITUDE else 0
            }
    }
}
