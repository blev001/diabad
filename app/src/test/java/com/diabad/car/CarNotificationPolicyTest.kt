package com.diabad.car

import com.diabad.domain.model.AppSettings
import com.diabad.domain.model.GlucoseReading
import com.diabad.domain.model.TrendArrow
import org.junit.Assert.*
import org.junit.Test

class CarNotificationPolicyTest {
    private val now = 1_000_000L
    private val reading = GlucoseReading(5.5, now, TrendArrow.FLAT)
    private val settings = AppSettings(carNotificationsEnabled = true)

    @Test fun requiresOptInAndCarConnectionAndReading() {
        val policy = CarNotificationPolicy()
        assertFalse(policy.shouldPost(false, reading, settings, now))
        assertFalse(policy.shouldPost(true, reading, AppSettings(), now))
        assertFalse(policy.shouldPost(true, null, settings, now))
        assertTrue(policy.shouldPost(true, reading, settings, now))
    }

    @Test fun ageTicksDoNotRepeatedlyAlertButNewSampleDoes() {
        val policy = CarNotificationPolicy()
        assertTrue(policy.shouldPost(true, reading, settings, now))
        assertFalse(policy.shouldPost(true, reading, settings, now + 60_000))
        assertTrue(policy.shouldPost(true, reading.copy(timestampMillis = now + 60_000), settings, now + 60_000))
    }

    @Test fun dataLossIsReportedOnceAtConfiguredBoundary() {
        val policy = CarNotificationPolicy()
        val prefs = settings.copy(connectionLossGraceMinutes = 5)
        assertTrue(policy.shouldPost(true, reading, prefs, now))
        assertFalse(policy.shouldPost(true, reading, prefs, now + 299_999))
        assertTrue(policy.shouldPost(true, reading, prefs, now + 300_000))
        assertFalse(policy.shouldPost(true, reading, prefs, now + 360_000))
    }

    @Test fun reconnectShowsCurrentSample() {
        val policy = CarNotificationPolicy()
        assertTrue(policy.shouldPost(true, reading, settings, now))
        assertFalse(policy.shouldPost(false, reading, settings, now))
        assertTrue(policy.shouldPost(true, reading, settings, now))
    }

    @Test fun toggleOffThenOnRestoresNotification() {
        val policy = CarNotificationPolicy()
        assertTrue(policy.shouldPost(true, reading, settings, now))
        assertFalse(policy.shouldPost(true, reading, settings.copy(carNotificationsEnabled = false), now))
        assertTrue(policy.shouldPost(true, reading, settings, now))
    }
}
