package com.diabad.complication

import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.diabad.R
import com.diabad.glucose.ActiveWearWidgets
import com.diabad.glucose.WatchGlucoseSnapshot
import com.diabad.glucose.WatchGlucoseStore
import android.graphics.drawable.Icon

abstract class BaseGlucoseComplicationService : SuspendingComplicationDataSourceService() {

    protected abstract fun buildData(
        snap: WatchGlucoseSnapshot?,
        type: ComplicationType,
    ): ComplicationData?

    override fun onComplicationActivated(complicationInstanceId: Int, type: ComplicationType) {
        ActiveWearWidgets.markComplication(
            this,
            javaClass,
            instanceId = complicationInstanceId,
            added = true,
        )
    }

    override fun onComplicationDeactivated(complicationInstanceId: Int) {
        ActiveWearWidgets.markComplication(
            this,
            javaClass,
            instanceId = complicationInstanceId,
            added = false,
        )
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val snap = WatchGlucoseStore.read(this)
        return buildData(snap, request.complicationType)
            ?: empty(request.complicationType)
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val preview = WatchGlucoseSnapshot(
            mmol = 5.4,
            trendName = "FLAT",
            trendGlyph = "→",
            hasDelta = true,
            delta = 0.2,
            timestampMillis = System.currentTimeMillis(),
            thresholdMmol = 3.9,
            hyperThresholdMmol = 10.0,
            alarming = false,
        )
        return buildData(preview, type)
    }

    protected fun shortText(text: String, title: String? = null): ShortTextComplicationData {
        val builder = ShortTextComplicationData.Builder(
            PlainComplicationText.Builder(text).build(),
            PlainComplicationText.Builder(text).build(),
        )
        if (title != null) {
            builder.setTitle(PlainComplicationText.Builder(title).build())
        }
        return builder.build()
    }

    protected fun longText(text: String, title: String? = null): LongTextComplicationData {
        val builder = LongTextComplicationData.Builder(
            PlainComplicationText.Builder(text).build(),
            PlainComplicationText.Builder(text).build(),
        )
        if (title != null) {
            builder.setTitle(PlainComplicationText.Builder(title).build())
        }
        return builder.build()
    }

    private fun empty(type: ComplicationType): ComplicationData? = when (type) {
        ComplicationType.SHORT_TEXT -> shortText("—", "DiaBAD")
        ComplicationType.LONG_TEXT -> longText("Нет данных", "DiaBAD")
        ComplicationType.RANGED_VALUE ->
            RangedValueComplicationData.Builder(
                value = 0f,
                min = 2f,
                max = 16f,
                contentDescription = PlainComplicationText.Builder("Нет данных").build(),
            )
                .setText(PlainComplicationText.Builder("—").build())
                .build()
        else -> null
    }
}

/** Цифра: 5.4 */
class ValueComplicationService : BaseGlucoseComplicationService() {
    override fun buildData(snap: WatchGlucoseSnapshot?, type: ComplicationType): ComplicationData? {
        val value = snap?.mmolText ?: "—"
        return when (type) {
            ComplicationType.SHORT_TEXT -> shortText(value, "ммоль")
            ComplicationType.LONG_TEXT -> longText("$value ммоль/л", "Глюкоза")
            else -> null
        }
    }
}

/** Цифра + стрелка: 5.4 → */
class ValueTrendComplicationService : BaseGlucoseComplicationService() {
    override fun buildData(snap: WatchGlucoseSnapshot?, type: ComplicationType): ComplicationData? {
        val value = snap?.mmolText ?: "—"
        val arrow = snap?.trendGlyph ?: ""
        return when (type) {
            ComplicationType.SHORT_TEXT -> shortText("$value$arrow")
            ComplicationType.LONG_TEXT -> longText("$value $arrow ммоль/л", "Глюкоза")
            else -> null
        }
    }
}

/** Только стрелка тренда */
class TrendComplicationService : BaseGlucoseComplicationService() {
    override fun buildData(snap: WatchGlucoseSnapshot?, type: ComplicationType): ComplicationData? {
        val arrow = snap?.trendGlyph?.ifBlank { "·" } ?: "·"
        return when (type) {
            ComplicationType.SHORT_TEXT -> shortText(arrow, snap?.mmolText)
            ComplicationType.LONG_TEXT -> longText(arrow, "Тренд ${snap?.mmolText ?: ""}")
            else -> null
        }
    }
}

/** Дельта между замерами */
class DeltaComplicationService : BaseGlucoseComplicationService() {
    override fun buildData(snap: WatchGlucoseSnapshot?, type: ComplicationType): ComplicationData? {
        val delta = snap?.deltaText ?: "Δ —"
        return when (type) {
            ComplicationType.SHORT_TEXT -> shortText(delta.replace("Δ ", ""), "Δ")
            ComplicationType.LONG_TEXT -> longText(delta, "Изменение")
            else -> null
        }
    }
}

/** Возраст замера */
class AgeComplicationService : BaseGlucoseComplicationService() {
    override fun buildData(snap: WatchGlucoseSnapshot?, type: ComplicationType): ComplicationData? {
        val age = snap?.ageText() ?: "—"
        return when (type) {
            ComplicationType.SHORT_TEXT -> shortText(age, "назад")
            ComplicationType.LONG_TEXT -> longText(age, "Обновлено")
            else -> null
        }
    }
}

/** Шкала 2–16 ммоль с цифрой */
class RangedComplicationService : BaseGlucoseComplicationService() {
    override fun buildData(snap: WatchGlucoseSnapshot?, type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.RANGED_VALUE && type != ComplicationType.SHORT_TEXT) {
            return null
        }
        val mmol = snap?.mmol?.toFloat() ?: 0f
        val text = snap?.mmolText ?: "—"
        if (type == ComplicationType.SHORT_TEXT) {
            return shortText(text, "шкала")
        }
        return RangedValueComplicationData.Builder(
            value = mmol.coerceIn(2f, 16f),
            min = 2f,
            max = 16f,
            contentDescription = PlainComplicationText.Builder("$text ммоль/л").build(),
        )
            .setText(PlainComplicationText.Builder(text).build())
            .setTitle(PlainComplicationText.Builder(snap?.trendGlyph ?: "·").build())
            .setMonochromaticImage(
                MonochromaticImage.Builder(
                    Icon.createWithResource(this, R.drawable.ic_stat_glucose),
                ).build(),
            )
            .build()
    }
}
