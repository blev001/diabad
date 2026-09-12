package com.diabad.domain.model

/**
 * What DiaBAD does when OtTai stops sending readings.
 * User picks one in Settings.
 */
enum class ConnectionLossMode {
    /** Show "no signal" only — no sound. */
    SILENT,

    /** After a grace period, gentle sound/vibration reminder. */
    REMIND,

    /** Treat loss of signal almost like a hypo alarm. */
    ALARM,
}
