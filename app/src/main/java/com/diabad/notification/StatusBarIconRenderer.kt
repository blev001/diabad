package com.diabad.notification

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.util.TypedValue
import com.diabad.core.glucose.formatMmol
import com.diabad.domain.model.GlucoseReading
import com.diabad.domain.model.GlucoseZone
import com.diabad.domain.model.TrendArrow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-resolution badge for the notification smallIcon (status bar) and a
 * coloured largeIcon for the shade. Unicode arrows are not used — they
 * collapse to noise at 24 dp; filled chevrons stay readable after downscale.
 */
@Singleton
class StatusBarIconRenderer @Inject constructor() {

    private var cachedKey: String? = null
    private var cachedBitmap: Bitmap? = null
    private var cachedLargeKey: String? = null
    private var cachedLargeBitmap: Bitmap? = null

    fun render(context: Context, reading: GlucoseReading?): Bitmap {
        val key = reading?.let { "${formatMmol(it.mmol)}|${it.trend.name}" } ?: "—"
        cachedBitmap?.let { if (cachedKey == key) return it }

        val sizePx = pixelSize(context, ICON_DP)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        drawBadge(
            canvas = Canvas(bitmap),
            sizePx = sizePx,
            reading = reading,
            ink = Color.WHITE,
            background = Color.TRANSPARENT,
        )
        cachedKey = key
        cachedBitmap = bitmap
        return bitmap
    }

    /** Larger coloured disk for the notification shade (not the status-bar slot). */
    fun renderShadeBadge(
        context: Context,
        reading: GlucoseReading?,
        hypoThresholdMmol: Double,
        hyperThresholdMmol: Double,
    ): Bitmap {
        val zone = GlucoseZone.classify(reading?.mmol, hypoThresholdMmol, hyperThresholdMmol)
        val key = "${reading?.let { "${formatMmol(it.mmol)}|${it.trend.name}" } ?: "—"}|$zone"
        cachedLargeBitmap?.let { if (cachedLargeKey == key) return it }

        val sizePx = pixelSize(context, SHADE_DP)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        drawBadge(
            canvas = Canvas(bitmap),
            sizePx = sizePx,
            reading = reading,
            ink = Color.WHITE,
            background = zoneBackground(zone),
        )
        cachedLargeKey = key
        cachedLargeBitmap = bitmap
        return bitmap
    }

    private fun drawBadge(
        canvas: Canvas,
        sizePx: Int,
        reading: GlucoseReading?,
        ink: Int,
        background: Int,
    ) {
        if (background != Color.TRANSPARENT) {
            val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = background }
            val r = sizePx / 2f
            canvas.drawCircle(r, r, r, fill)
        }

        val value = reading?.let { formatMmol(it.mmol) } ?: "—"
        val trend = reading?.trend ?: TrendArrow.NONE
        val hasArrow = trend.glyph.isNotEmpty()
        val metrics = computeStatusBarIconMetrics(sizePx, value, hasArrow)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
            isSubpixelText = true
            isLinearText = true
            letterSpacing = -0.06f
        }

        paint.textSize = metrics.valueTextSize
        canvas.drawText(value, metrics.valueCenterX, metrics.valueBaselineY, paint)

        if (metrics.hasArrow) {
            paint.style = Paint.Style.FILL
            drawTrendArrow(
                canvas = canvas,
                paint = paint,
                trend = trend,
                left = metrics.arrowLeft,
                top = metrics.arrowTop,
                width = metrics.arrowWidth,
                height = metrics.arrowHeight,
            )
        }
    }

    private fun drawTrendArrow(
        canvas: Canvas,
        paint: Paint,
        trend: TrendArrow,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
    ) {
        val cx = left + width / 2f
        val cy = top + height / 2f
        val rotation = when (trend) {
            TrendArrow.DOUBLE_UP, TrendArrow.SINGLE_UP -> 0f
            TrendArrow.FORTY_FIVE_UP -> 45f
            TrendArrow.FLAT -> 90f
            TrendArrow.FORTY_FIVE_DOWN -> 135f
            TrendArrow.SINGLE_DOWN, TrendArrow.DOUBLE_DOWN -> 180f
            else -> return
        }
        val doubleArrow = trend == TrendArrow.DOUBLE_UP || trend == TrendArrow.DOUBLE_DOWN
        if (doubleArrow) {
            val chevronH = height * 0.48f
            val chevronW = width * 0.92f
            drawChevron(canvas, paint, cx, cy - height * 0.18f, chevronW, chevronH, rotation)
            drawChevron(canvas, paint, cx, cy + height * 0.18f, chevronW, chevronH, rotation)
        } else {
            drawChevron(canvas, paint, cx, cy, width * 0.98f, height * 0.92f, rotation)
        }
    }

    private fun drawChevron(
        canvas: Canvas,
        paint: Paint,
        cx: Float,
        cy: Float,
        width: Float,
        height: Float,
        rotationDeg: Float,
    ) {
        val path = Path().apply {
            // Fat triangle — stays a clear direction after the system downscales to ~24 dp.
            moveTo(cx, cy - height * 0.50f)
            lineTo(cx + width * 0.52f, cy + height * 0.42f)
            lineTo(cx - width * 0.52f, cy + height * 0.42f)
            close()
        }
        canvas.save()
        canvas.rotate(rotationDeg, cx, cy)
        canvas.drawPath(path, paint)
        canvas.restore()
    }

    private fun pixelSize(context: Context, dp: Float): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics,
        ).toInt().coerceAtLeast(96)

    private fun zoneBackground(zone: GlucoseZone): Int = when (zone) {
        GlucoseZone.VERY_LOW, GlucoseZone.LOW -> Color.rgb(255, 69, 58)
        GlucoseZone.HIGH, GlucoseZone.VERY_HIGH -> Color.rgb(255, 159, 10)
        GlucoseZone.IN_RANGE -> Color.rgb(52, 199, 89)
        GlucoseZone.UNKNOWN -> Color.rgb(88, 88, 92)
    }

    private companion object {
        const val ICON_DP = 72f
        const val SHADE_DP = 96f
    }
}
