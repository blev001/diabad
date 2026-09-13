package com.diabad.domain.usecase

import com.diabad.domain.model.GlucoseReading
import com.diabad.domain.repository.GlucoseRepository
import com.diabad.domain.signal.GlucoseSignalClock
import kotlin.math.abs

/**
 * Persists CGM samples and ignores OtTai notification spam.
 *
 * OtTai refreshes its status-bar notification far more often than the sensor
 * produces a new point. Stamping each refresh with "now" used to write a new
 * Room row, rebuild the phone notification, and urgently wake the watch.
 */
class IngestGlucoseReadingsUseCase(
    private val glucoseRepository: GlucoseRepository,
    private val signalClock: GlucoseSignalClock,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) {
    @Volatile
    private var lastPruneAtMillis: Long = 0L

    @Volatile
    private var cachedLatest: GlucoseReading? = null

    suspend operator fun invoke(readings: List<GlucoseReading>) {
        if (readings.isEmpty()) return
        signalClock.mark(nowMillis())

        val accepted = LinkedHashMap<Long, GlucoseReading>()
        var latestKept = cachedLatest ?: glucoseRepository.getLatest()?.also { cachedLatest = it }
        for (reading in readings.sortedBy { it.timestampMillis }) {
            if (!shouldPersist(reading, latestKept)) continue
            accepted[reading.timestampMillis] = reading
            val candidate = reading
            if (latestKept == null || candidate.timestampMillis >= latestKept.timestampMillis) {
                latestKept = candidate
            }
        }

        if (accepted.isNotEmpty()) {
            glucoseRepository.ingest(accepted.values.toList())
            cachedLatest = latestKept
        }

        val now = nowMillis()
        if (now - lastPruneAtMillis >= PRUNE_INTERVAL_MS) {
            lastPruneAtMillis = now
            glucoseRepository.pruneOlderThan(now - RETENTION_MS)
        }
    }

    companion object {
        const val RETENTION_MS: Long = 24L * 60L * 60L * 1000L
        const val PRUNE_INTERVAL_MS: Long = 60L * 60L * 1000L
        const val MIN_SAMPLE_GAP_MS: Long = 4L * 60L * 1000L
        const val VALUE_EPS: Double = 0.05

        internal fun shouldPersist(
            candidate: GlucoseReading,
            latestStored: GlucoseReading?,
        ): Boolean {
            if (latestStored == null) return true
            if (candidate.timestampMillis == latestStored.timestampMillis) return false
            val sameValue = abs(candidate.mmol - latestStored.mmol) < VALUE_EPS &&
                candidate.trend == latestStored.trend
            if (sameValue) {
                return abs(candidate.timestampMillis - latestStored.timestampMillis) >=
                    MIN_SAMPLE_GAP_MS
            }
            return true
        }
    }
}
