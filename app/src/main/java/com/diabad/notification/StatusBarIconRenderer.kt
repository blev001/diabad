package com.diabad.notification

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.TypedValue
import com.diabad.core.glucose.formatMmol
import com.diabad.domain.model.GlucoseReading
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monochrome white-on-transparent badge for notification smallIcon (status bar).
 */
@Singleton
class StatusBarIconRenderer @Inject constructor() {

    private var cachedKey: String? = null
    private var cachedBitmap: Bitmap? = null

    fun render(context: Context, reading: GlucoseReading?): Bitmap {
        val key = reading?.let { "${formatMmol(it.mmol)}|${it.trend.name}" } ?: "—"
        cachedBitmap?.let { if (cachedKey == key) return it }

        val sizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            ICON_DP,
            context.resources.displayMetrics,
        ).toInt().coerceAtLeast(48)

        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

        val value = reading?.let { formatMmol(it.mmol) } ?: "—"
        val arrow = reading?.trend?.glyph.orEmpty()

        if (arrow.isEmpty()) {
            paint.textSize = sizePx * 0.42f
            val y = sizePx / 2f - (paint.descent() + paint.ascent()) / 2f
            canvas.drawText(value, sizePx / 2f, y, paint)
        } else {
            paint.textSize = sizePx * 0.36f
            val valueY = sizePx * 0.42f - (paint.descent() + paint.ascent()) / 2f
            canvas.drawText(value, sizePx / 2f, valueY, paint)

            paint.textSize = sizePx * 0.34f
            val arrowY = sizePx * 0.78f - (paint.descent() + paint.ascent()) / 2f
            canvas.drawText(arrow, sizePx / 2f, arrowY, paint)
        }

        cachedKey = key
        cachedBitmap = bitmap
        return bitmap
    }

    private companion object {
        const val ICON_DP = 24f
    }
}
