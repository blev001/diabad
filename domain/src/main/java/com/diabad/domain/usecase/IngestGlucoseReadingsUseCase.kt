package com.diabad.domain.usecase

import com.diabad.domain.model.GlucoseReading
import com.diabad.domain.repository.GlucoseRepository

class IngestGlucoseReadingsUseCase(
    private val glucoseRepository: GlucoseRepository,
) {
    suspend operator fun invoke(readings: List<GlucoseReading>) {
        if (readings.isEmpty()) return
        glucoseRepository.ingest(readings)
        val cutoff = System.currentTimeMillis() - RETENTION_MS
        glucoseRepository.pruneOlderThan(cutoff)
    }

    companion object {
        const val RETENTION_MS: Long = 24L * 60L * 60L * 1000L
    }
}
