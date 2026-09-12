package com.diabad.domain.repository

import com.diabad.domain.model.GlucoseReading
import kotlinx.coroutines.flow.Flow

interface GlucoseRepository {
    /** Latest reading, or null if none yet. */
    fun observeLatest(): Flow<GlucoseReading?>

    /** Readings within the rolling retention window (24h). */
    fun observeHistory(): Flow<List<GlucoseReading>>

    suspend fun ingest(readings: List<GlucoseReading>)

    suspend fun pruneOlderThan(cutoffMillis: Long)
}
