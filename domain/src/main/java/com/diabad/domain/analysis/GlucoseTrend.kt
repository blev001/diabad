package com.diabad.domain.analysis

import com.diabad.domain.model.GlucoseReading

/** Rate of change from a least-squares fit over the last few readings. */
data class GlucoseTrend(
    /** mmol/L per minute; negative = falling. */
    val ratePerMinute: Double,
    val latestMmol: Double,
    val latestTimestampMillis: Long,
) {
    fun predictMmol(minutesAhead: Int): Double = latestMmol + ratePerMinute * minutesAhead

    companion object {
        const val WINDOW_MINUTES = 15
        private const val MIN_POINTS = 3
        private const val MIN_SPAN_MINUTES = 8.0

        /**
         * Returns null when there is not enough recent data for a trustworthy slope:
         * fewer than [MIN_POINTS] readings or less than [MIN_SPAN_MINUTES] covered
         * within [WINDOW_MINUTES] before the latest reading.
         */
        fun from(readings: List<GlucoseReading>): GlucoseTrend? {
            val latest = readings.maxByOrNull { it.timestampMillis } ?: return null
            val windowStart = latest.timestampMillis - WINDOW_MINUTES * MS_PER_MINUTE
            val window = readings.filter { it.timestampMillis in windowStart..latest.timestampMillis }
            if (window.size < MIN_POINTS) return null

            val xs = window.map { (it.timestampMillis - latest.timestampMillis) / MS_PER_MINUTE.toDouble() }
            val span = xs.max() - xs.min()
            if (span < MIN_SPAN_MINUTES) return null

            val ys = window.map { it.mmol }
            val meanX = xs.average()
            val meanY = ys.average()
            var num = 0.0
            var den = 0.0
            for (i in xs.indices) {
                val dx = xs[i] - meanX
                num += dx * (ys[i] - meanY)
                den += dx * dx
            }
            if (den == 0.0) return null
            return GlucoseTrend(
                ratePerMinute = num / den,
                latestMmol = latest.mmol,
                latestTimestampMillis = latest.timestampMillis,
            )
        }
    }
}

internal const val MS_PER_MINUTE = 60_000L
