package com.diabad.domain.model

/**
 * Why the alarm is (or would be) ringing. Higher [severity] wins and may break
 * through an earlier Stop / Snooze of a milder reason.
 */
enum class AlarmReason(val severity: Int) {
    /** Below min(3.0, hypo threshold) — ADA Level 2. Never waits for the vibrate-only stage. */
    URGENT_LOW(6),
    LOW(5),
    PREDICTED_LOW(4),
    SIGNAL_LOSS(3),
    FAST_DROP(2),
    HIGH(1);

    val isLowFamily: Boolean
        get() = this == URGENT_LOW || this == LOW || this == PREDICTED_LOW || this == FAST_DROP
}
