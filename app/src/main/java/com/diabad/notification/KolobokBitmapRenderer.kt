package com.diabad.notification

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.diabad.domain.model.GlucoseZone
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Canvas Kolobok for the monitoring notification large-icon.
 * Faces and motion match the on-screen mascot, driven by [nowMillis].
 */
@Singleton
class KolobokBitmapRenderer @Inject constructor() {

    fun render(
        sizePx: Int,
        zone: GlucoseZone,
        alarming: Boolean,
        nowMillis: Long = System.currentTimeMillis(),
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT)

        val bob = sin(nowMillis / 700.0) * sizePx * 0.03
        val hairSway = sin(nowMillis / 450.0) * 8.0
        val blinkPhase = (nowMillis % 2800L) / 2800f
        val trick = ((nowMillis / 3200L) % 6L).toInt()
        val blinkClosed = when (trick) {
            1 -> blinkPhase in 0.18f..0.24f || blinkPhase in 0.30f..0.36f
            2 -> blinkPhase > 0.55f
            else -> blinkPhase in 0.86f..0.92f
        }
        val hop = sin((nowMillis % 520L) / 520.0 * PI) * sizePx * 0.05
        val spinExtra = if (trick == 3) ((nowMillis % 1400L) / 1400.0 * 360.0) else 0.0
        val slide = if (trick == 4) sin(nowMillis / 400.0) * sizePx * 0.05 else 0.0
        val shakeActive = alarming ||
            zone == GlucoseZone.VERY_LOW ||
            zone == GlucoseZone.LOW ||
            trick == 5
        val pulseActive = zone == GlucoseZone.HIGH ||
            zone == GlucoseZone.VERY_HIGH ||
            trick == 0 ||
            (alarming && zone != GlucoseZone.LOW && zone != GlucoseZone.VERY_LOW)
        val shakePeriod = when {
            alarming || zone == GlucoseZone.VERY_LOW -> 220.0
            zone == GlucoseZone.LOW -> 360.0
            else -> 800.0
        }
        val pulsePeriod = if (zone == GlucoseZone.VERY_HIGH || alarming) 420.0 else 720.0
        val shake = if (shakeActive) {
            sin((nowMillis / shakePeriod) * PI) * sizePx * 0.04
        } else {
            0.0
        }
        val pulse = if (pulseActive) {
            ((sin((nowMillis / pulsePeriod) * PI) * 0.5) + 0.5) * 0.055 + 0.96
        } else {
            1.0
        }

