package com.diabad.tile

import android.graphics.Color
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.FontStyle
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.Row
import androidx.wear.protolayout.LayoutElementBuilders.TEXT_ALIGN_CENTER
import androidx.wear.protolayout.LayoutElementBuilders.Text
import androidx.wear.protolayout.ModifiersBuilders.Background
import androidx.wear.protolayout.ModifiersBuilders.Modifiers
import androidx.wear.protolayout.ModifiersBuilders.Padding
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.TimelineBuilders.TimelineEntry
import androidx.wear.protolayout.TypeBuilders.StringProp
import androidx.wear.tiles.EventBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.TileService
import com.diabad.glucose.ActiveWearWidgets
import com.diabad.glucose.WatchGlucoseSnapshot
import com.diabad.glucose.WatchGlucoseStore
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

enum class TileStyle {
    BIG_NUMBER,
    NUMBER_ARROW,
    DETAIL,
    ARROW_ONLY,
    DELTA,
    ZONE,
}

abstract class BaseGlucoseTileService : TileService() {

    protected abstract val style: TileStyle

    override fun onTileAddEvent(requestParams: EventBuilders.TileAddEvent) {
        super.onTileAddEvent(requestParams)
        ActiveWearWidgets.markTile(this, javaClass, added = true)
    }

    override fun onTileRemoveEvent(requestParams: EventBuilders.TileRemoveEvent) {
        super.onTileRemoveEvent(requestParams)
        ActiveWearWidgets.markTile(this, javaClass, added = false)
    }

    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest,
    ): ListenableFuture<Tile> {
        val snap = WatchGlucoseStore.read(this)
        val layout = when (style) {
            TileStyle.BIG_NUMBER -> bigNumber(snap)
            TileStyle.NUMBER_ARROW -> numberArrow(snap)
            TileStyle.DETAIL -> detail(snap)
            TileStyle.ARROW_ONLY -> arrowOnly(snap)
            TileStyle.DELTA -> delta(snap)
            TileStyle.ZONE -> zone(snap)
        }
        val tile = Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTileTimeline(
                Timeline.Builder()
                    .addTimelineEntry(
                        TimelineEntry.Builder()
                            .setLayout(
                                LayoutElementBuilders.Layout.Builder()
                                    .setRoot(layout)
                                    .build(),
                            )
                            .build(),
                    )
                    .build(),
            )
            .setFreshnessIntervalMillis(60_000L)
            .build()
        return Futures.immediateFuture(tile)
    }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest,
    ): ListenableFuture<androidx.wear.protolayout.ResourceBuilders.Resources> =
        Futures.immediateFuture(
            androidx.wear.protolayout.ResourceBuilders.Resources.Builder()
                .setVersion(RESOURCES_VERSION)
                .build(),
        )

    private fun bigNumber(snap: WatchGlucoseSnapshot?): LayoutElement {
        val color = valueColor(snap)
        return centeredColumn(
            text(snap?.mmolText ?: "—", 48f, color, true),
            text("ммоль/л", 14f, Color.LTGRAY, false),
            text(snap?.trendGlyph ?: "", 28f, color, true),
        )
    }

    private fun numberArrow(snap: WatchGlucoseSnapshot?): LayoutElement {
        val color = valueColor(snap)
        return centeredColumn(
            row(
                text(snap?.mmolText ?: "—", 40f, color, true),
                text(" ${snap?.trendGlyph ?: ""}", 36f, color, true),
            ),
            text(snap?.ageText ?: "нет данных", 13f, Color.GRAY, false),
        )
    }

    private fun detail(snap: WatchGlucoseSnapshot?): LayoutElement {
        val color = valueColor(snap)
        return centeredColumn(
            text(snap?.mmolText ?: "—", 36f, color, true),
            text("${snap?.trendGlyph ?: "·"}  ${snap?.deltaText ?: ""}", 18f, Color.WHITE, false),
            text(snap?.ageText ?: "ожидание OtTai", 12f, Color.GRAY, false),
        )
    }

    private fun arrowOnly(snap: WatchGlucoseSnapshot?): LayoutElement {
        val color = valueColor(snap)
        return centeredColumn(
            text(snap?.trendGlyph ?: "·", 64f, color, true),
            text(snap?.mmolText ?: "—", 20f, Color.WHITE, false),
            text("тренд", 12f, Color.GRAY, false),
        )
    }

    private fun delta(snap: WatchGlucoseSnapshot?): LayoutElement {
        val color = valueColor(snap)
        return centeredColumn(
            text(snap?.deltaText ?: "Δ —", 32f, color, true),
            text("${snap?.mmolText ?: "—"} ${snap?.trendGlyph ?: ""}", 18f, Color.WHITE, false),
            text("изменение", 12f, Color.GRAY, false),
        )
    }

    private fun zone(snap: WatchGlucoseSnapshot?): LayoutElement {
        val color = valueColor(snap)
        return centeredColumn(
            text(snap?.zoneLabel ?: "Нет данных", 22f, color, true),
            text(snap?.mmolText ?: "—", 40f, color, true),
            text("порог ${snap?.let { "${format(it.thresholdMmol)}–${format(it.hyperThresholdMmol)}" } ?: "3.9–10"}", 12f, Color.GRAY, false),
        )
    }

    private fun valueColor(snap: WatchGlucoseSnapshot?): Int = when {
        snap == null -> Color.GRAY
        snap.alarming || snap.isLow -> Color.parseColor("#FF5252")
        snap.approaching -> Color.parseColor("#FFB74D")
        snap.isHigh -> Color.parseColor("#FFB74D")
        else -> Color.parseColor("#69F0AE")
    }

    private fun format(v: Double): String =
        String.format(java.util.Locale.US, "%.1f", v)

    private fun centeredColumn(vararg children: LayoutElement): LayoutElement {
        val col = Column.Builder()
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setModifiers(
                Modifiers.Builder()
                    .setPadding(Padding.Builder().setAll(dp(8f)).build())
                    .setBackground(
                        Background.Builder()
                            .setColor(argb(Color.parseColor("#121212")))
                            .build(),
                    )
                    .build(),
            )
        children.forEach { col.addContent(it) }
        return col.build()
    }

    private fun row(vararg children: LayoutElement): LayoutElement {
        val row = Row.Builder()
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
        children.forEach { row.addContent(it) }
        return row.build()
    }

    private fun text(
        value: String,
        sizeSp: Float,
        color: Int,
        bold: Boolean,
    ): LayoutElement =
        Text.Builder()
            .setText(StringProp.Builder(value).build())
            .setFontStyle(
                FontStyle.Builder()
                    .setSize(sp(sizeSp))
                    .setColor(argb(color))
                    .setWeight(
                        if (bold) {
                            LayoutElementBuilders.FONT_WEIGHT_BOLD
                        } else {
                            LayoutElementBuilders.FONT_WEIGHT_NORMAL
                        },
                    )
                    .build(),
            )
            .setMultilineAlignment(TEXT_ALIGN_CENTER)
            .build()

    companion object {
        const val RESOURCES_VERSION = "1"
    }
}

class BigNumberTileService : BaseGlucoseTileService() {
    override val style = TileStyle.BIG_NUMBER
}

class NumberArrowTileService : BaseGlucoseTileService() {
    override val style = TileStyle.NUMBER_ARROW
}

class DetailTileService : BaseGlucoseTileService() {
    override val style = TileStyle.DETAIL
}

class ArrowOnlyTileService : BaseGlucoseTileService() {
    override val style = TileStyle.ARROW_ONLY
}

class DeltaTileService : BaseGlucoseTileService() {
    override val style = TileStyle.DELTA
}

class ZoneTileService : BaseGlucoseTileService() {
    override val style = TileStyle.ZONE
}
