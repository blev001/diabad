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

/** Previous sample in an ascending-by-time history list. */
fun previousReading(
    latest: GlucoseReading?,
    historyAscending: List<GlucoseReading>,
): GlucoseReading? {
    if (latest == null || historyAscending.isEmpty()) return null
    val index = historyAscending.indexOfLast { it.timestampMillis == latest.timestampMillis }
    return when {
        index > 0 -> historyAscending[index - 1]
        index < 0 -> historyAscending.last()
        else -> null
    }
}
