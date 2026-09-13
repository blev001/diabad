package com.diabad.glucose

import android.content.ComponentName
import android.content.Context
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.diabad.complication.AgeComplicationService
import com.diabad.complication.DeltaComplicationService
import com.diabad.complication.RangedComplicationService
import com.diabad.complication.TrendComplicationService
import com.diabad.complication.ValueComplicationService
import com.diabad.complication.ValueTrendComplicationService
import com.diabad.core.glucose.formatDeltaMmol
import com.diabad.core.glucose.formatMmol
import com.diabad.core.wear.WearGlucosePaths
import com.diabad.tile.ArrowOnlyTileService
import com.diabad.tile.BigNumberTileService
import com.diabad.tile.DeltaTileService
import com.diabad.tile.DetailTileService
import com.diabad.tile.NumberArrowTileService
import com.diabad.tile.ZoneTileService

data class WatchGlucoseSnapshot(
    val mmol: Double,
    val trendName: String,
    val trendGlyph: String,
    val hasDelta: Boolean,
    val delta: Double,
    val timestampMillis: Long,
    val thresholdMmol: Double,
    val hyperThresholdMmol: Double = 10.0,
    val alarming: Boolean,
) {
    val mmolText: String get() = formatMmol(mmol)

    val deltaText: String
        get() {
            if (!hasDelta) return "—"
            return formatDeltaMmol(mmol, mmol - delta) ?: "—"
        }

    val ageMinutes: Long
        get() = ((System.currentTimeMillis() - timestampMillis) / 60_000L).coerceAtLeast(0)

    val ageText: String
        get() = when {
            ageMinutes <= 0L -> "сейчас"
            ageMinutes < 60L -> "$ageMinutes мин"
            else -> "${ageMinutes / 60} ч"
        }

    val isLow: Boolean get() = mmol < thresholdMmol
    val isHigh: Boolean get() = mmol > hyperThresholdMmol

    val zoneLabel: String
        get() = when {
            alarming && isHigh -> "Высокий"
            alarming || isLow -> "Низкий"
            isHigh -> "Высокий"
            else -> "Норма"
        }
}

object WatchGlucoseStore {
    private const val PREFS = "diabad_watch_glucose"

    fun read(context: Context): WatchGlucoseSnapshot? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!p.contains(WearGlucosePaths.KEY_MMOL)) return null
        val trend = p.getString(WearGlucosePaths.KEY_TREND, "NONE") ?: "NONE"
        return WatchGlucoseSnapshot(
            mmol = Double.fromBits(p.getLong(WearGlucosePaths.KEY_MMOL, 0L)),
            trendName = trend,
            trendGlyph = glyphFor(trend),
            hasDelta = p.getBoolean(WearGlucosePaths.KEY_HAS_DELTA, false),
            delta = Double.fromBits(p.getLong(WearGlucosePaths.KEY_DELTA, 0L)),
            timestampMillis = p.getLong(WearGlucosePaths.KEY_TIMESTAMP, 0L),
            thresholdMmol = Double.fromBits(
                p.getLong(WearGlucosePaths.KEY_THRESHOLD, (3.9).toBits()),
            ),
            hyperThresholdMmol = Double.fromBits(
                p.getLong(WearGlucosePaths.KEY_HYPER_THRESHOLD, (10.0).toBits()),
            ),
            alarming = p.getBoolean(WearGlucosePaths.KEY_ALARMING, false),
        )
    }

    fun write(
        context: Context,
        mmol: Double,
        trend: String,
        hasDelta: Boolean,
        delta: Double,
        timestampMillis: Long,
        thresholdMmol: Double,
        hyperThresholdMmol: Double,
        alarming: Boolean,
    ) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(WearGlucosePaths.KEY_MMOL, mmol.toBits())
            .putString(WearGlucosePaths.KEY_TREND, trend)
            .putBoolean(WearGlucosePaths.KEY_HAS_DELTA, hasDelta)
            .putLong(WearGlucosePaths.KEY_DELTA, delta.toBits())
            .putLong(WearGlucosePaths.KEY_TIMESTAMP, timestampMillis)
            .putLong(WearGlucosePaths.KEY_THRESHOLD, thresholdMmol.toBits())
            .putLong(WearGlucosePaths.KEY_HYPER_THRESHOLD, hyperThresholdMmol.toBits())
            .putBoolean(WearGlucosePaths.KEY_ALARMING, alarming)
            .apply()
        requestUiRefresh(context)
    }

    fun requestUiRefresh(context: Context) {
        val app = context.applicationContext
        listOf(
            BigNumberTileService::class.java,
            NumberArrowTileService::class.java,
            DetailTileService::class.java,
            ArrowOnlyTileService::class.java,
            DeltaTileService::class.java,
            ZoneTileService::class.java,
        ).forEach { TileService.getUpdater(app).requestUpdate(it) }

        listOf(
            ValueComplicationService::class.java,
            ValueTrendComplicationService::class.java,
            TrendComplicationService::class.java,
            DeltaComplicationService::class.java,
            AgeComplicationService::class.java,
            RangedComplicationService::class.java,
        ).forEach { clazz ->
            ComplicationDataSourceUpdateRequester
                .create(app, ComponentName(app, clazz))
                .requestUpdateAll()
        }
    }

    fun glyphFor(trendName: String): String = when (trendName) {
        "DOUBLE_UP" -> "⇈"
        "SINGLE_UP" -> "↑"
        "FORTY_FIVE_UP" -> "↗"
        "FLAT" -> "→"
        "FORTY_FIVE_DOWN" -> "↘"
        "SINGLE_DOWN" -> "↓"
        "DOUBLE_DOWN" -> "⇊"
        else -> "·"
    }
}
