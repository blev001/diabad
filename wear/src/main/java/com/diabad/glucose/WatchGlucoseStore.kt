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
import com.diabad.core.glucose.formatMmol
import com.diabad.core.wear.WearGlucosePaths
import com.diabad.tile.ArrowOnlyTileService
import com.diabad.tile.BigNumberTileService
import com.diabad.tile.DeltaTileService
import com.diabad.tile.DetailTileService
import com.diabad.tile.NumberArrowTileService
import com.diabad.tile.ZoneTileService
import kotlin.math.abs

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
            if (abs(delta) < 0.05) return "Δ 0.0"
            val sign = if (delta > 0) "+" else "−"
            return "Δ $sign${formatMmol(abs(delta))}"
        }

    fun ageMinutes(nowMillis: Long = System.currentTimeMillis()): Long =
        ((nowMillis - timestampMillis) / 60_000L).coerceAtLeast(0)

    fun ageText(nowMillis: Long = System.currentTimeMillis()): String {
        val age = ageMinutes(nowMillis)
        return when {
            age <= 0L -> "сейчас"
            age < 60L -> "$age мин"
            else -> "${age / 60} ч"
        }
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
        val current = read(context)
        val unchanged = current != null &&
            current.mmol.toBits() == mmol.toBits() &&
            current.trendName == trend &&
            current.hasDelta == hasDelta &&
            current.delta.toBits() == delta.toBits() &&
            current.timestampMillis == timestampMillis &&
            current.thresholdMmol.toBits() == thresholdMmol.toBits() &&
            current.hyperThresholdMmol.toBits() == hyperThresholdMmol.toBits() &&
            current.alarming == alarming
        if (unchanged) return
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
        val tiles = listOf(
            BigNumberTileService::class.java,
            NumberArrowTileService::class.java,
            DetailTileService::class.java,
            ArrowOnlyTileService::class.java,
            DeltaTileService::class.java,
            ZoneTileService::class.java,
        )
        tiles.filter { ActiveWearWidgets.shouldUpdateTile(app, it) }.forEach { clazz ->
            TileService.getUpdater(app).requestUpdate(clazz)
        }

        val complications = listOf(
            ValueComplicationService::class.java,
            ValueTrendComplicationService::class.java,
            TrendComplicationService::class.java,
            DeltaComplicationService::class.java,
            AgeComplicationService::class.java,
            RangedComplicationService::class.java,
        )
        ActiveWearWidgets.complicationUpdates(app, complications).forEach { (clazz, ids) ->
            val requester = ComplicationDataSourceUpdateRequester
                .create(app, ComponentName(app, clazz))
            if (ids.isEmpty()) requester.requestUpdateAll() else requester.requestUpdate(*ids)
        }
        ActiveWearWidgets.refreshTilesFromSystem(app)
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
