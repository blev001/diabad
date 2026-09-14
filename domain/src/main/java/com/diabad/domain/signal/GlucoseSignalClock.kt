package com.diabad.domain.signal

import java.util.concurrent.atomic.AtomicLong

/**
 * Last time OtTai was seen alive (broadcast or notification), independent of
 * whether the glucose number itself changed. Used for connection-loss only.
 */
class GlucoseSignalClock {
    private val lastSignalMillis = AtomicLong(0L)

    fun mark(atMillis: Long = System.currentTimeMillis()) {
        lastSignalMillis.updateAndGet { current -> maxOf(current, atMillis) }
    }

    fun lastSignalMillis(): Long = lastSignalMillis.get()
}
