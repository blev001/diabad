package com.diabad.domain.model

/**
 * Persisted alarm bookkeeping so Stop / Snooze survive process death.
 * All times are wall-clock millis; 0 means "not set".
 */
data class AlarmState(
    val snoozedUntilMillis: Long = 0L,
    val snoozedReason: AlarmReason? = null,
    val dismissedAtMillis: Long = 0L,
    val dismissedReason: AlarmReason? = null,
    val dismissedMmol: Double? = null,
    val ringingSinceMillis: Long = 0L,
    val ringingReason: AlarmReason? = null,
    val lastRemindAtMillis: Long = 0L,
)
