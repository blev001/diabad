package com.diabad.domain.model

/**
 * CGM-oriented glucose bands aligned with ADA Standards of Care
 * (Time in Range 3.9–10.0 mmol/L; Level 1 hypo &lt; 3.9; Level 2 hypo &lt; 3.0;
 * very high typically &gt; 13.9 mmol/L / 250 mg/dL).
 *
 * Alarm edges use the user's [hypoThresholdMmol] / [hyperThresholdMmol];
 * VERY_LOW / VERY_HIGH keep fixed clinical cutoffs.
 */
enum class GlucoseZone {
    UNKNOWN,
    VERY_LOW,
    LOW,
    IN_RANGE,
    HIGH,
    VERY_HIGH;

    val isAlarmBand: Boolean
        get() = this == VERY_LOW || this == LOW || this == HIGH || this == VERY_HIGH

    companion object {
        /** ADA Level 2 hypoglycemia. */
        const val VERY_LOW_MMOL = 3.0

        /** ADA CGM Time-Above-Range “very high” (~250 mg/dL). */
        const val VERY_HIGH_MMOL = 13.9

        fun classify(
            mmol: Double?,
            hypoThresholdMmol: Double,
            hyperThresholdMmol: Double,
        ): GlucoseZone {
            if (mmol == null) return UNKNOWN
            val hypo = hypoThresholdMmol.coerceAtMost(hyperThresholdMmol)
            val hyper = hyperThresholdMmol.coerceAtLeast(hypo)
            return when {
                mmol < VERY_LOW_MMOL -> VERY_LOW
                mmol < hypo -> LOW
                mmol > VERY_HIGH_MMOL -> VERY_HIGH
                mmol > hyper -> HIGH
                else -> IN_RANGE
            }
        }
    }
}

enum class GlucoseAlarmKind {
    HYPO,
    HYPER,
}

fun AppSettings.alarmKindFor(mmol: Double): GlucoseAlarmKind? = when {
    mmol < hypoThresholdMmol -> GlucoseAlarmKind.HYPO
    mmol > hyperThresholdMmol -> GlucoseAlarmKind.HYPER
    else -> null
}

fun AppSettings.isOutOfAlarmRange(mmol: Double): Boolean =
    alarmKindFor(mmol) != null

/** One band in the on-screen sugar-level guide. */
data class GlucoseGuideBand(
    val zone: GlucoseZone,
    val kind: Kind,
    val firstMmol: Double,
    val secondMmol: Double? = null,
) {
    enum class Kind { BELOW, BETWEEN, ABOVE }
}

fun glucoseGuideBands(
    hypoThresholdMmol: Double,
    hyperThresholdMmol: Double,
): List<GlucoseGuideBand> {
    val hypo = minOf(hypoThresholdMmol, hyperThresholdMmol)
    val hyper = maxOf(hypoThresholdMmol, hyperThresholdMmol)
    return listOf(
        GlucoseGuideBand(GlucoseZone.VERY_LOW, GlucoseGuideBand.Kind.BELOW, GlucoseZone.VERY_LOW_MMOL),
        GlucoseGuideBand(GlucoseZone.LOW, GlucoseGuideBand.Kind.BETWEEN, GlucoseZone.VERY_LOW_MMOL, hypo),
        GlucoseGuideBand(GlucoseZone.IN_RANGE, GlucoseGuideBand.Kind.BETWEEN, hypo, hyper),
        GlucoseGuideBand(GlucoseZone.HIGH, GlucoseGuideBand.Kind.BETWEEN, hyper, GlucoseZone.VERY_HIGH_MMOL),
        GlucoseGuideBand(GlucoseZone.VERY_HIGH, GlucoseGuideBand.Kind.ABOVE, GlucoseZone.VERY_HIGH_MMOL),
    )
}
