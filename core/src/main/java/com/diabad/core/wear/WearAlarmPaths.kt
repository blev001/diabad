package com.diabad.core.wear

/**
 * Data Layer paths between phone DiaBAD and the Galaxy Watch companion.
 * Watch UX mirrors the system Clock alarm: full-screen, strong vibe, Stop / Snooze.
 */
object WearAlarmPaths {
    const val RING = "/diabad/alarm/ring"
    const val CLEAR = "/diabad/alarm/clear"
    const val DISMISS = "/diabad/alarm/dismiss"
    const val SNOOZE = "/diabad/alarm/snooze"

    /** Payload: `mmol|thresholdMmol|snoozeMinutes` */
    fun encodeRing(mmol: Double, thresholdMmol: Double, snoozeMinutes: Int): ByteArray =
        "$mmol|$thresholdMmol|$snoozeMinutes".toByteArray(Charsets.UTF_8)

    fun decodeRing(data: ByteArray): RingPayload? {
        val parts = data.toString(Charsets.UTF_8).split('|')
        if (parts.size < 3) return null
        val mmol = parts[0].toDoubleOrNull() ?: return null
        val threshold = parts[1].toDoubleOrNull() ?: return null
        val snooze = parts[2].toIntOrNull() ?: return null
        return RingPayload(mmol, threshold, snooze)
    }

    data class RingPayload(
        val mmol: Double,
        val thresholdMmol: Double,
        val snoozeMinutes: Int,
    )
}
