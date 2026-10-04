package com.diabad.domain.alarm

import com.diabad.domain.analysis.GlucoseTrend
import com.diabad.domain.model.AlarmReason
import com.diabad.domain.model.AlarmState
import com.diabad.domain.model.AppSettings
import com.diabad.domain.model.ConnectionLossMode
import com.diabad.domain.model.GlucoseReading
import com.diabad.domain.model.GlucoseZone
import kotlin.math.min

/** What is wrong right now, independent of Stop / Snooze. */
data class AlarmCondition(
    val reason: AlarmReason,
    val latest: GlucoseReading,
    val thresholdMmol: Double? = null,
    val predictedMmol: Double? = null,
    val ratePerMinute: Double? = null,
    val dataAgeMinutes: Long = 0L,
)

enum class AlarmStatus { IDLE, RINGING, SNOOZED, DISMISSED }

/** How loud the phone side should be. The watch vibrates for both VIBRATE and SOUND. */
enum class AlarmOutput { NONE, VIBRATE, SOUND }

data class AlarmDecision(
    val condition: AlarmCondition?,
    val status: AlarmStatus,
    val output: AlarmOutput,
    /** Fire a one-shot gentle reminder now (signal loss in REMIND mode). */
    val remind: Boolean,
    val state: AlarmState,
    /** Re-evaluate at this time even if no new reading arrives. */
    val nextWakeMillis: Long?,
)

/**
 * Pure alarm rules. Callers pass wall-clock [now] and local [minuteOfDay] so the
 * logic is deterministic in tests.
 */
object AlarmPolicy {
    const val REMIND_INTERVAL_MS = 15 * 60_000L

    /** ≈ 3 mg/dL/min — the CGM "double down" arrow. */
    const val FAST_DROP_RATE_PER_MINUTE = -0.17

    /** A stopped low alarm comes back if glucose falls this much further. */
    const val DEEPER_LOW_DELTA_MMOL = 0.5

    fun evaluate(
        now: Long,
        minuteOfDay: Int,
        readings: List<GlucoseReading>,
        settings: AppSettings,
        state: AlarmState,
    ): AlarmDecision {
        val detection = detect(now, minuteOfDay, readings, settings)
        val wakes = mutableListOf<Long>()
        detection.recheckAtMillis?.let(wakes::add)
        val condition = detection.condition
            ?: return AlarmDecision(
                condition = null,
                status = AlarmStatus.IDLE,
                output = AlarmOutput.NONE,
                remind = false,
                state = AlarmState(),
                nextWakeMillis = wakes.minOrNull(),
            )
        val reason = condition.reason
        var s = state

        fun quiet(status: AlarmStatus, remind: Boolean = false) = AlarmDecision(
            condition = condition,
            status = status,
            output = AlarmOutput.NONE,
            remind = remind,
            state = s.copy(ringingSinceMillis = 0L, ringingReason = null),
            nextWakeMillis = wakes.minOrNull(),
        )

        if (reason == AlarmReason.SIGNAL_LOSS && settings.connectionLossMode == ConnectionLossMode.REMIND) {
            val due = now - s.lastRemindAtMillis >= REMIND_INTERVAL_MS
            if (due) s = s.copy(lastRemindAtMillis = now)
            wakes += s.lastRemindAtMillis + REMIND_INTERVAL_MS
            return quiet(AlarmStatus.IDLE, remind = due)
        }

        val vibrateFirstMs = settings.vibrateFirstSeconds.coerceAtLeast(0) * 1000L
        var repeatRing = false

        if (s.dismissedAtMillis > 0L) {
            val dismissedReason = s.dismissedReason
            val dismissedMmol = s.dismissedMmol
            val worse = dismissedReason == null || reason.severity > dismissedReason.severity
            val deeper = reason.isLowFamily &&
                dismissedReason?.isLowFamily == true &&
                dismissedMmol != null &&
                condition.latest.mmol <= dismissedMmol - DEEPER_LOW_DELTA_MMOL
            val reAlarmAt = if (settings.reAlarmMinutes > 0 && reason != AlarmReason.HIGH) {
                s.dismissedAtMillis + settings.reAlarmMinutes * MS_PER_MINUTE
            } else {
                null
            }
            val expired = reAlarmAt != null && now >= reAlarmAt
            if (!worse && !deeper && !expired) {
                reAlarmAt?.let(wakes::add)
                return quiet(AlarmStatus.DISMISSED)
            }
            s = s.copy(dismissedAtMillis = 0L, dismissedReason = null, dismissedMmol = null)
            repeatRing = true
        }

        if (s.snoozedUntilMillis > 0L) {
            val snoozedReason = s.snoozedReason
            val worse = snoozedReason != null && reason.severity > snoozedReason.severity
            if (now < s.snoozedUntilMillis && !worse) {
                wakes += s.snoozedUntilMillis
                return quiet(AlarmStatus.SNOOZED)
            }
            s = s.copy(snoozedUntilMillis = 0L, snoozedReason = null)
            repeatRing = true
        }

        val since = when {
            s.ringingSinceMillis > 0L -> s.ringingSinceMillis
            repeatRing -> now - vibrateFirstMs
            else -> now
        }
        s = s.copy(ringingSinceMillis = since, ringingReason = reason)

        val soundAt = since + vibrateFirstMs
        val output = when {
            reason == AlarmReason.HIGH && settings.hyperVibrateOnly -> AlarmOutput.VIBRATE
            reason == AlarmReason.URGENT_LOW -> AlarmOutput.SOUND
            now >= soundAt -> AlarmOutput.SOUND
            else -> {
                wakes += soundAt
                AlarmOutput.VIBRATE
            }
        }
        return AlarmDecision(
            condition = condition,
            status = AlarmStatus.RINGING,
            output = output,
            remind = false,
            state = s,
            nextWakeMillis = wakes.minOrNull(),
        )
    }

