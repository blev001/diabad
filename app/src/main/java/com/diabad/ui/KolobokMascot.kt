package com.diabad.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import android.os.PowerManager
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.diabad.domain.model.GlucoseZone
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val KolobokYellow = Color(0xFFFFCE42)
private val KolobokYellowSick = Color(0xFFB8D44A)
private val KolobokYellowHot = Color(0xFFFFB020)
private val KolobokYellowFire = Color(0xFFFF6A3D)
private val KolobokOutline = Color(0xFF56341A)
private val KolobokHighlight = Color(0xFFFFF6B0)

/**
 * Sculpted sphere, three hairs and the original six zone expressions.
 * Cached drawing is independent of the single optional transform animation.
 * Idle in-range is static. Motion only when the zone is off-target or alarming.
 */
@Composable
fun KolobokMascot(
    zone: GlucoseZone,
    alarming: Boolean,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
) {
    val context = LocalContext.current
    val powerManager = remember(context) { context.getSystemService(PowerManager::class.java) }
    val motionOn = animate && animationsEnabled() && !powerManager.isPowerSaveMode && (
        alarming || zone == GlucoseZone.VERY_LOW || zone == GlucoseZone.LOW ||
            zone == GlucoseZone.HIGH || zone == GlucoseZone.VERY_HIGH
        )
    // One clock replaces five. Only layer transforms animate; the face stays cached.
    val motion = loopingFloat(
        enabled = motionOn,
        from = -1f,
        to = 1f,
        durationMs = if (alarming || zone == GlucoseZone.VERY_LOW) 360 else 900,
        label = "kolobokMotion",
        resting = 0f,
    )

    val shakeActive = alarming ||
        zone == GlucoseZone.VERY_LOW ||
        zone == GlucoseZone.LOW
    val pulseActive = zone == GlucoseZone.HIGH ||
        zone == GlucoseZone.VERY_HIGH ||
        (alarming && zone != GlucoseZone.LOW && zone != GlucoseZone.VERY_LOW)

    val artwork = remember(zone) {
        Modifier.drawWithCache {
            // Record geometry once per size/zone; frame ticks never invalidate it.
            val picture = android.graphics.Picture()
            val recording = picture.beginRecording(size.width.toInt().coerceAtLeast(1), size.height.toInt().coerceAtLeast(1))
            val drawScope = androidx.compose.ui.graphics.drawscope.CanvasDrawScope()
            drawScope.draw(this, layoutDirection, androidx.compose.ui.graphics.Canvas(recording), size) {
                drawKolobok(zone = zone, blinkClosed = false, hairSwayDeg = 0f)
            }
            picture.endRecording()
            onDrawBehind {
                drawContext.canvas.nativeCanvas.drawPicture(picture)
            }
        }
    }

    Box(
        modifier = modifier.size(52.dp)
            .graphicsLayer {
                translationX = if (shakeActive) motion * 3f else 0f
                rotationZ = if (shakeActive) motion * 1.2f else 0f
                val scale = if (pulseActive) 1f + motion * 0.025f else 1f
                scaleX = scale
                scaleY = scale
            }
            .then(artwork),
    )
}