        canvas.save()
        canvas.translate(
            (sizePx / 2f) + shake.toFloat() + slide.toFloat(),
            (sizePx / 2f) + bob.toFloat() - hop.toFloat(),
        )
        canvas.rotate(
            spinExtra.toFloat() +
                if (shakeActive) (0.35 * shake).toFloat() else (0.05 * hairSway).toFloat(),
        )
        canvas.scale(pulse.toFloat(), pulse.toFloat())
        canvas.translate(-sizePx / 2f, -sizePx / 2f)
        drawKolobok(canvas, sizePx, zone, blinkClosed, hairSway.toFloat())
        canvas.restore()
        return bitmap
    }

    private fun drawKolobok(
        canvas: Canvas,
        size: Int,
        zone: GlucoseZone,
        blinkClosed: Boolean,
        hairSwayDeg: Float,
    ) {
        val cx = size * 0.5f
        val cy = size * 0.58f
        val r = size * 0.36f
        val body = when (zone) {
            GlucoseZone.VERY_LOW -> COLOR_SICK
            GlucoseZone.LOW -> COLOR_LOW
            GlucoseZone.HIGH -> COLOR_HOT
            GlucoseZone.VERY_HIGH -> COLOR_FIRE
            else -> COLOR_YELLOW
        }

        canvas.drawCircle(cx + r * 0.06f, cy + r * 0.12f, r, fill(Color.argb(46, 0, 0, 0)))
        canvas.drawCircle(cx, cy, r, fill(body))
        canvas.drawCircle(cx, cy, r, stroke(COLOR_OUTLINE, r * 0.08f))
        canvas.drawOval(
            RectF(cx - r * 0.42f, cy - r * 0.48f, cx - r * 0.04f, cy - r * 0.20f),
            fill(Color.argb(217, 255, 246, 176)),
        )
        canvas.drawCircle(cx - r * 0.28f, cy - r * 0.32f, r * 0.08f, fill(Color.argb(140, 255, 255, 255)))

        canvas.save()
        canvas.rotate(hairSwayDeg * 0.35f, cx, cy - r)
        drawHair(canvas, cx - r * 0.18f, cy - r * 0.92f, -18f, r)
        drawHair(canvas, cx, cy - r * 1.02f, 0f, r)
        drawHair(canvas, cx + r * 0.18f, cy - r * 0.92f, 18f, r)
        canvas.restore()

        when (zone) {
            GlucoseZone.VERY_LOW -> drawDizzyFace(canvas, cx, cy, r, blinkClosed)
            GlucoseZone.LOW -> drawSadFace(canvas, cx, cy, r, blinkClosed)
            GlucoseZone.HIGH -> drawWorriedFace(canvas, cx, cy, r, blinkClosed)
            GlucoseZone.VERY_HIGH -> drawPanicFace(canvas, cx, cy, r, blinkClosed)
            GlucoseZone.UNKNOWN -> drawNeutralFace(canvas, cx, cy, r, blinkClosed)
            GlucoseZone.IN_RANGE -> drawHappyFace(canvas, cx, cy, r, blinkClosed)
        }
    }

    private fun drawHair(canvas: Canvas, x: Float, y: Float, tiltDeg: Float, r: Float) {
        val rad = Math.toRadians(tiltDeg.toDouble()).toFloat()
        val tipX = x + sin(rad) * r * 0.28f
        val tipY = y - cos(rad) * r * 0.28f
        canvas.drawLine(x, y + r * 0.08f, tipX, tipY, stroke(COLOR_OUTLINE, r * 0.06f))
    }

    private fun drawEye(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        r: Float,
        blinkClosed: Boolean,
        pupilScale: Float = 1f,
    ) {
        if (blinkClosed) {
            canvas.drawLine(
                centerX - r * 0.12f,
                centerY,
                centerX + r * 0.12f,
                centerY,
                stroke(COLOR_OUTLINE, r * 0.07f),
            )
            return
        }
        canvas.drawOval(
            RectF(centerX - r * 0.13f, centerY - r * 0.16f, centerX + r * 0.13f, centerY + r * 0.16f),
            fill(Color.WHITE),
        )
        canvas.drawCircle(centerX, centerY, r * 0.09f * pupilScale, fill(COLOR_OUTLINE))
        canvas.drawCircle(centerX - r * 0.03f, centerY - r * 0.04f, r * 0.035f, fill(Color.WHITE))
    }

    private fun strokePath(canvas: Canvas, path: Path, width: Float) {
        canvas.drawPath(path, stroke(COLOR_OUTLINE, width))
    }

    private fun drawHappyFace(canvas: Canvas, cx: Float, cy: Float, r: Float, blink: Boolean) {
        drawEye(canvas, cx - r * 0.28f, cy - r * 0.08f, r, blink)
        drawEye(canvas, cx + r * 0.28f, cy - r * 0.08f, r, blink)
        val smile = Path().apply {
            moveTo(cx - r * 0.32f, cy + r * 0.22f)
            quadTo(cx, cy + r * 0.52f, cx + r * 0.32f, cy + r * 0.22f)
        }
        strokePath(canvas, smile, r * 0.08f)
    }

    private fun drawNeutralFace(canvas: Canvas, cx: Float, cy: Float, r: Float, blink: Boolean) {
        drawEye(canvas, cx - r * 0.28f, cy - r * 0.06f, r, blink, pupilScale = 0.85f)
        drawEye(canvas, cx + r * 0.28f, cy - r * 0.06f, r, blink, pupilScale = 0.85f)
        canvas.drawLine(
            cx - r * 0.22f,
            cy + r * 0.28f,
            cx + r * 0.22f,
            cy + r * 0.28f,
            stroke(COLOR_OUTLINE, r * 0.07f),
        )
    }

    private fun drawSadFace(canvas: Canvas, cx: Float, cy: Float, r: Float, blink: Boolean) {
        drawEye(canvas, cx - r * 0.28f, cy - r * 0.02f, r, blink, pupilScale = 0.9f)
        drawEye(canvas, cx + r * 0.28f, cy - r * 0.02f, r, blink, pupilScale = 0.9f)
        val brow = stroke(COLOR_OUTLINE, r * 0.06f)
        canvas.drawLine(cx - r * 0.42f, cy - r * 0.28f, cx - r * 0.12f, cy - r * 0.22f, brow)
        canvas.drawLine(cx + r * 0.12f, cy - r * 0.22f, cx + r * 0.42f, cy - r * 0.28f, brow)
        val frown = Path().apply {
            moveTo(cx - r * 0.28f, cy + r * 0.38f)
            quadTo(cx, cy + r * 0.18f, cx + r * 0.28f, cy + r * 0.38f)
        }
        strokePath(canvas, frown, r * 0.08f)
    }

    private fun drawDizzyFace(canvas: Canvas, cx: Float, cy: Float, r: Float, blink: Boolean) {
        if (blink) {
            drawEye(canvas, cx - r * 0.28f, cy - r * 0.04f, r, blinkClosed = true)
            drawEye(canvas, cx + r * 0.28f, cy - r * 0.04f, r, blinkClosed = true)
        } else {
            drawSpiralEye(canvas, cx - r * 0.28f, cy - r * 0.04f, r * 0.16f)
            drawSpiralEye(canvas, cx + r * 0.28f, cy - r * 0.04f, r * 0.16f)
        }
        canvas.drawOval(
            RectF(cx - r * 0.14f, cy + r * 0.22f, cx + r * 0.14f, cy + r * 0.44f),
            fill(COLOR_OUTLINE),
        )
        canvas.drawOval(
            RectF(cx + r * 0.55f, cy - r * 0.35f, cx + r * 0.69f, cy - r * 0.13f),
            fill(Color.parseColor("#5AC8FA")),
        )
    }

    private fun drawSpiralEye(canvas: Canvas, centerX: Float, centerY: Float, radius: Float) {
        val path = Path()
        val end = 4.2f * PI.toFloat()
        var a = 0f
        while (a < end) {
            val t = a / end
            val rr = radius * t
            val x = centerX + cos(a) * rr
            val y = centerY + sin(a) * rr
            if (a == 0f) path.moveTo(x, y) else path.lineTo(x, y)
            a += 0.25f
        }
        strokePath(canvas, path, radius * 0.22f)
    }

    private fun drawWorriedFace(canvas: Canvas, cx: Float, cy: Float, r: Float, blink: Boolean) {
        drawEye(canvas, cx - r * 0.28f, cy - r * 0.10f, r, blink, pupilScale = 1.15f)
        drawEye(canvas, cx + r * 0.28f, cy - r * 0.10f, r, blink, pupilScale = 1.15f)
        val brow = stroke(COLOR_OUTLINE, r * 0.06f)
        canvas.drawLine(cx - r * 0.42f, cy - r * 0.34f, cx - r * 0.10f, cy - r * 0.26f, brow)
        canvas.drawLine(cx + r * 0.10f, cy - r * 0.26f, cx + r * 0.42f, cy - r * 0.34f, brow)
        canvas.drawOval(
            RectF(cx - r * 0.16f, cy + r * 0.18f, cx + r * 0.16f, cy + r * 0.52f),
            fill(COLOR_OUTLINE),
        )
        canvas.drawOval(
            RectF(cx - r * 0.10f, cy + r * 0.24f, cx + r * 0.10f, cy + r * 0.46f),
            fill(Color.parseColor("#4A1020")),
        )
    }

    private fun drawPanicFace(canvas: Canvas, cx: Float, cy: Float, r: Float, blink: Boolean) {
        if (blink) {
            drawEye(canvas, cx - r * 0.31f, cy - r * 0.08f, r, blinkClosed = true)
            drawEye(canvas, cx + r * 0.31f, cy - r * 0.08f, r, blinkClosed = true)
        } else {
            canvas.drawOval(
                RectF(cx - r * 0.48f, cy - r * 0.28f, cx - r * 0.14f, cy + r * 0.10f),
                fill(Color.WHITE),
            )
            canvas.drawOval(
                RectF(cx + r * 0.14f, cy - r * 0.28f, cx + r * 0.48f, cy + r * 0.10f),
                fill(Color.WHITE),
            )
            canvas.drawCircle(cx - r * 0.31f, cy - r * 0.08f, r * 0.10f, fill(COLOR_OUTLINE))
            canvas.drawCircle(cx + r * 0.31f, cy - r * 0.08f, r * 0.10f, fill(COLOR_OUTLINE))
        }
        canvas.drawOval(
            RectF(cx - r * 0.22f, cy + r * 0.12f, cx + r * 0.22f, cy + r * 0.54f),
            fill(COLOR_OUTLINE),
        )
        canvas.drawOval(
            RectF(cx - r * 0.14f, cy + r * 0.20f, cx + r * 0.14f, cy + r * 0.48f),
            fill(Color.parseColor("#3A0810")),
        )
        val hand = fill(COLOR_FIRE)
        val handOutline = stroke(COLOR_OUTLINE, r * 0.05f)
        canvas.drawCircle(cx - r * 0.78f, cy - r * 0.55f, r * 0.16f, hand)
        canvas.drawCircle(cx - r * 0.78f, cy - r * 0.55f, r * 0.16f, handOutline)
        canvas.drawCircle(cx + r * 0.78f, cy - r * 0.55f, r * 0.16f, hand)
        canvas.drawCircle(cx + r * 0.78f, cy - r * 0.55f, r * 0.16f, handOutline)
    }

    private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }

    private fun stroke(color: Int, width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = width
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private companion object {
        val COLOR_YELLOW = Color.parseColor("#FFD400")
        val COLOR_SICK = Color.parseColor("#B8D44A")
        val COLOR_LOW = Color.parseColor("#E8D040")
        val COLOR_HOT = Color.parseColor("#FFB020")
        val COLOR_FIRE = Color.parseColor("#FF6A3D")
        val COLOR_OUTLINE = Color.parseColor("#2A2208")
    }
}
