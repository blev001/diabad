package com.diabad.domain.model

/**
 * How a full glucose / connection-loss alarm is delivered on the phone.
 * The watch already uses strong vibration without sound.
 */
enum class AlarmAlertMode {
    /** Play the selected ringtone (notification also vibrates). */
    SOUND,

    /** No audio — strong repeating haptic only. */
    VIBRATION_ONLY,
}
