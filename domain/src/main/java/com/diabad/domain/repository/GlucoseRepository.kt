package com.diabad.domain.repository

import com.diabad.domain.model.GlucoseReading
import kotlinx.coroutines.flow.Flow

interface GlucoseRepository {
    /** Latest reading, or null if none yet. */
    fun observeLatest(): Flow<GlucoseReading?>

    /** Sample immediately before the latest, for delta. */
    fun observePrevious(): Flow<GlucoseReading?>

    /** Readings within the rolling retention window, oldest first. */
    fun observeHistory(): Flow<List<GlucoseReading>>

    /** Up to [limit] newest readings, oldest first. Used by the alarm trend window. */
    fun observeRecent(limit: Int): Flow<List<GlucoseReading>>

    suspend fun getRange(fromMillis: Long, toMillis: Long): List<GlucoseReading>

    suspend fun getLatest(): GlucoseReading?

    suspend fun ingest(readings: List<GlucoseReading>)

    suspend fun pruneOlderThan(cutoffMillis: Long)
}
