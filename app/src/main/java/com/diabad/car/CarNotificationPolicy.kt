package com.diabad.car

import com.diabad.domain.model.AppSettings
import com.diabad.domain.model.GlucoseReading

/** Repost on a new sample or once on data loss, never on every age tick. */
class CarNotificationPolicy {
    private var lastKey: String? = null

    fun reset() { lastKey = null }

    fun shouldPost(connected: Boolean, reading: GlucoseReading?, settings: AppSettings, now: Long): Boolean {
        if (!connected || !settings.carNotificationsEnabled || reading == null) {
            lastKey = null
            return false
        }
        val stale = now - reading.timestampMillis >= settings.connectionLossGraceMinutes * 60_000L
        val key = "${reading.timestampMillis}|${reading.mmol}|${reading.trend}|$stale"
        if (key == lastKey) return false
        lastKey = key
        return true
    }
}
