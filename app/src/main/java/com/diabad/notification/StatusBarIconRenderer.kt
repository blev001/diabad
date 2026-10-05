package com.diabad.notification

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Typeface
import android.util.TypedValue
import com.diabad.core.glucose.formatMmol
import com.diabad.domain.model.GlucoseReading
import com.diabad.domain.model.TrendArrow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Monochrome white-on-transparent badge for notification smallIcon (status bar).
 * The system tints it, so only alpha carries meaning: old data is dimmed and struck through.
 */
@Singleton
class StatusBarIconRenderer @Inject constructor() {

    private var cachedKey: String? = null
    private var cachedBitmap: Bitmap? = null

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        textAlign = Paint.Align.LEFT
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val bounds = Rect()

    fun render(
        context: Context,
        reading: GlucoseReading?,
        nowMillis: Long = System.currentTimeMillis(),
        lostAfterMinutes: Int = DEFAULT_LOST_MINUTES,
    ): Bitmap {
        val ageMinutes = reading?.let {
            TimeUnit.MILLISECONDS.toMinutes((nowMillis - it.timestampMillis).coerceAtLeast(0L))
        }
        val freshness = when {
            reading == null || ageMinutes == null -> Freshness.NONE
            ageMinutes >= lostAfterMinutes.toLong().coerceAtLeast(STALE_MINUTES + 1) -> Freshness.LOST
            ageMinutes >= STALE_MINUTES -> Freshness.STALE
            else -> Freshness.FRESH
        }
        val value = when (freshness) {
            Freshness.NONE -> "—"
            Freshness.LOST -> "– –"
            else -> statusValue(reading!!.mmol)
        }
        val ageLabel = ageMinutes?.let(::ageLabel).orEmpty()
        val trend = reading?.trend ?: TrendArrow.NONE
        val key = "$value|${trend.name}|$freshness|$ageLabel"
        cachedBitmap?.let { if (cachedKey == key) return it }

        val size = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            ICON_DP,
            context.resources.displayMetrics,
        ).toInt().coerceAtLeast(48)
        val s = size.toFloat()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        when (freshness) {
            Freshness.NONE -> drawFitted(canvas, value, 0f, s * 0.2f, s, s * 0.6f, 255)
            Freshness.FRESH -> {
                val angle = arrowAngle(trend)
                if (angle == null) {
                    drawFitted(canvas, value, 0f, s * 0.12f, s, s * 0.76f, 255)
                } else {
                    drawFitted(canvas, value, 0f, 0f, s, s * 0.58f, 255)
                    val doubled = trend == TrendArrow.DOUBLE_UP || trend == TrendArrow.DOUBLE_DOWN
                    drawArrow(canvas, s, centerY = s * 0.8f, angle = angle, doubled = doubled)
                }
            }
            Freshness.STALE -> {
                drawFitted(canvas, value, 0f, 0f, s, s * 0.58f, STALE_ALPHA)
                strokePaint.strokeWidth = s * 0.07f
                strokePaint.alpha = 255
                canvas.drawLine(s * 0.04f, s * 0.54f, s * 0.96f, s * 0.06f, strokePaint)
                drawFitted(canvas, ageLabel, s * 0.12f, s * 0.68f, s * 0.76f, s * 0.32f, 255)
            }
            Freshness.LOST -> {
                drawFitted(canvas, value, s * 0.1f, s * 0.12f, s * 0.8f, s * 0.36f, 255)
                drawFitted(canvas, ageLabel, s * 0.08f, s * 0.64f, s * 0.84f, s * 0.36f, 255)
            }
        }

