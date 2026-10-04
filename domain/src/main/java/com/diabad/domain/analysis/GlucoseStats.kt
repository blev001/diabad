package com.diabad.domain.analysis

import com.diabad.core.glucose.mmolToMgdl
import com.diabad.domain.model.GlucoseReading
import com.diabad.domain.model.GlucoseZone
import kotlin.math.sqrt

/**
 * Standard CGM report metrics (International Consensus on Time in Range, 2019).
 * Percentages are shares of readings, 0..100.
 */
data class GlucoseStats(
    val readingCount: Int,
    val meanMmol: Double,
    val sdMmol: Double,
    /** Coefficient of variation, %. Target ≤ 36. */
    val cvPercent: Double,
    /** Glucose Management Indicator — estimated HbA1c, %. */
    val gmiPercent: Double,
    val veryLowPercent: Double,
    val lowPercent: Double,
    val inRangePercent: Double,
    val highPercent: Double,
    val veryHighPercent: Double,
    /** Share of the period covered by sensor data, %. */
    val coveragePercent: Double,
) {
    companion object {
        const val RANGE_LOW_MMOL = 3.9
        const val RANGE_HIGH_MMOL = 10.0

        /**
         * @param expectedIntervalMinutes typical sensor cadence; used only for [coveragePercent].
         */
        fun compute(
            readings: List<GlucoseReading>,
            periodMillis: Long,
            expectedIntervalMinutes: Double = estimateIntervalMinutes(readings),
        ): GlucoseStats? {
            if (readings.isEmpty()) return null
            val values = readings.map { it.mmol }
            val n = values.size
            val mean = values.average()
            val sd = if (n > 1) sqrt(values.sumOf { (it - mean) * (it - mean) } / (n - 1)) else 0.0

            fun pct(predicate: (Double) -> Boolean) = values.count(predicate) * 100.0 / n

            val expected = if (expectedIntervalMinutes > 0) {
                periodMillis / (expectedIntervalMinutes * MS_PER_MINUTE)
            } else {
                0.0
            }
            val coverage = if (expected > 0) (n / expected * 100.0).coerceAtMost(100.0) else 0.0

            return GlucoseStats(
                readingCount = n,
                meanMmol = mean,
                sdMmol = sd,
                cvPercent = if (mean > 0) sd / mean * 100.0 else 0.0,
                gmiPercent = gmiFromMeanMmol(mean),
                veryLowPercent = pct { it < GlucoseZone.VERY_LOW_MMOL },
                lowPercent = pct { it >= GlucoseZone.VERY_LOW_MMOL && it < RANGE_LOW_MMOL },
                inRangePercent = pct { it >= RANGE_LOW_MMOL && it <= RANGE_HIGH_MMOL },
                highPercent = pct { it > RANGE_HIGH_MMOL && it <= GlucoseZone.VERY_HIGH_MMOL },
                veryHighPercent = pct { it > GlucoseZone.VERY_HIGH_MMOL },
                coveragePercent = coverage,
            )
        }

        /** GMI (%) = 3.31 + 0.02392 × mean glucose (mg/dL). Bergenstal et al., 2018. */
        fun gmiFromMeanMmol(meanMmol: Double): Double = 3.31 + 0.02392 * mmolToMgdl(meanMmol)

        /** Median gap between consecutive readings, ignoring gaps longer than 30 min. */
        fun estimateIntervalMinutes(readings: List<GlucoseReading>): Double {
            if (readings.size < 2) return 5.0
            val sorted = readings.map { it.timestampMillis }.sorted()
            val gaps = sorted.zipWithNext { a, b -> (b - a) / MS_PER_MINUTE.toDouble() }
                .filter { it > 0 && it <= 30 }
                .sorted()
            if (gaps.isEmpty()) return 5.0
            return gaps[gaps.size / 2]
        }
    }
}

/** One hour-of-day bucket of the Ambulatory Glucose Profile. */
data class AgpBucket(
    val hour: Int,
    val p5: Double,
    val p25: Double,
    val p50: Double,
    val p75: Double,
    val p95: Double,
)

object AgpProfile {
    /**
     * Groups readings by local hour of day and returns percentiles.
     * [hourOf] converts a timestamp to 0..23 in the user's time zone.
     */
    fun compute(readings: List<GlucoseReading>, hourOf: (Long) -> Int): List<AgpBucket> =
        readings.groupBy { hourOf(it.timestampMillis) }
            .toSortedMap()
            .mapNotNull { (hour, list) ->
                if (list.size < 3) return@mapNotNull null
                val sorted = list.map { it.mmol }.sorted()
                AgpBucket(
                    hour = hour,
                    p5 = percentile(sorted, 0.05),
                    p25 = percentile(sorted, 0.25),
                    p50 = percentile(sorted, 0.50),
                    p75 = percentile(sorted, 0.75),
                    p95 = percentile(sorted, 0.95),
                )
            }

    /** Linear interpolation between closest ranks; [sorted] must be ascending. */
    fun percentile(sorted: List<Double>, p: Double): Double {
        if (sorted.isEmpty()) return Double.NaN
        if (sorted.size == 1) return sorted.first()
        val pos = p.coerceIn(0.0, 1.0) * (sorted.size - 1)
        val lo = pos.toInt()
        val hi = (lo + 1).coerceAtMost(sorted.lastIndex)
        val frac = pos - lo
        return sorted[lo] + (sorted[hi] - sorted[lo]) * frac
    }
}
