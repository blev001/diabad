package com.diabad.core.alarm

/**
 * Fixed postpone slots. While a slot is active the glucose alarm stays silent,
 * even if the reading is still below (or above) the threshold.
 */
object AlarmSnoozeSlots {
    val MINUTES: List<Int> = listOf(15, 30, 60)
    const val DEFAULT_MINUTES = 15

    fun normalize(minutes: Int): Int {
        if (minutes in MINUTES) return minutes
        val clamped = minutes.coerceIn(1, 180)
        return MINUTES.minBy { kotlin.math.abs(it - clamped) }
    }

    fun isActive(untilMillis: Long, nowMillis: Long): Boolean = untilMillis > nowMillis

    fun remainingMinutes(untilMillis: Long, nowMillis: Long): Int {
        if (untilMillis <= nowMillis) return 0
        val leftover = untilMillis - nowMillis
        return ((leftover + 59_999L) / 60_000L).toInt().coerceAtLeast(1)
    }

    fun shouldRing(
        outOfRange: Boolean,
        dismissedUntilRecovery: Boolean,
        snoozedUntilMillis: Long,
        nowMillis: Long,
    ): Boolean = outOfRange &&
        !dismissedUntilRecovery &&
        !isActive(snoozedUntilMillis, nowMillis)
}
