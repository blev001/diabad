package com.diabad.domain.model

/** Previous sample relative to [latest], assuming history is time-ordered. */
fun List<GlucoseReading>.previousOf(latest: GlucoseReading?): GlucoseReading? {
    if (latest == null || size < 2) return null
    val index = indexOfLast { it.timestampMillis == latest.timestampMillis }
    return when {
        index > 0 -> this[index - 1]
        index == 0 -> null
        else -> getOrNull(lastIndex - 1)
    }
}
