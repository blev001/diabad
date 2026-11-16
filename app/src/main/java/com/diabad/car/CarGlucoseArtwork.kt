package com.diabad.car

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.diabad.core.glucose.formatMmol
import com.diabad.domain.model.AppSettings
import com.diabad.domain.model.GlucoseReading
import kotlin.math.ceil
import kotlin.math.floor

/** Three-hour instrument chart for the image region of the Android Auto pane. */
object CarGlucoseArtwork {
    private const val SIZE = 384
    const val HISTORY_MS = 3 * 60 * 60_000L

    private val background = Color.rgb(16, 19, 24)
    private val primary = Color.rgb(244, 247, 251)
    private val secondary = Color.rgb(178, 191, 207)
    private val mint = Color.rgb(102, 233, 189)

    fun render(
        latest: GlucoseReading?,
        points: List<GlucoseReading>,
        settings: AppSettings,
        now: Long,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val low = settings.hypoThresholdMmol
        val high = settings.hyperThresholdMmol
        val stale = latest == null || now - latest.timestampMillis >= settings.connectionLossGraceMinutes * 60_000L
        val accent = when {
            stale -> secondary
            latest.mmol < low -> Color.rgb(255, 118, 128)
            latest.mmol > high || (settings.approachingHypoEnabled &&
                latest.mmol <= settings.approachingHypoThresholdMmol) -> Color.rgb(255, 193, 108)
            else -> mint
        }

        fun text(value: String, x: Float, y: Float, size: Float, color: Int, bold: Boolean = false) {
            paint.color = color
            paint.textSize = size
            paint.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            canvas.drawText(value, x, y, paint)
        }

        paint.color = background
        canvas.drawRoundRect(RectF(0f, 0f, SIZE.toFloat(), SIZE.toFloat()), 24f, 24f, paint)
        val number = latest?.let { formatMmol(it.mmol) } ?: "—"
        text(number, 20f, 83f, 76f, if (stale) secondary else primary, bold = true)
        val numberEnd = 20f + paint.measureText(number)
        if (!stale && latest != null) {
            text(latest.trend.glyph, numberEnd + 10f, 78f, 43f, accent, bold = true)
        }
        text(
            when {
                latest == null -> "Ожидание OtTai"
                stale -> "ммоль/л · данные устарели"
                else -> "ммоль/л"
            },
            24f, 114f, 23f, secondary,
        )

        val chart = RectF(25f, 144f, 294f, 326f)
        val floorValue = floor(minOf(low - 1, points.minOfOrNull { it.mmol } ?: low)).coerceAtLeast(0.0)
        val ceilingValue = ceil(maxOf(high + 1, points.maxOfOrNull { it.mmol } ?: high))
        val start = now - HISTORY_MS
        fun y(value: Double): Float =
            chart.bottom - ((value - floorValue) / (ceilingValue - floorValue)).toFloat() * chart.height()
        fun x(time: Long): Float = chart.left + (time - start).toFloat() / HISTORY_MS * chart.width()

        paint.color = Color.argb(36, 102, 233, 189)
        canvas.drawRect(chart.left, y(high), chart.right, y(low), paint)
        paint.color = Color.rgb(57, 69, 81)
        paint.strokeWidth = 1.5f
        for (hour in 0..3) {
            val gridX = chart.left + hour * chart.width() / 3f
            canvas.drawLine(gridX, chart.top, gridX, chart.bottom, paint)
        }
        for (threshold in listOf(low, high)) {
            paint.color = Color.rgb(72, 105, 98)
            val gridY = y(threshold)
            canvas.drawLine(chart.left, gridY, chart.right, gridY, paint)
            text(formatMmol(threshold), chart.right + 9f, gridY + 7f, 20f, secondary)
        }

        val lineColor = if (stale) secondary else mint
        val gapMs = settings.connectionLossGraceMinutes * 60_000L
        val segments = mutableListOf<MutableList<GlucoseReading>>()
        for (point in points) {
            val segment = segments.lastOrNull()
            if (segment == null || point.timestampMillis - segment.last().timestampMillis > gapMs) {
                segments.add(mutableListOf(point))
            } else {
                segment.add(point)
            }
        }
        canvas.save()
        canvas.clipRect(chart.left - 6f, chart.top - 6f, chart.right + 6f, chart.bottom)
        for (segment in segments) {
            val first = segment.first()
            val last = segment.last()
            if (segment.size > 1) {
                val trace = Path().apply {
                    moveTo(x(first.timestampMillis), y(first.mmol))
                    for (point in segment.drop(1)) lineTo(x(point.timestampMillis), y(point.mmol))
                }
                val fill = Path(trace).apply {
                    lineTo(x(last.timestampMillis), chart.bottom)
                    lineTo(x(first.timestampMillis), chart.bottom)
                    close()
                }
                paint.color = Color.WHITE
                paint.shader = LinearGradient(0f, chart.top, 0f, chart.bottom,
                    (lineColor and 0x00FFFFFF) or 0x48000000, Color.TRANSPARENT, Shader.TileMode.CLAMP)
                canvas.drawPath(fill, paint)
                paint.shader = null
                paint.color = lineColor
                paint.strokeWidth = 4.5f
                paint.strokeJoin = Paint.Join.ROUND
                paint.strokeCap = Paint.Cap.ROUND
                paint.style = Paint.Style.STROKE
                canvas.drawPath(trace, paint)
                paint.style = Paint.Style.FILL
            }
            if (segment.size == 1 || last == points.last()) {
                paint.color = if (last == points.last()) accent else lineColor
                canvas.drawCircle(x(last.timestampMillis), y(last.mmol), 4.5f, paint)
            }
        }
        canvas.restore()
        text("−3 ч", chart.left, 359f, 22f, secondary)
        text("Сейчас", chart.right - 74f, 359f, 22f, secondary)
        return bitmap
    }
}