        cachedKey = key
        cachedBitmap = bitmap
        return bitmap
    }

    /** Largest text whose ink fits the box, centred on its real glyph bounds. */
    private fun drawFitted(canvas: Canvas, text: String, left: Float, top: Float, width: Float, height: Float, alpha: Int) {
        if (text.isEmpty()) return
        textPaint.alpha = alpha
        textPaint.textSize = PROBE_TEXT_SIZE
        textPaint.getTextBounds(text, 0, text.length, bounds)
        if (bounds.width() == 0 || bounds.height() == 0) return
        val scale = min(width / bounds.width(), height / bounds.height())
        textPaint.textSize = PROBE_TEXT_SIZE * scale
        textPaint.getTextBounds(text, 0, text.length, bounds)
        val x = left + width / 2f - (bounds.left + bounds.right) / 2f
        val y = top + height / 2f - (bounds.top + bounds.bottom) / 2f
        canvas.drawText(text, x, y, textPaint)
    }

    private fun drawArrow(canvas: Canvas, s: Float, centerY: Float, angle: Float, doubled: Boolean) {
        val rad = Math.toRadians(angle.toDouble())
        val dx = cos(rad).toFloat()
        val dy = sin(rad).toFloat()
        val centers = if (doubled) listOf(s * 0.28f, s * 0.72f) else listOf(s * 0.5f)
        // The arrow row is wide but short: horizontal arrows can be much longer than vertical ones.
        val length = when {
            doubled -> s * 0.36f
            abs(dy) < 0.01f -> s * 0.6f
            abs(dx) > 0.01f -> s * 0.44f
            else -> s * 0.36f
        }
        strokePaint.strokeWidth = s * 0.1f
        strokePaint.alpha = 255
        for (cx in centers) {
            val tailX = cx - dx * length / 2f
            val tailY = centerY - dy * length / 2f
            val tipX = cx + dx * length / 2f
            val tipY = centerY + dy * length / 2f
            val head = s * 0.17f
            val baseX = tipX - dx * head
            val baseY = tipY - dy * head
            canvas.drawLine(tailX, tailY, baseX, baseY, strokePaint)
            val half = head * 0.75f
            val path = Path().apply {
                moveTo(tipX, tipY)
                lineTo(baseX - dy * half, baseY + dx * half)
                lineTo(baseX + dy * half, baseY - dx * half)
                close()
            }
            canvas.drawPath(path, fillPaint)
        }
    }

    /** Screen angle in degrees (0 = right, 90 = down), or null when there is no usable trend. */
    private fun arrowAngle(trend: TrendArrow): Float? = when (trend) {
        TrendArrow.DOUBLE_UP, TrendArrow.SINGLE_UP -> -90f
        TrendArrow.FORTY_FIVE_UP -> -45f
        TrendArrow.FLAT -> 0f
        TrendArrow.FORTY_FIVE_DOWN -> 45f
        TrendArrow.SINGLE_DOWN, TrendArrow.DOUBLE_DOWN -> 90f
        TrendArrow.NONE, TrendArrow.NOT_COMPUTABLE, TrendArrow.RATE_OUT_OF_RANGE -> null
    }

    private enum class Freshness { NONE, FRESH, STALE, LOST }

    companion object {
        private const val ICON_DP = 24f
        private const val PROBE_TEXT_SIZE = 100f
        /** Two missed 5-min readings. */
        const val STALE_MINUTES = 11L
        private const val DEFAULT_LOST_MINUTES = 20
        private const val STALE_ALPHA = 110

        /** One decimal for the shade / status-bar badge, including values from 10 up. */
        fun statusValue(mmol: Double): String = formatMmol(mmol)

        /** Text for the Live Update status-bar chip, same freshness rules as the icon. */
        fun chipText(reading: GlucoseReading?, nowMillis: Long): String {
            if (reading == null) return "—"
            val age = TimeUnit.MILLISECONDS.toMinutes((nowMillis - reading.timestampMillis).coerceAtLeast(0L))
            return if (age >= STALE_MINUTES) {
                "${formatMmol(reading.mmol)} · ${ageLabel(age)}"
            } else {
                formatMmol(reading.mmol) + reading.trend.glyph
            }
        }

        private fun ageLabel(minutes: Long): String =
            if (minutes < 60) "${minutes}м" else "${minutes / 60}ч"
    }
}
