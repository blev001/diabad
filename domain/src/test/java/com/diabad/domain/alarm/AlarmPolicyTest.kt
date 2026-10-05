package com.diabad.domain.alarm

import com.diabad.domain.model.AlarmReason
import com.diabad.domain.model.AlarmState
import com.diabad.domain.model.AppSettings
import com.diabad.domain.model.ConnectionLossMode
import com.diabad.domain.model.GlucoseAlarmKind
import com.diabad.domain.model.GlucoseReading
import com.diabad.domain.model.TrendArrow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmPolicyTest {

    private val t0 = 1_700_000_000_000L
    private val noon = 12 * 60
    private val base = AppSettings(
        vibrateFirstSeconds = 0,
        predictiveLowEnabled = false,
        fastDropEnabled = false,
    )

    private fun min(m: Int) = m * 60_000L

    private fun reading(minute: Int, mmol: Double) =
        GlucoseReading(mmol = mmol, timestampMillis = t0 + min(minute), trend = TrendArrow.FLAT)

    /** One reading per minute; last one at [endMinute]. */
    private fun series(endMinute: Int, vararg values: Double): List<GlucoseReading> =
        values.mapIndexed { i, v -> reading(endMinute - (values.size - 1 - i), v) }

    private fun eval(
        nowMinute: Int,
        readings: List<GlucoseReading>,
        settings: AppSettings = base,
        state: AlarmState = AlarmState(),
        minuteOfDay: Int = noon,
    ) = AlarmPolicy.evaluate(t0 + min(nowMinute), minuteOfDay, readings, settings, state)

    @Test
    fun `no data means no alarm`() {
        val d = eval(0, emptyList())
        assertEquals(AlarmStatus.IDLE, d.status)
        assertNull(d.condition)
    }

    @Test
    fun `in range is idle and schedules a stale check`() {
        val d = eval(0, listOf(reading(0, 6.0)))
        assertEquals(AlarmStatus.IDLE, d.status)
        assertEquals(t0 + min(base.connectionLossGraceMinutes), d.nextWakeMillis)
    }

    @Test
    fun `low rings with sound when vibrate stage is off`() {
        val d = eval(0, listOf(reading(0, 3.5)))
        assertEquals(AlarmReason.LOW, d.condition?.reason)
        assertEquals(AlarmStatus.RINGING, d.status)
        assertEquals(AlarmOutput.SOUND, d.output)
    }

    @Test
    fun `below 3_0 is urgent and skips vibrate stage`() {
        val d = eval(0, listOf(reading(0, 2.8)), base.copy(vibrateFirstSeconds = 120))
        assertEquals(AlarmReason.URGENT_LOW, d.condition?.reason)
        assertEquals(AlarmOutput.SOUND, d.output)
    }

    @Test
    fun `first ring vibrates then escalates to sound`() {
        val settings = base.copy(vibrateFirstSeconds = 60)
        val readings = listOf(reading(0, 3.5))
        val first = eval(0, readings, settings)
        assertEquals(AlarmOutput.VIBRATE, first.output)
        assertEquals(t0 + min(1), first.nextWakeMillis)

        val later = eval(1, readings, settings, first.state)
        assertEquals(AlarmOutput.SOUND, later.output)
    }

    @Test
    fun `dismissed low stays quiet until re-alarm time`() {
        val readings = listOf(reading(0, 3.5))
        val stopped = AlarmPolicy.dismiss(AlarmState(), t0, AlarmReason.LOW, 3.5)

        val quiet = eval(10, readings, base.copy(reAlarmMinutes = 30, connectionLossGraceMinutes = 120), stopped)
        assertEquals(AlarmStatus.DISMISSED, quiet.status)
        assertEquals(t0 + min(30), quiet.nextWakeMillis)

        val again = eval(30, readings, base.copy(reAlarmMinutes = 30, connectionLossGraceMinutes = 120), stopped)
        assertEquals(AlarmStatus.RINGING, again.status)
    }

    @Test
    fun `dismissed low rings again when glucose drops further`() {
        val stopped = AlarmPolicy.dismiss(AlarmState(), t0, AlarmReason.LOW, 3.7)
        val d = eval(5, listOf(reading(5, 3.1)), base.copy(reAlarmMinutes = 0), stopped)
        assertEquals(AlarmStatus.RINGING, d.status)
    }

    @Test
    fun `dismissed low escalates to urgent`() {
        val stopped = AlarmPolicy.dismiss(AlarmState(), t0, AlarmReason.LOW, 3.4)
        val d = eval(5, listOf(reading(5, 2.9)), base.copy(reAlarmMinutes = 0), stopped)
        assertEquals(AlarmReason.URGENT_LOW, d.condition?.reason)
        assertEquals(AlarmStatus.RINGING, d.status)
    }

    @Test
    fun `dismissed high never repeats until recovery`() {
        val stopped = AlarmPolicy.dismiss(AlarmState(), t0, AlarmReason.HIGH, 12.0)
        val d = eval(
            120,
            listOf(reading(120, 12.0)),
            base.copy(reAlarmMinutes = 15),
            stopped,
        )
        assertEquals(AlarmStatus.DISMISSED, d.status)
    }

    @Test
    fun `recovery clears stop so next episode rings`() {
        val stopped = AlarmPolicy.dismiss(AlarmState(), t0, AlarmReason.LOW, 3.5)
        val recovered = eval(5, listOf(reading(5, 5.0)), state = stopped)
        assertEquals(AlarmState(), recovered.state)
        val next = eval(10, listOf(reading(10, 3.6)), state = recovered.state)
        assertEquals(AlarmStatus.RINGING, next.status)
    }

    @Test
    fun `snooze holds until expiry then rings with sound`() {
        val settings = base.copy(vibrateFirstSeconds = 60, connectionLossGraceMinutes = 120)
        val readings = listOf(reading(0, 3.5))
        val snoozed = AlarmPolicy.snooze(AlarmState(), t0, 10, AlarmReason.LOW)

        val during = eval(5, readings, settings, snoozed)
        assertEquals(AlarmStatus.SNOOZED, during.status)
        assertEquals(t0 + min(10), during.nextWakeMillis)

        val after = eval(10, readings, settings, during.state)
        assertEquals(AlarmStatus.RINGING, after.status)
        assertEquals(AlarmOutput.SOUND, after.output)
    }

    @Test
    fun `snoozed low breaks through on urgent`() {
        val snoozed = AlarmPolicy.snooze(AlarmState(), t0, 30, AlarmReason.LOW)
        val d = eval(3, listOf(reading(3, 2.7)), state = snoozed)
        assertEquals(AlarmStatus.RINGING, d.status)
    }

    @Test
    fun `stale low keeps alarming`() {
        val d = eval(60, listOf(reading(0, 3.4)))
        assertEquals(AlarmReason.LOW, d.condition?.reason)
        assertEquals(60L, d.condition?.dataAgeMinutes)
        assertEquals(AlarmStatus.RINGING, d.status)
    }

    @Test
    fun `signal loss follows connection loss mode`() {
        val readings = listOf(reading(0, 6.0))
        val silent = eval(20, readings, base.copy(connectionLossMode = ConnectionLossMode.SILENT))
        assertNull(silent.condition)

        val alarm = eval(20, readings, base.copy(connectionLossMode = ConnectionLossMode.ALARM))
        assertEquals(AlarmReason.SIGNAL_LOSS, alarm.condition?.reason)
        assertEquals(AlarmStatus.RINGING, alarm.status)

        val remindSettings = base.copy(connectionLossMode = ConnectionLossMode.REMIND)
        val remind = eval(20, readings, remindSettings)
        assertTrue(remind.remind)
        assertEquals(AlarmStatus.IDLE, remind.status)
        val notYet = eval(25, readings, remindSettings, remind.state)
        assertFalse(notYet.remind)
        val again = eval(35, readings, remindSettings, notYet.state)
        assertTrue(again.remind)
    }

    @Test
    fun `predicted low fires before crossing threshold`() {
        // Falling 0.1/min from 6.0 → 5.0 over 10 min; 20 min ahead ≈ 3.0 < 3.9.
        val readings = series(10, 6.0, 5.9, 5.8, 5.7, 5.6, 5.5, 5.4, 5.3, 5.2, 5.1, 5.0)
        val d = eval(10, readings, base.copy(predictiveLowEnabled = true))
        assertEquals(AlarmReason.PREDICTED_LOW, d.condition?.reason)
        assertEquals(3.0, d.condition!!.predictedMmol!!, 0.05)
    }

    @Test
    fun `falling 4_7 still in range rings as predicted low not actual low`() {
        // 5.5 → 4.7 over 15 min ≈ −0.053/min; 20 min ahead ≈ 3.63 < 3.9.
        val values = DoubleArray(16) { i -> 5.5 - i * (5.5 - 4.7) / 15.0 }
        val readings = series(15, *values)
        val d = eval(15, readings, base.copy(predictiveLowEnabled = true))
        assertEquals(AlarmReason.PREDICTED_LOW, d.condition?.reason)
        assertEquals(AlarmStatus.RINGING, d.status)
        assertEquals(4.7, d.condition!!.latest.mmol, 0.05)
        assertTrue(d.condition!!.latest.mmol >= 3.9)
        assertTrue((d.condition!!.predictedMmol ?: 9.0) < 3.9)
    }

    @Test
    fun `reason maps to hypo or hyper kind`() {
        assertEquals(GlucoseAlarmKind.HYPO, AlarmReason.PREDICTED_LOW.toGlucoseAlarmKind())
        assertEquals(GlucoseAlarmKind.HYPO, AlarmReason.FAST_DROP.toGlucoseAlarmKind())
        assertEquals(GlucoseAlarmKind.HYPER, AlarmReason.HIGH.toGlucoseAlarmKind())
    }

    @Test
    fun `fast drop fires when predictive is off`() {
        val readings = series(10, 12.0, 11.8, 11.6, 11.4, 11.2, 11.0, 10.8, 10.6, 10.4, 10.2, 10.0)
        val d = eval(10, readings, base.copy(fastDropEnabled = true))
        assertEquals(AlarmReason.FAST_DROP, d.condition?.reason)
    }

    @Test
    fun `high waits for delay`() {
        val settings = base.copy(hyperDelayMinutes = 15, connectionLossGraceMinutes = 120)
        val early = eval(10, series(10, 11.0, 11.1, 11.2), settings)
        assertNull(early.condition)
        assertEquals(t0 + min(8 + 15), early.nextWakeMillis)

        val readings = (0..20).map { reading(it, 11.0) }
        val late = eval(20, readings, settings)
        assertEquals(AlarmReason.HIGH, late.condition?.reason)
    }

    @Test
    fun `high vibrate only never plays sound`() {
        val d = eval(0, listOf(reading(0, 12.0)), base.copy(hyperVibrateOnly = true))
        assertEquals(AlarmOutput.VIBRATE, d.output)
    }

    @Test
    fun `night profile uses night thresholds and can mute high`() {
        val settings = base.copy(
            nightProfileEnabled = true,
            nightHypoThresholdMmol = 4.5,
            nightHyperAlarmEnabled = false,
        )
        val night = 2 * 60
        assertEquals(
            AlarmReason.LOW,
            eval(0, listOf(reading(0, 4.2)), settings, minuteOfDay = night).condition?.reason,
        )
        assertNull(eval(0, listOf(reading(0, 4.2)), settings).condition)
        assertNull(eval(0, listOf(reading(0, 15.0)), settings, minuteOfDay = night).condition)
    }

    @Test
    fun `night window wraps midnight`() {
        val s = AppSettings(nightProfileEnabled = true, nightStartMinute = 22 * 60, nightEndMinute = 7 * 60)
        assertTrue(s.isNight(23 * 60))
        assertTrue(s.isNight(3 * 60))
        assertFalse(s.isNight(7 * 60))
        assertFalse(s.isNight(12 * 60))
    }
}
