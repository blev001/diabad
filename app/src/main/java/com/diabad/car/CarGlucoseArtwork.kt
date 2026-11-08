package com.diabad.car

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import com.diabad.core.glucose.formatMmol
import com.diabad.domain.model.AppSettings
import com.diabad.domain.model.GlucoseReading

/** A glanceable instrument image inside the host's standard Pane template. */
object CarGlucoseArtwork {
    fun render(latest: GlucoseReading?, history: List<GlucoseReading>, settings: AppSettings, now: Long): Bitmap {
        val bitmap = Bitmap.createBitmap(480, 480, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val stale = latest == null || now - latest.timestampMillis >= settings.connectionLossGraceMinutes * 60_000L
        val accent = when {
            stale -> Color.rgb(157, 170, 186)
            latest!!.mmol < settings.hypoThresholdMmol -> Color.rgb(255, 101, 111)
            latest.mmol > settings.hyperThresholdMmol -> Color.rgb(255, 189, 97)
            settings.approachingHypoEnabled && latest.mmol <= settings.approachingHypoThresholdMmol -> Color.rgb(255, 189, 97)
            else -> Color.rgb(87, 224, 180)
        }
        paint.color = Color.rgb(16, 25, 37)
        canvas.drawRoundRect(RectF(0f, 0f, 480f, 480f), 40f, 40f, paint)
        fun text(value: String, y: Float, size: Float, color: Int, bold: Boolean = false) {
            paint.color = color
            paint.textSize = size
            paint.typeface = if (bold) Typeface.create("sans-serif", Typeface.BOLD) else Typeface.create("sans-serif", Typeface.NORMAL)
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(value, 240f, y, paint)
        }
        text(if (stale) "ПОСЛЕДНЕЕ ИЗМЕРЕНИЕ" else "ГЛЮКОЗА", 48f, 20f, Color.LTGRAY)
        text(latest?.let { formatMmol(it.mmol) } ?: "—", 175f, 112f, accent, true)
        text(if (stale) "ммоль/л · нет свежих данных" else "ммоль/л  ${latest?.trend?.glyph.orEmpty()}", 217f, 25f, Color.WHITE)
        // Thresholds come from the user's settings. The indicator is clamped to the scale.
        val low = settings.hypoThresholdMmol.toFloat()
        val high = settings.hyperThresholdMmol.toFloat()
        val max = maxOf(16f, high + 2f)
        fun x(value: Float) = 40f + value.coerceIn(0f, max) / max * 400f
        val colors = listOf(Color.rgb(255,101,111), Color.rgb(87,224,180), Color.rgb(255,189,97))
        val edges = listOf(0f, low, high, max)
        for (i in 0..2) {
            paint.color = colors[i]
            paint.alpha = 140
            canvas.drawRect(x(edges[i]), 248f, x(edges[i+1]), 262f, paint)
        }
        paint.alpha = 255
        if (latest != null) {
            paint.color = accent
            canvas.drawCircle(x(latest.mmol.toFloat()), 255f, 11f, paint)
        }
        text("Цель ${formatMmol(low.toDouble())}–${formatMmol(high.toDouble())}", 297f, 22f, Color.LTGRAY)
        val from = now - 3 * 60 * 60_000L
        val points = history.filter { it.timestampMillis in from..now && it.mmol.isFinite() }
        if (points.size >= 2) {
            val minY = minOf(low.toDouble() - 1, points.minOf { it.mmol }).coerceAtLeast(0.0)
            val maxY = maxOf(high.toDouble() + 1, points.maxOf { it.mmol })
            fun py(v: Double) = (409 - (v - minY) / (maxY - minY).coerceAtLeast(1.0) * 78).toFloat()
            fun px(t: Long) = 40f + (t - from).toFloat() / (now - from) * 400f
            paint.color = Color.rgb(87,224,180)
            paint.alpha = 26
            canvas.drawRect(40f, py(high.toDouble()), 440f, py(low.toDouble()), paint)
            paint.alpha = 255
            paint.color = accent
            paint.strokeWidth = 4f
            paint.style = Paint.Style.STROKE
            val path = Path()
            points.forEachIndexed { i, p ->
                // Do not draw continuity through a sensor outage.
                if (i == 0 || p.timestampMillis - points[i-1].timestampMillis > settings.connectionLossGraceMinutes * 60_000L)
                    path.moveTo(px(p.timestampMillis), py(p.mmol))
                else path.lineTo(px(p.timestampMillis), py(p.mmol))
            }
            canvas.drawPath(path, paint)
            paint.style = Paint.Style.FILL
            text("ПОСЛЕДНИЕ 3 ЧАСА", 451f, 18f, Color.LTGRAY)
        } else text("Ожидание истории", 386f, 22f, Color.LTGRAY)
        return bitmap
    }
}