private fun DrawScope.drawKolobok(
    zone: GlucoseZone,
    blinkClosed: Boolean,
    hairSwayDeg: Float,
) {
    val cx = size.width * 0.50f
    val cy = size.height * 0.58f
    val r = size.minDimension * 0.36f
    val body = when (zone) {
        GlucoseZone.VERY_LOW -> KolobokYellowSick
        GlucoseZone.LOW -> Color(0xFFE8D040)
        GlucoseZone.HIGH -> KolobokYellowHot
        GlucoseZone.VERY_HIGH -> KolobokYellowFire
        else -> KolobokYellow
    }

    // Ground contact and a radial lit surface create depth without a 3D renderer.
    drawOval(
        brush = Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.24f), Color.Transparent),
            center = Offset(cx, cy + r * 0.90f), radius = r),
        topLeft = Offset(cx - r * 0.86f, cy + r * 0.72f),
        size = Size(r * 1.72f, r * 0.38f),
    )
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to lerp(body, Color.White, 0.65f),
                0.36f to lerp(body, KolobokHighlight, 0.28f),
                0.66f to body,
                1f to lerp(body, Color(0xFF6B2615), 0.65f),
            ),
            center = Offset(cx - r * 0.35f, cy - r * 0.42f),
            radius = r * 1.65f,
        ),
        radius = r, center = Offset(cx, cy),
    )
    drawCircle(
        brush = Brush.radialGradient(listOf(Color.Transparent, Color(0xFF7B351B).copy(alpha = 0.26f)),
            center = Offset(cx - r * 0.2f, cy - r * 0.3f), radius = r * 1.35f),
        radius = r, center = Offset(cx, cy),
    )
    drawOval(
        brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.65f), Color.Transparent),
            center = Offset(cx - r * 0.32f, cy - r * 0.49f), radius = r * 0.44f),
        topLeft = Offset(cx - r * 0.73f, cy - r * 0.80f),
        size = Size(r * 0.88f, r * 0.58f),
    )
    // Soft cheeks keep the original facial expressions legible at 52 dp.
    for (side in listOf(-1f, 1f)) {
        val cheek = Offset(cx + side * r * 0.56f, cy + r * 0.13f)
        drawCircle(Brush.radialGradient(listOf(Color(0xFFF48150).copy(alpha = 0.38f), Color.Transparent),
            center = cheek, radius = r * 0.25f), radius = r * 0.25f, center = cheek)
    }

    // Three ICQ hairs
    rotate(degrees = hairSwayDeg * 0.35f, pivot = Offset(cx, cy - r)) {
        drawHair(cx - r * 0.18f, cy - r * 0.92f, -18f, r)
        drawHair(cx, cy - r * 1.02f, 0f, r)
        drawHair(cx + r * 0.18f, cy - r * 0.92f, 18f, r)
    }

    when (zone) {
        GlucoseZone.UNKNOWN -> drawNeutralFace(cx, cy, r, blinkClosed)
        GlucoseZone.IN_RANGE -> drawHappyFace(cx, cy, r, blinkClosed)
        GlucoseZone.LOW -> drawSadFace(cx, cy, r, blinkClosed)
        GlucoseZone.VERY_LOW -> drawDizzyFace(cx, cy, r, blinkClosed)
        GlucoseZone.HIGH -> drawWorriedFace(cx, cy, r, blinkClosed)
        GlucoseZone.VERY_HIGH -> drawPanicFace(cx, cy, r, blinkClosed)
    }
}

