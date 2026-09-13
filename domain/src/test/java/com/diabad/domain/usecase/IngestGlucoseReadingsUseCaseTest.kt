package com.diabad.domain.usecase

import com.diabad.domain.model.GlucoseReading
import com.diabad.domain.model.GlucoseSource
import com.diabad.domain.model.TrendArrow
import com.diabad.domain.repository.GlucoseRepository
import com.diabad.domain.signal.GlucoseSignalClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IngestGlucoseReadingsUseCaseTest {

    @Test
    fun persistsFirstSample() {
        val stored = reading(5.4, 1_000L)
        assertTrue(IngestGlucoseReadingsUseCase.shouldPersist(stored, latestStored = null))
    }

    @Test
    fun skipsIdenticalTimestamp() {
        val first = reading(5.4, 1_000L)
        assertFalse(IngestGlucoseReadingsUseCase.shouldPersist(first, first))
    }

    @Test
    fun skipsSameValueWithinFourMinutes() {
        val first = reading(5.4, 10_000L)
        val spam = reading(5.4, 10_000L + 30_000L)
        assertFalse(IngestGlucoseReadingsUseCase.shouldPersist(spam, first))
    }

    @Test
    fun persistsSameValueAfterFourMinutes() {
        val first = reading(5.4, 10_000L)
        val later = reading(5.4, 10_000L + 4L * 60_000L)
        assertTrue(IngestGlucoseReadingsUseCase.shouldPersist(later, first))
    }

    @Test
    fun persistsValueChangeImmediately() {
        val first = reading(5.4, 10_000L)
        val drop = reading(3.6, 10_000L + 5_000L, TrendArrow.SINGLE_DOWN)
        assertTrue(IngestGlucoseReadingsUseCase.shouldPersist(drop, first))
    }

    @Test
    fun notificationSpamDoesNotWriteOrPruneTwice() = runBlocking {
        val repo = FakeGlucoseRepository()
        val clock = GlucoseSignalClock()
        var now = 1_700_000_000_000L
        val useCase = IngestGlucoseReadingsUseCase(repo, clock) { now }

        useCase(listOf(reading(5.4, now)))
        assertEquals(1, repo.stored.size)
        assertEquals(1, repo.pruneCount)
        assertTrue(clock.lastSignalMillis() >= now)

        now += 15_000L
        useCase(listOf(reading(5.4, now)))
        assertEquals(1, repo.stored.size)
        assertEquals(1, repo.pruneCount)

        now += 4L * 60_000L
        useCase(listOf(reading(5.4, now)))
        assertEquals(2, repo.stored.size)
        assertEquals(1, repo.pruneCount)
        assertEquals(1, repo.latestReads)
    }

    @Test
    fun hypoChangeIsNeverDropped() = runBlocking {
        val repo = FakeGlucoseRepository()
        val useCase = IngestGlucoseReadingsUseCase(repo, GlucoseSignalClock()) { 0L }
        useCase(listOf(reading(5.4, 1_000L)))
        useCase(listOf(reading(3.5, 2_000L, TrendArrow.DOUBLE_DOWN)))
        assertEquals(2, repo.stored.size)
        assertEquals(3.5, repo.stored.maxBy { it.timestampMillis }.mmol, 0.001)
    }

    private fun reading(
        mmol: Double,
        timestamp: Long,
        trend: TrendArrow = TrendArrow.FLAT,
    ) = GlucoseReading(
        mmol = mmol,
        timestampMillis = timestamp,
        trend = trend,
        source = GlucoseSource.OTTAI,
    )

    private class FakeGlucoseRepository : GlucoseRepository {
        val stored = mutableListOf<GlucoseReading>()
        var pruneCount = 0
        var latestReads = 0

        override fun observeLatest(): Flow<GlucoseReading?> =
            MutableStateFlow(stored.maxByOrNull { it.timestampMillis })

        override fun observePrevious(): Flow<GlucoseReading?> = MutableStateFlow(null)

        override fun observeHistory(): Flow<List<GlucoseReading>> = MutableStateFlow(stored.toList())

        override suspend fun getLatest(): GlucoseReading? {
            latestReads++
            return stored.maxByOrNull { it.timestampMillis }
        }

        override suspend fun ingest(readings: List<GlucoseReading>) {
            readings.forEach { reading ->
                stored.removeAll { it.timestampMillis == reading.timestampMillis }
                stored.add(reading)
            }
        }

        override suspend fun pruneOlderThan(cutoffMillis: Long) {
            pruneCount++
            stored.removeAll { it.timestampMillis < cutoffMillis }
        }
    }
}
