package com.diabad.data.ottai

import com.diabad.core.glucose.mgdlToMmol
import com.diabad.domain.model.GlucoseReading
import com.diabad.domain.model.GlucoseSource
import com.diabad.domain.model.TrendArrow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OttaiBroadcastParser @Inject constructor() {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Parses Nightscout-style entries JSON from OtTai.
     * `sgv` arrives in mg/dL (AAPS convention); we store mmol/L.
     */
    fun parseEntriesJson(data: String): List<GlucoseReading> {
        val entries = json.decodeFromString<List<OttaiEntryDto>>(data)
        return entries.mapNotNull { entry ->
            if (!entry.type.equals("sgv", ignoreCase = true)) return@mapNotNull null
            val mgdl = entry.sgv ?: return@mapNotNull null
            val date = entry.date ?: return@mapNotNull null
            GlucoseReading(
                mmol = mgdlToMmol(mgdl),
                timestampMillis = date,
                trend = TrendArrow.fromNightscout(entry.direction),
                source = GlucoseSource.OTTAI,
            )
        }
    }
}

@Serializable
internal data class OttaiEntryDto(
    val type: String? = null,
    val date: Long? = null,
    val sgv: Double? = null,
    val direction: String? = null,
)
