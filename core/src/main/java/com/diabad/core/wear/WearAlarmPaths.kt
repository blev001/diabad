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
    const val KIND_URGENT_LOW = "URGENT_LOW"
    const val KIND_PREDICTED_LOW = "PREDICTED_LOW"
    const val KIND_FAST_DROP = "FAST_DROP"
    const val KIND_SIGNAL_LOSS = "SIGNAL_LOSS"
    const val FLAG_TEST = "TEST"

    fun normalizeKind(raw: String?): String = when (raw) {
        KIND_HYPER, "HIGH" -> KIND_HYPER
        KIND_URGENT_LOW -> KIND_URGENT_LOW
        KIND_PREDICTED_LOW -> KIND_PREDICTED_LOW
        KIND_FAST_DROP -> KIND_FAST_DROP
        KIND_SIGNAL_LOSS -> KIND_SIGNAL_LOSS
        else -> KIND_HYPO
    }

    fun encodeSnooze(minutes: Int): ByteArray =
        minutes.toString().toByteArray(Charsets.UTF_8)

    fun decodeSnooze(data: ByteArray, fallback: Int = 15): Int =
        data.toString(Charsets.UTF_8).toIntOrNull() ?: fallback

    /** Payload: `mmol|thresholdMmol|snoozeMinutes|kind|vibration|TEST?`. */
    fun encodeRing(
        mmol: Double,
        thresholdMmol: Double,
        snoozeMinutes: Int,
        kind: String,
        vibration: String = "CLOCK",
        test: Boolean = false,
    ): ByteArray {
        val base = "$mmol|$thresholdMmol|$snoozeMinutes|$kind|$vibration"
        val payload = if (test) "$base|$FLAG_TEST" else base
        return payload.toByteArray(Charsets.UTF_8)
    }

    fun decodeRing(data: ByteArray): RingPayload? {
        val parts = data.toString(Charsets.UTF_8).split('|')
        if (parts.size < 3) return null
        val mmol = parts[0].toDoubleOrNull() ?: return null
        val threshold = parts[1].toDoubleOrNull() ?: return null
        val snooze = parts[2].toIntOrNull() ?: return null
        val kind = normalizeKind(parts.getOrNull(3))
        val vibration = parts.getOrNull(4)
            ?.takeIf { it.isNotBlank() && it != FLAG_TEST }
            ?: "CLOCK"
        val test = parts.any { it == FLAG_TEST }
        return RingPayload(mmol, threshold, snooze, kind, vibration, test)
    }

    data class RingPayload(
        val mmol: Double,
        val thresholdMmol: Double,
        val snoozeMinutes: Int,
        val kind: String = KIND_HYPO,
        val vibration: String = "CLOCK",
        val test: Boolean = false,
    )
}
