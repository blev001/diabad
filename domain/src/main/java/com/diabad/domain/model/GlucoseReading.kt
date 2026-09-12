package com.diabad.domain.model

/**
 * Single CGM sample stored and shown in mmol/L.
 */
data class GlucoseReading(
    val mmol: Double,
    val timestampMillis: Long,
    val trend: TrendArrow,
    val source: GlucoseSource = GlucoseSource.OTTAI,
)

enum class GlucoseSource {
    OTTAI,
}
