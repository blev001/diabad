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

    const val KIND_HYPO = "HYPO"
    const val KIND_HYPER = "HYPER"

    /** Payload: `mmol|thresholdMmol|snoozeMinutes|kind|vibration` (last two optional). */
    fun encodeRing(
        mmol: Double,
        thresholdMmol: Double,
        snoozeMinutes: Int,
        kind: String,
        vibration: String = "CLOCK",
    ): ByteArray =
        "$mmol|$thresholdMmol|$snoozeMinutes|$kind|$vibration".toByteArray(Charsets.UTF_8)

    fun decodeRing(data: ByteArray): RingPayload? {
        val parts = data.toString(Charsets.UTF_8).split('|')
        if (parts.size < 3) return null
        val mmol = parts[0].toDoubleOrNull() ?: return null
        val threshold = parts[1].toDoubleOrNull() ?: return null
        val snooze = parts[2].toIntOrNull() ?: return null
        val kind = parts.getOrNull(3)?.takeIf { it == KIND_HYPER } ?: KIND_HYPO
        val vibration = parts.getOrNull(4)?.takeIf { it.isNotBlank() } ?: "CLOCK"
        return RingPayload(mmol, threshold, snooze, kind, vibration)
    }

    data class RingPayload(
        val mmol: Double,
        val thresholdMmol: Double,
        val snoozeMinutes: Int,
        val kind: String = KIND_HYPO,
        val vibration: String = "CLOCK",
    )
}