private fun DrawScope.drawHair(x: Float, y: Float, tiltDeg: Float, r: Float) {
    val rad = tiltDeg * (PI.toFloat() / 180f)
    val tipX = x + sin(rad) * r * 0.28f
    val tipY = y - cos(rad) * r * 0.28f
    drawLine(
        color = KolobokOutline,
        start = Offset(x, y + r * 0.08f),
        end = Offset(tipX, tipY),
        strokeWidth = r * 0.06f,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawEye(
    center: Offset,
    r: Float,
    blinkClosed: Boolean,
    pupilScale: Float = 1f,
) {
    if (blinkClosed) {
        drawLine(
            color = KolobokOutline,
            start = Offset(center.x - r * 0.12f, center.y),
            end = Offset(center.x + r * 0.12f, center.y),
            strokeWidth = r * 0.07f,
            cap = StrokeCap.Round,
        )
    } else {
        drawOval(
            brush = Brush.radialGradient(listOf(Color.White, Color(0xFFE2D5BB)),
                center = Offset(center.x - r * 0.04f, center.y - r * 0.06f), radius = r * 0.26f),
            topLeft = Offset(center.x - r * 0.13f, center.y - r * 0.16f),
            size = Size(r * 0.26f, r * 0.32f),
        )
        drawCircle(
            color = KolobokOutline,
            radius = r * 0.09f * pupilScale,
            center = center,
        )
        drawCircle(
            color = Color.White,
            radius = r * 0.035f,
            center = Offset(center.x - r * 0.03f, center.y - r * 0.04f),
        )
    }
}

private fun DrawScope.drawHappyFace(cx: Float, cy: Float, r: Float, blink: Boolean) {
    drawEye(Offset(cx - r * 0.28f, cy - r * 0.08f), r, blink)
    drawEye(Offset(cx + r * 0.28f, cy - r * 0.08f), r, blink)
    val smile = Path().apply {
        moveTo(cx - r * 0.32f, cy + r * 0.22f)
        quadraticTo(cx, cy + r * 0.52f, cx + r * 0.32f, cy + r * 0.22f)
    }
    drawPath(smile, KolobokOutline, style = Stroke(width = r * 0.08f, cap = StrokeCap.Round))
}

private fun DrawScope.drawNeutralFace(cx: Float, cy: Float, r: Float, blink: Boolean) {
    drawEye(Offset(cx - r * 0.28f, cy - r * 0.06f), r, blink, pupilScale = 0.85f)
    drawEye(Offset(cx + r * 0.28f, cy - r * 0.06f), r, blink, pupilScale = 0.85f)
    drawLine(
        color = KolobokOutline,
        start = Offset(cx - r * 0.22f, cy + r * 0.28f),
        end = Offset(cx + r * 0.22f, cy + r * 0.28f),
        strokeWidth = r * 0.07f,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawSadFace(cx: Float, cy: Float, r: Float, blink: Boolean) {
    drawEye(Offset(cx - r * 0.28f, cy - r * 0.02f), r, blink, pupilScale = 0.9f)
    drawEye(Offset(cx + r * 0.28f, cy - r * 0.02f), r, blink, pupilScale = 0.9f)
    // Brow droop
    drawLine(
        KolobokOutline,
        Offset(cx - r * 0.42f, cy - r * 0.28f),
        Offset(cx - r * 0.12f, cy - r * 0.22f),
        strokeWidth = r * 0.06f,
        cap = StrokeCap.Round,
    )
    drawLine(
        KolobokOutline,
        Offset(cx + r * 0.12f, cy - r * 0.22f),
        Offset(cx + r * 0.42f, cy - r * 0.28f),
        strokeWidth = r * 0.06f,
        cap = StrokeCap.Round,
    )
    val frown = Path().apply {
        moveTo(cx - r * 0.28f, cy + r * 0.38f)
        quadraticTo(cx, cy + r * 0.18f, cx + r * 0.28f, cy + r * 0.38f)
    }
    drawPath(frown, KolobokOutline, style = Stroke(width = r * 0.08f, cap = StrokeCap.Round))
}

private fun DrawScope.drawDizzyFace(cx: Float, cy: Float, r: Float, blink: Boolean) {
    if (blink) {
        drawEye(Offset(cx - r * 0.28f, cy - r * 0.04f), r, true)
        drawEye(Offset(cx + r * 0.28f, cy - r * 0.04f), r, true)
    } else {
        drawSpiralEye(Offset(cx - r * 0.28f, cy - r * 0.04f), r * 0.16f)
        drawSpiralEye(Offset(cx + r * 0.28f, cy - r * 0.04f), r * 0.16f)
    }
    // Open weak mouth
    drawOval(
        color = KolobokOutline,
        topLeft = Offset(cx - r * 0.14f, cy + r * 0.22f),
        size = Size(r * 0.28f, r * 0.22f),
    )
    // Sweat drop
    drawOval(
        color = Color(0xFF5AC8FA),
        topLeft = Offset(cx + r * 0.55f, cy - r * 0.35f),
        size = Size(r * 0.14f, r * 0.22f),
    )
}

private fun DrawScope.drawSpiralEye(center: Offset, radius: Float) {
    val path = Path()
    var a = 0f
    while (a < 4.2f * PI.toFloat()) {
        val t = a / (4.2f * PI.toFloat())
        val rr = radius * t
        val x = center.x + cos(a) * rr
        val y = center.y + sin(a) * rr
        if (a == 0f) path.moveTo(x, y) else path.lineTo(x, y)
        a += 0.25f
    }
    drawPath(path, KolobokOutline, style = Stroke(width = radius * 0.22f, cap = StrokeCap.Round))
}

private fun DrawScope.drawWorriedFace(cx: Float, cy: Float, r: Float, blink: Boolean) {
    drawEye(Offset(cx - r * 0.28f, cy - r * 0.10f), r, blink, pupilScale = 1.15f)
    drawEye(Offset(cx + r * 0.28f, cy - r * 0.10f), r, blink, pupilScale = 1.15f)
    drawLine(
        KolobokOutline,
        Offset(cx - r * 0.42f, cy - r * 0.34f),
        Offset(cx - r * 0.10f, cy - r * 0.26f),
        strokeWidth = r * 0.06f,
        cap = StrokeCap.Round,
    )
    drawLine(
        KolobokOutline,
        Offset(cx + r * 0.10f, cy - r * 0.26f),
        Offset(cx + r * 0.42f, cy - r * 0.34f),
        strokeWidth = r * 0.06f,
        cap = StrokeCap.Round,
    )
    // Open O mouth
    drawOval(
        color = KolobokOutline,
        topLeft = Offset(cx - r * 0.16f, cy + r * 0.18f),
        size = Size(r * 0.32f, r * 0.34f),
    )
    drawOval(
        color = Color(0xFF4A1020),
        topLeft = Offset(cx - r * 0.10f, cy + r * 0.24f),
        size = Size(r * 0.20f, r * 0.22f),
    )
}

private fun DrawScope.drawPanicFace(cx: Float, cy: Float, r: Float, blink: Boolean) {
    // Wide scared eyes
    if (!blink) {
        drawOval(
            color = Color.White,
            topLeft = Offset(cx - r * 0.48f, cy - r * 0.28f),
            size = Size(r * 0.34f, r * 0.38f),
        )
        drawOval(
            color = Color.White,
            topLeft = Offset(cx + r * 0.14f, cy - r * 0.28f),
            size = Size(r * 0.34f, r * 0.38f),
        )
        drawCircle(KolobokOutline, r * 0.10f, Offset(cx - r * 0.31f, cy - r * 0.08f))
        drawCircle(KolobokOutline, r * 0.10f, Offset(cx + r * 0.31f, cy - r * 0.08f))
    } else {
        drawEye(Offset(cx - r * 0.31f, cy - r * 0.08f), r, true)
        drawEye(Offset(cx + r * 0.31f, cy - r * 0.08f), r, true)
    }
    // Big scream mouth
    drawOval(
        color = KolobokOutline,
        topLeft = Offset(cx - r * 0.22f, cy + r * 0.12f),
        size = Size(r * 0.44f, r * 0.42f),
    )
    drawOval(
        color = Color(0xFF3A0810),
        topLeft = Offset(cx - r * 0.14f, cy + r * 0.20f),
        size = Size(r * 0.28f, r * 0.28f),
    )
    // Little hands up (ICQ floating gloves)
    translate(left = 0f, top = 0f) {
        drawCircle(KolobokYellowFire, r * 0.16f, Offset(cx - r * 0.78f, cy - r * 0.55f))
        drawCircle(KolobokOutline, r * 0.16f, Offset(cx - r * 0.78f, cy - r * 0.55f), style = Stroke(r * 0.05f))
        drawCircle(KolobokYellowFire, r * 0.16f, Offset(cx + r * 0.78f, cy - r * 0.55f))
        drawCircle(KolobokOutline, r * 0.16f, Offset(cx + r * 0.78f, cy - r * 0.55f), style = Stroke(r * 0.05f))
    }
}
