package com.diabad.data.repository

import com.diabad.data.local.db.GlucoseDao
import com.diabad.data.local.db.toDomain
import com.diabad.data.local.db.toEntity
import com.diabad.domain.model.GlucoseReading
import com.diabad.domain.repository.GlucoseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlucoseRepositoryImpl @Inject constructor(
    private val glucoseDao: GlucoseDao,
) : GlucoseRepository {

    override fun observeLatest(): Flow<GlucoseReading?> =
        glucoseDao.observeLatest().map { it?.toDomain() }.distinctUntilChanged()

    override fun observePrevious(): Flow<GlucoseReading?> =
        glucoseDao.observePrevious().map { it?.toDomain() }.distinctUntilChanged()

    override fun observeHistory(): Flow<List<GlucoseReading>> =
        glucoseDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getLatest(): GlucoseReading? =
        glucoseDao.latest()?.toDomain()

    override suspend fun ingest(readings: List<GlucoseReading>) {
        if (readings.isEmpty()) return
        glucoseDao.upsertAll(readings.map { it.toEntity() })
    }

    override suspend fun pruneOlderThan(cutoffMillis: Long) {
        glucoseDao.deleteOlderThan(cutoffMillis)
    }
}
