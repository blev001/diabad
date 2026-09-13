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
import com.diabad.domain.model.TrendArrow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

/**
 * Status-bar badge: a full-width mmol value and a short, in-bounds trend
 * mark under it. Each direction is drawn natively — rotating a large
 * triangle is what made the previous arrow clip and look tiny.
 */
@Singleton
class StatusBarIconRenderer @Inject constructor() {

    private var cachedKey: String? = null
    private var cachedBitmap: Bitmap? = null

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

    private fun drawBadge(
        canvas: Canvas,
        sizePx: Int,
        reading: GlucoseReading?,
        ink: Int,
        background: Int,
    ) {
        if (background != Color.TRANSPARENT) {
            val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = background }
            val pad = sizePx * 0.06f
            canvas.drawRoundRect(
                pad,
                pad,
                sizePx - pad,
                sizePx - pad,
                sizePx * 0.18f,
                sizePx * 0.18f,
                fill,
            )
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
            letterSpacing = -0.07f
        }

        paint.textSize = metrics.valueTextSize
        canvas.drawText(value, metrics.valueCenterX, metrics.valueBaselineY, paint)

        if (metrics.hasArrow) {
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
        val stroke = min(width, height) * 0.34f
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = stroke
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND

        val inset = stroke * 0.55f
        val l = left + inset
        val r = left + width - inset
        val t = top + inset
        val b = top + height - inset
        val cx = (l + r) / 2f
        val cy = (t + b) / 2f

        when (trend) {
            TrendArrow.SINGLE_UP -> drawCaret(canvas, paint, cx, t, l, b, r, b)
            TrendArrow.SINGLE_DOWN -> drawCaret(canvas, paint, cx, b, l, t, r, t)
            TrendArrow.DOUBLE_UP -> {
                val mid = t + (b - t) * 0.52f
                drawCaret(canvas, paint, cx, t, l, mid, r, mid)
                drawCaret(canvas, paint, cx, mid, l, b, r, b)
            }
            TrendArrow.DOUBLE_DOWN -> {
                val mid = t + (b - t) * 0.48f
                drawCaret(canvas, paint, cx, mid, l, t, r, t)
                drawCaret(canvas, paint, cx, b, l, mid, r, mid)
            }
            TrendArrow.FLAT -> drawCaret(canvas, paint, r, cy, l, t, l, b)
            TrendArrow.FORTY_FIVE_UP -> drawCaret(canvas, paint, r, t, l, cy, cx, b)
            TrendArrow.FORTY_FIVE_DOWN -> drawCaret(canvas, paint, r, b, l, cy, cx, t)
            else -> Unit
        }
    }

    /** Two-stroke caret that stays inside the given points. */
    private fun drawCaret(
        canvas: Canvas,
        paint: Paint,
        tipX: Float,
        tipY: Float,
        aX: Float,
        aY: Float,
        bX: Float,
        bY: Float,
    ) {
        val path = Path().apply {
            moveTo(aX, aY)
            lineTo(tipX, tipY)
            lineTo(bX, bY)
        }
        canvas.drawPath(path, paint)
    }

    private fun pixelSize(context: Context, dp: Float): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics,
        ).toInt().coerceAtLeast(96)

    private companion object {
        const val ICON_DP = 72f
    }
}