    fun snooze(state: AlarmState, now: Long, minutes: Int, reason: AlarmReason?): AlarmState =
        state.copy(
            snoozedUntilMillis = now + minutes.coerceIn(1, 120) * MS_PER_MINUTE,
            snoozedReason = reason ?: state.ringingReason,
            dismissedAtMillis = 0L,
            dismissedReason = null,
            dismissedMmol = null,
            ringingSinceMillis = 0L,
            ringingReason = null,
        )

    fun dismiss(state: AlarmState, now: Long, reason: AlarmReason?, mmol: Double?): AlarmState =
        state.copy(
            dismissedAtMillis = now,
            dismissedReason = reason ?: state.ringingReason,
            dismissedMmol = mmol,
            snoozedUntilMillis = 0L,
            snoozedReason = null,
            ringingSinceMillis = 0L,
            ringingReason = null,
        )

    private data class Detection(val condition: AlarmCondition?, val recheckAtMillis: Long?)

    private fun detect(
        now: Long,
        minuteOfDay: Int,
        readings: List<GlucoseReading>,
        settings: AppSettings,
    ): Detection {
        val latest = readings.maxByOrNull { it.timestampMillis } ?: return Detection(null, null)
        val ageMs = (now - latest.timestampMillis).coerceAtLeast(0L)
        val ageMinutes = ageMs / MS_PER_MINUTE
        val graceMs = settings.connectionLossGraceMinutes.coerceAtLeast(1) * MS_PER_MINUTE
        val stale = ageMs >= graceMs
        val hypo = settings.effectiveHypoThreshold(minuteOfDay)
        val hyper = settings.effectiveHyperThreshold(minuteOfDay)
        val urgent = min(GlucoseZone.VERY_LOW_MMOL, hypo)

        fun found(
            reason: AlarmReason,
            threshold: Double? = null,
            predicted: Double? = null,
            rate: Double? = null,
            recheckAt: Long? = null,
        ) = Detection(
            AlarmCondition(reason, latest, threshold, predicted, rate, ageMinutes),
            recheckAt,
        )

        // A low reading keeps alarming even when the sensor goes quiet afterwards.
        if (latest.mmol < urgent) return found(AlarmReason.URGENT_LOW, threshold = urgent)
        if (latest.mmol < hypo) return found(AlarmReason.LOW, threshold = hypo)

        // A high reading keeps its alarm after the sensor goes quiet, same as a low.
        // Signal-loss is only for an in-range reading; ConnectionLossMonitor owns that alert.
        val staleAt = latest.timestampMillis + graceMs
        if (settings.hyperAlarmActive(minuteOfDay) && latest.mmol > hyper) {
            val highSince = highRunStart(readings, hyper)
            val ringAt = highSince + settings.hyperDelayMinutes.coerceAtLeast(0) * MS_PER_MINUTE
            if (now >= ringAt) {
                return found(AlarmReason.HIGH, threshold = hyper, recheckAt = staleAt)
            }
            return Detection(null, min(ringAt, staleAt))
        }

        if (stale) {
            return if (settings.connectionLossMode == ConnectionLossMode.SILENT) {
                Detection(null, null)
            } else {
                found(AlarmReason.SIGNAL_LOSS)
            }
        }
        val trend = GlucoseTrend.from(readings)
        if (trend != null && trend.ratePerMinute < 0) {
            if (settings.predictiveLowEnabled) {
                val predicted = trend.predictMmol(settings.predictiveLowMinutes)
                if (predicted < hypo) {
                    return found(
                        AlarmReason.PREDICTED_LOW,
                        threshold = hypo,
                        predicted = predicted,
                        rate = trend.ratePerMinute,
                        recheckAt = staleAt,
                    )
                }
            }
            if (settings.fastDropEnabled && trend.ratePerMinute <= FAST_DROP_RATE_PER_MINUTE) {
                return found(AlarmReason.FAST_DROP, rate = trend.ratePerMinute, recheckAt = staleAt)
            }
        }

        return Detection(null, staleAt)
    }

    /** Timestamp of the oldest reading in the unbroken run above [hyper] ending at the latest one. */
    private fun highRunStart(readings: List<GlucoseReading>, hyper: Double): Long {
        var since = Long.MAX_VALUE
        for (r in readings.sortedByDescending { it.timestampMillis }) {
            if (r.mmol <= hyper) break
            since = r.timestampMillis
        }
        return since
    }

    private const val MS_PER_MINUTE = 60_000L
}
