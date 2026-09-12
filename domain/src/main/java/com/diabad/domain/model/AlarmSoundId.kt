package com.diabad.domain.model

/**
 * Built-in alarm presets. CUSTOM uses [AppSettings.customAlarmUri].
 * Game/comic names are style labels — sounds are original synthesizer tones.
 */
enum class AlarmSoundId {
    // Classic
    SIREN,
    MEDICAL,
    CLOCK,
    TWO_TONE,
    KLAXON,
    SOS_ROBOT,
    RISING_PANIC,

    // 8-bit / game-like (original)
    COIN_RUSH,
    POWERUP,
    BOSS_ALERT,
    LASER_ZAP,
    FANFARE_8BIT,
    SPACE_BLIPS,
    LEVEL_UP,
    GAME_OVER,
    BUMP_BEEP,

    // Comic / funny
    BOING,
    DUCK,
    MEOW,
    GIGGLE,
    BUBBLES,
    RETRO_PHONE,
    XYLOPHONE,

    // Cartoon-style originals (existing)
    CARTOON_HORN,
    CARTOON_RING,
    CARTOON_CHIRP,

    // Loud test + user file
    CUSTOM,
}
