package com.diabad.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.diabad.domain.model.GlucoseZone
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val KolobokYellow = Color(0xFFFFD400)
private val KolobokYellowSick = Color(0xFFB8D44A)
private val KolobokYellowHot = Color(0xFFFFB020)
private val KolobokYellowFire = Color(0xFFFF6A3D)
private val KolobokOutline = Color(0xFF2A2208)
private val KolobokHighlight = Color(0xFFFFF6B0)
private val KolobokSweat = Color(0xFF5AC8FA)
private val KolobokHeart = Color(0xFFFF4D6A)
private val KolobokSpark = Color(0xFFFFF3A0)
private val KolobokNote = Color(0xFF7A5CFF)
private val KolobokCrown = Color(0xFFFFC107)
private val KolobokSteam = Color(0x66FFFFFF)

private enum class KolobokTrick {
    HOP,
    SPIN,
    WINK,
    LOOK_AROUND,
    NOD,
    SQUASH,
    FIGURE_EIGHT,
    TEETER,
    WAVE,
    BREATHE,
    PEEK,
    DIZZY_TWIRL,
    HEARTS,
    SLEEPY,
    SWEAT_BOUNCE,
    POP,
    SWAY_WIDE,
    DOUBLE_BLINK,
    TONGUE,
    JIGGLE,
    BACKFLIP,
    MOONWALK,
    HEADBANG,
    NO_NO,
    TAP_DANCE,
    ZOOM,
    FLOAT,
    SIDE_STEP,
    BLOW_KISS,
    BUBBLES,
    CONFETTI,
    STARS,
    MUSIC,
    SHADES,
    CROWN,
    QUESTION,
    EXCLAIM,
    YAWN,
    SNEEZE,
    GIGGLE,
    CROSS_EYE,
    HALO,
    STEAM,
    SNOW,
    VICTORY,
    SHRUG,
    TIPTOE,
    DROOP,
    SURPRISE,
    RAINBOW,
}

/**
 * Classic ICQ «kolobok»: yellow sphere, three hairs, glossy highlight.
 * Each instance cycles random idle tricks so several колобки on screen
 * do not move in lockstep.
 */
@Composable
fun KolobokMascot(
    zone: GlucoseZone,
    alarming: Boolean,
    modifier: Modifier = Modifier,
    varietyKey: Int = 0,
) {
    val infinite = rememberInfiniteTransition(label = "kolobok")
    var trick by remember(varietyKey, zone) {
        mutableStateOf(randomKolobokTrick(zone, varietyKey))
    }
    LaunchedEffect(zone, varietyKey) {
        while (true) {
            delay(Random.nextLong(1600L, 3800L))
            trick = randomKolobokTrick(zone, Random.nextInt())
        }
    }

    val bob by infinite.animateFloat(
        initialValue = -2.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "kolobokBob",
    )
    val hairSway by infinite.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "kolobokHair",
    )
    val blinkPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "kolobokBlink",
    )
    val winkPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "kolobokWink",
    )
    val hop by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(520, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "kolobokHop",
    )
    val spin by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "kolobokSpin",
    )
    val look by infinite.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "kolobokLook",
    )
    val squash by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "kolobokSquash",
    )
    val orbit by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "kolobokOrbit",
    )
    val shake by infinite.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                when {
                    alarming || zone == GlucoseZone.VERY_LOW -> 220
                    zone == GlucoseZone.LOW -> 360
                    else -> 800
                },
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "kolobokShake",
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                if (zone == GlucoseZone.VERY_HIGH || alarming) 420 else 720,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "kolobokPulse",
    )

    val blinkClosed = when (trick) {
        KolobokTrick.DOUBLE_BLINK, KolobokTrick.GIGGLE ->
            blinkPhase in 0.18f..0.24f || blinkPhase in 0.30f..0.36f
        KolobokTrick.SLEEPY, KolobokTrick.YAWN -> blinkPhase > 0.45f
        KolobokTrick.SNEEZE -> winkPhase in 0.40f..0.62f
        KolobokTrick.SURPRISE -> false
        else -> blinkPhase in 0.86f..0.92f
    }
    val winkLeft = trick == KolobokTrick.WINK && winkPhase in 0.35f..0.62f
    val winkRight = (trick == KolobokTrick.PEEK || trick == KolobokTrick.BLOW_KISS) &&
        winkPhase in 0.40f..0.70f
    val pupilShift = when (trick) {
        KolobokTrick.LOOK_AROUND, KolobokTrick.PEEK, KolobokTrick.MOONWALK -> look * 0.055f
        KolobokTrick.CROSS_EYE -> -0.04f
        else -> 0f
    }

    val shakeActive = alarming ||
        zone == GlucoseZone.VERY_LOW ||
        zone == GlucoseZone.LOW ||
        trick == KolobokTrick.TEETER ||
        trick == KolobokTrick.JIGGLE ||
        trick == KolobokTrick.SWEAT_BOUNCE ||
        trick == KolobokTrick.NO_NO ||
        trick == KolobokTrick.GIGGLE ||
        trick == KolobokTrick.SNEEZE ||
        trick == KolobokTrick.TAP_DANCE
    val pulseActive = zone == GlucoseZone.HIGH ||
        zone == GlucoseZone.VERY_HIGH ||
        trick == KolobokTrick.BREATHE ||
        trick == KolobokTrick.POP ||
        trick == KolobokTrick.ZOOM ||
        trick == KolobokTrick.SURPRISE ||
        (alarming && zone != GlucoseZone.LOW && zone != GlucoseZone.VERY_LOW)

    val hopLift = when (trick) {
        KolobokTrick.HOP, KolobokTrick.POP, KolobokTrick.BACKFLIP, KolobokTrick.VICTORY -> {
            val t = if (hop < 0.45f) hop / 0.45f else (1f - hop) / 0.55f
            -10f * (t * (1f - t) * 4f)
        }
        KolobokTrick.TAP_DANCE, KolobokTrick.TIPTOE -> -absHop(hop) * 6f
        KolobokTrick.FLOAT -> -4f - sin(orbit) * 3f
        KolobokTrick.DROOP -> 5f
        else -> 0f
    }
    val nodTilt = when (trick) {
        KolobokTrick.NOD, KolobokTrick.YAWN -> look * 10f
        KolobokTrick.HEADBANG -> look * 16f
        KolobokTrick.SHRUG -> look * 6f
        else -> 0f
    }
    val wideSway = when (trick) {
        KolobokTrick.SWAY_WIDE, KolobokTrick.WAVE, KolobokTrick.MOONWALK -> look * 7f
        KolobokTrick.NO_NO -> look * 12f
        KolobokTrick.SIDE_STEP -> look * 9f
        else -> 0f
    }
    val figureX = when (trick) {
        KolobokTrick.FIGURE_EIGHT -> sin(orbit) * 5f
        KolobokTrick.MOONWALK, KolobokTrick.SIDE_STEP -> look * 8f
        else -> 0f
    }
    val figureY = if (trick == KolobokTrick.FIGURE_EIGHT) sin(orbit * 2f) * 3.5f else 0f
    val peekX = if (trick == KolobokTrick.PEEK) look * 6f else 0f
    val extraSpin = when (trick) {
        KolobokTrick.SPIN -> spin * 0.18f
        KolobokTrick.DIZZY_TWIRL, KolobokTrick.STARS -> spin
        KolobokTrick.BACKFLIP -> spin
        else -> 0f
    }
    val squashX = when (trick) {
        KolobokTrick.SQUASH, KolobokTrick.GIGGLE -> 1f + squash * 0.12f
        KolobokTrick.POP, KolobokTrick.SNEEZE, KolobokTrick.SURPRISE ->
            1f + (if (hop < 0.2f) 0.12f else -0.04f)
        KolobokTrick.ZOOM -> 1f + squash * 0.18f
        else -> 1f
    }
    val squashY = when (trick) {
        KolobokTrick.SQUASH, KolobokTrick.GIGGLE -> 1f - squash * 0.10f
        KolobokTrick.POP, KolobokTrick.SNEEZE -> 1f - (if (hop < 0.2f) 0.08f else -0.05f)
        KolobokTrick.ZOOM -> 1f + squash * 0.18f
        KolobokTrick.DROOP -> 0.92f
        else -> 1f
    }

    Canvas(
        modifier = modifier
            .size(52.dp)
            .graphicsLayer {
                translationY = bob + hopLift + figureY
                translationX = (if (shakeActive) shake else 0f) + figureX + peekX + wideSway * 0.15f
                rotationZ = extraSpin + nodTilt + wideSway +
                    if (shakeActive) shake * 0.4f else hairSway * 0.05f
                val s = if (pulseActive) pulse else 1f
                scaleX = s * squashX
                scaleY = s * squashY
            },
    ) {
        drawKolobok(
            zone = zone,
            blinkClosed = blinkClosed,
            hairSwayDeg = hairSway + when (trick) {
                KolobokTrick.WAVE, KolobokTrick.HEADBANG, KolobokTrick.MUSIC -> look * 16f
                else -> 0f
            },
            pupilShiftX = pupilShift,
            winkLeft = winkLeft,
            winkRight = winkRight,
            trick = trick,
            overlayPhase = winkPhase,
        )
    }
}

private fun absHop(hop: Float): Float {
    val t = if (hop < 0.5f) hop / 0.5f else (1f - hop) / 0.5f
    return t * (1f - t) * 4f
}

private fun randomKolobokTrick(zone: GlucoseZone, salt: Int): KolobokTrick {
    val funPool = listOf(
        KolobokTrick.HOP, KolobokTrick.SPIN, KolobokTrick.WINK, KolobokTrick.LOOK_AROUND,
        KolobokTrick.NOD, KolobokTrick.SQUASH, KolobokTrick.FIGURE_EIGHT, KolobokTrick.WAVE,
        KolobokTrick.BREATHE, KolobokTrick.HEARTS, KolobokTrick.POP, KolobokTrick.SWAY_WIDE,
        KolobokTrick.DOUBLE_BLINK, KolobokTrick.TONGUE, KolobokTrick.JIGGLE, KolobokTrick.PEEK,
        KolobokTrick.BACKFLIP, KolobokTrick.MOONWALK, KolobokTrick.HEADBANG, KolobokTrick.NO_NO,
        KolobokTrick.TAP_DANCE, KolobokTrick.ZOOM, KolobokTrick.FLOAT, KolobokTrick.SIDE_STEP,
        KolobokTrick.BLOW_KISS, KolobokTrick.BUBBLES, KolobokTrick.CONFETTI, KolobokTrick.MUSIC,
        KolobokTrick.SHADES, KolobokTrick.CROWN, KolobokTrick.GIGGLE, KolobokTrick.HALO,
        KolobokTrick.VICTORY, KolobokTrick.SHRUG, KolobokTrick.TIPTOE, KolobokTrick.RAINBOW,
        KolobokTrick.SURPRISE, KolobokTrick.EXCLAIM, KolobokTrick.CROSS_EYE,
    )
    val pool = when (zone) {
        GlucoseZone.IN_RANGE -> funPool
        GlucoseZone.LOW -> listOf(
            KolobokTrick.TEETER, KolobokTrick.SWEAT_BOUNCE, KolobokTrick.LOOK_AROUND,
            KolobokTrick.NOD, KolobokTrick.SWAY_WIDE, KolobokTrick.DOUBLE_BLINK, KolobokTrick.JIGGLE,
            KolobokTrick.DROOP, KolobokTrick.SHRUG, KolobokTrick.SNOW, KolobokTrick.YAWN,
            KolobokTrick.NO_NO, KolobokTrick.QUESTION, KolobokTrick.SIDE_STEP, KolobokTrick.PEEK,
        )
        GlucoseZone.VERY_LOW -> listOf(
            KolobokTrick.DIZZY_TWIRL, KolobokTrick.SWEAT_BOUNCE, KolobokTrick.TEETER,
            KolobokTrick.SPIN, KolobokTrick.JIGGLE, KolobokTrick.STARS, KolobokTrick.SNOW,
            KolobokTrick.DROOP, KolobokTrick.SNEEZE, KolobokTrick.QUESTION,
        )
        GlucoseZone.HIGH -> listOf(
            KolobokTrick.SWEAT_BOUNCE, KolobokTrick.TEETER, KolobokTrick.BREATHE,
            KolobokTrick.POP, KolobokTrick.LOOK_AROUND, KolobokTrick.JIGGLE,
            KolobokTrick.STEAM, KolobokTrick.SURPRISE, KolobokTrick.HEADBANG,
            KolobokTrick.NO_NO, KolobokTrick.EXCLAIM, KolobokTrick.ZOOM,
        )
        GlucoseZone.VERY_HIGH -> listOf(
            KolobokTrick.POP, KolobokTrick.SPIN, KolobokTrick.HOP,
            KolobokTrick.DIZZY_TWIRL, KolobokTrick.SWEAT_BOUNCE, KolobokTrick.JIGGLE,
            KolobokTrick.STEAM, KolobokTrick.STARS, KolobokTrick.SURPRISE,
            KolobokTrick.EXCLAIM, KolobokTrick.BACKFLIP, KolobokTrick.SNEEZE,
        )
        GlucoseZone.UNKNOWN -> listOf(
            KolobokTrick.SLEEPY, KolobokTrick.NOD, KolobokTrick.BREATHE,
            KolobokTrick.LOOK_AROUND, KolobokTrick.SWAY_WIDE, KolobokTrick.PEEK,
            KolobokTrick.YAWN, KolobokTrick.QUESTION, KolobokTrick.SHRUG,
            KolobokTrick.FLOAT, KolobokTrick.TIPTOE, KolobokTrick.DROOP,
        )
    }
    val index = kotlin.math.abs(salt) % pool.size
    return pool[index]
}

private fun DrawScope.drawKolobok(
    zone: GlucoseZone,
    blinkClosed: Boolean,
    hairSwayDeg: Float,
    pupilShiftX: Float,
    winkLeft: Boolean,
    winkRight: Boolean,
    trick: KolobokTrick,
    overlayPhase: Float,
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

    drawCircle(
        color = Color.Black.copy(alpha = 0.18f),
        radius = r,
        center = Offset(cx + r * 0.06f, cy + r * 0.12f),
    )
    drawCircle(color = body, radius = r, center = Offset(cx, cy))
    drawCircle(
        color = KolobokOutline,
        radius = r,
        center = Offset(cx, cy),
        style = Stroke(width = r * 0.08f),
    )
    drawOval(
        color = KolobokHighlight.copy(alpha = 0.85f),
        topLeft = Offset(cx - r * 0.42f, cy - r * 0.48f),
        size = Size(r * 0.38f, r * 0.28f),
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.55f),
        radius = r * 0.08f,
        center = Offset(cx - r * 0.28f, cy - r * 0.32f),
    )

    rotate(degrees = hairSwayDeg * 0.35f, pivot = Offset(cx, cy - r)) {
        drawHair(cx - r * 0.18f, cy - r * 0.92f, -18f, r)
        drawHair(cx, cy - r * 1.02f, 0f, r)
        drawHair(cx + r * 0.18f, cy - r * 0.92f, 18f, r)
    }

    when (zone) {
        GlucoseZone.UNKNOWN -> drawNeutralFace(cx, cy, r, blinkClosed, pupilShiftX, winkLeft, winkRight)
        GlucoseZone.IN_RANGE -> drawHappyFace(cx, cy, r, blinkClosed, pupilShiftX, winkLeft, winkRight, trick)
        GlucoseZone.LOW -> drawSadFace(cx, cy, r, blinkClosed, pupilShiftX, winkLeft, winkRight)
        GlucoseZone.VERY_LOW -> drawDizzyFace(cx, cy, r, blinkClosed)
        GlucoseZone.HIGH -> drawWorriedFace(cx, cy, r, blinkClosed, pupilShiftX, winkLeft, winkRight)
        GlucoseZone.VERY_HIGH -> drawPanicFace(cx, cy, r, blinkClosed)
    }

    when (trick) {
        KolobokTrick.HEARTS, KolobokTrick.BLOW_KISS -> drawHearts(cx, cy, r, overlayPhase)
        KolobokTrick.SLEEPY, KolobokTrick.YAWN -> drawZzz(cx, cy, r, overlayPhase)
        KolobokTrick.SWEAT_BOUNCE, KolobokTrick.TEETER, KolobokTrick.SNEEZE ->
            drawExtraSweat(cx, cy, r, overlayPhase)
        KolobokTrick.SPIN, KolobokTrick.HOP, KolobokTrick.BACKFLIP, KolobokTrick.TAP_DANCE ->
            drawSparkles(cx, cy, r, overlayPhase)
        KolobokTrick.BUBBLES -> drawBubbles(cx, cy, r, overlayPhase)
        KolobokTrick.CONFETTI, KolobokTrick.RAINBOW -> drawConfetti(cx, cy, r, overlayPhase)
        KolobokTrick.STARS -> drawStars(cx, cy, r, overlayPhase)
        KolobokTrick.MUSIC -> drawNotes(cx, cy, r, overlayPhase)
        KolobokTrick.SHADES -> drawShades(cx, cy, r)
        KolobokTrick.CROWN, KolobokTrick.HALO -> drawCrown(cx, cy, r, halo = trick == KolobokTrick.HALO)
        KolobokTrick.QUESTION -> drawMark(cx, cy, r, "?")
        KolobokTrick.EXCLAIM, KolobokTrick.SURPRISE -> drawMark(cx, cy, r, "!")
        KolobokTrick.STEAM -> drawSteam(cx, cy, r, overlayPhase)
        KolobokTrick.SNOW -> drawSnow(cx, cy, r, overlayPhase)
        KolobokTrick.VICTORY -> drawVictoryHands(cx, cy, r)
        else -> Unit
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
    pupilShiftX: Float = 0f,
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
            color = Color.White,
            topLeft = Offset(center.x - r * 0.13f, center.y - r * 0.16f),
            size = Size(r * 0.26f, r * 0.32f),
        )
        drawCircle(
            color = KolobokOutline,
            radius = r * 0.09f * pupilScale,
            center = Offset(center.x + r * pupilShiftX, center.y),
        )
        drawCircle(
            color = Color.White,
            radius = r * 0.035f,
            center = Offset(center.x + r * pupilShiftX - r * 0.03f, center.y - r * 0.04f),
        )
    }
}

private fun DrawScope.drawHappyFace(
    cx: Float,
    cy: Float,
    r: Float,
    blink: Boolean,
    pupilShiftX: Float,
    winkLeft: Boolean,
    winkRight: Boolean,
    trick: KolobokTrick,
) {
    drawEye(Offset(cx - r * 0.28f, cy - r * 0.08f), r, blink || winkLeft, pupilShiftX = pupilShiftX)
    drawEye(Offset(cx + r * 0.28f, cy - r * 0.08f), r, blink || winkRight, pupilShiftX = pupilShiftX)
    val smile = Path().apply {
        moveTo(cx - r * 0.32f, cy + r * 0.22f)
        quadraticTo(cx, cy + r * 0.52f, cx + r * 0.32f, cy + r * 0.22f)
    }
    drawPath(smile, KolobokOutline, style = Stroke(width = r * 0.08f, cap = StrokeCap.Round))
    if (trick == KolobokTrick.TONGUE) {
        drawOval(
            color = Color(0xFFE23D4F),
            topLeft = Offset(cx - r * 0.08f, cy + r * 0.36f),
            size = Size(r * 0.16f, r * 0.16f),
        )
    }
}

private fun DrawScope.drawNeutralFace(
    cx: Float,
    cy: Float,
    r: Float,
    blink: Boolean,
    pupilShiftX: Float,
    winkLeft: Boolean,
    winkRight: Boolean,
) {
    drawEye(
        Offset(cx - r * 0.28f, cy - r * 0.06f),
        r,
        blink || winkLeft,
        pupilScale = 0.85f,
        pupilShiftX = pupilShiftX,
    )
    drawEye(
        Offset(cx + r * 0.28f, cy - r * 0.06f),
        r,
        blink || winkRight,
        pupilScale = 0.85f,
        pupilShiftX = pupilShiftX,
    )
    drawLine(
        color = KolobokOutline,
        start = Offset(cx - r * 0.22f, cy + r * 0.28f),
        end = Offset(cx + r * 0.22f, cy + r * 0.28f),
        strokeWidth = r * 0.07f,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawSadFace(
    cx: Float,
    cy: Float,
    r: Float,
    blink: Boolean,
    pupilShiftX: Float,
    winkLeft: Boolean,
    winkRight: Boolean,
) {
    drawEye(
        Offset(cx - r * 0.28f, cy - r * 0.02f),
        r,
        blink || winkLeft,
        pupilScale = 0.9f,
        pupilShiftX = pupilShiftX,
    )
    drawEye(
        Offset(cx + r * 0.28f, cy - r * 0.02f),
        r,
        blink || winkRight,
        pupilScale = 0.9f,
        pupilShiftX = pupilShiftX,
    )
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
    drawOval(
        color = KolobokOutline,
        topLeft = Offset(cx - r * 0.14f, cy + r * 0.22f),
        size = Size(r * 0.28f, r * 0.22f),
    )
    drawOval(
        color = KolobokSweat,
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

private fun DrawScope.drawWorriedFace(
    cx: Float,
    cy: Float,
    r: Float,
    blink: Boolean,
    pupilShiftX: Float,
    winkLeft: Boolean,
    winkRight: Boolean,
) {
    drawEye(
        Offset(cx - r * 0.28f, cy - r * 0.10f),
        r,
        blink || winkLeft,
        pupilScale = 1.15f,
        pupilShiftX = pupilShiftX,
    )
    drawEye(
        Offset(cx + r * 0.28f, cy - r * 0.10f),
        r,
        blink || winkRight,
        pupilScale = 1.15f,
        pupilShiftX = pupilShiftX,
    )
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
    drawCircle(KolobokYellowFire, r * 0.16f, Offset(cx - r * 0.78f, cy - r * 0.55f))
    drawCircle(KolobokOutline, r * 0.16f, Offset(cx - r * 0.78f, cy - r * 0.55f), style = Stroke(r * 0.05f))
    drawCircle(KolobokYellowFire, r * 0.16f, Offset(cx + r * 0.78f, cy - r * 0.55f))
    drawCircle(KolobokOutline, r * 0.16f, Offset(cx + r * 0.78f, cy - r * 0.55f), style = Stroke(r * 0.05f))
}

private fun DrawScope.drawHearts(cx: Float, cy: Float, r: Float, phase: Float) {
    val lift = sin(phase * 2f * PI.toFloat()) * r * 0.08f
    drawCircle(KolobokHeart, r * 0.09f, Offset(cx - r * 0.72f, cy - r * 0.55f - lift))
    drawCircle(KolobokHeart, r * 0.09f, Offset(cx - r * 0.60f, cy - r * 0.55f - lift))
    val heart = Path().apply {
        moveTo(cx - r * 0.66f, cy - r * 0.48f - lift)
        lineTo(cx - r * 0.54f, cy - r * 0.36f - lift)
        lineTo(cx - r * 0.78f, cy - r * 0.36f - lift)
        close()
    }
    drawPath(heart, KolobokHeart)
}

private fun DrawScope.drawZzz(cx: Float, cy: Float, r: Float, phase: Float) {
    val lift = phase * r * 0.35f
    drawCircle(Color.White.copy(alpha = 0.9f), r * 0.07f, Offset(cx + r * 0.62f, cy - r * 0.55f - lift))
    drawCircle(Color.White.copy(alpha = 0.7f), r * 0.05f, Offset(cx + r * 0.78f, cy - r * 0.72f - lift * 0.6f))
}

private fun DrawScope.drawExtraSweat(cx: Float, cy: Float, r: Float, phase: Float) {
    val drop = sin(phase * 2f * PI.toFloat()) * r * 0.06f
    drawOval(
        color = KolobokSweat,
        topLeft = Offset(cx + r * 0.58f, cy - r * 0.20f + drop),
        size = Size(r * 0.12f, r * 0.18f),
    )
}

private fun DrawScope.drawSparkles(cx: Float, cy: Float, r: Float, phase: Float) {
    val a = phase * 2f * PI.toFloat()
    drawCircle(KolobokSpark, r * 0.06f, Offset(cx + cos(a) * r * 0.85f, cy - r * 0.70f + sin(a) * r * 0.08f))
    drawCircle(KolobokSpark, r * 0.045f, Offset(cx - cos(a) * r * 0.80f, cy - r * 0.62f))
}

private fun DrawScope.drawBubbles(cx: Float, cy: Float, r: Float, phase: Float) {
    val lift = phase * r * 0.55f
    drawCircle(KolobokSweat.copy(alpha = 0.55f), r * 0.10f, Offset(cx + r * 0.62f, cy - lift))
    drawCircle(KolobokSweat.copy(alpha = 0.40f), r * 0.07f, Offset(cx - r * 0.58f, cy - r * 0.15f - lift * 0.7f))
    drawCircle(Color.White.copy(alpha = 0.5f), r * 0.03f, Offset(cx + r * 0.58f, cy - lift - r * 0.03f))
}

private fun DrawScope.drawConfetti(cx: Float, cy: Float, r: Float, phase: Float) {
    val colors = listOf(KolobokHeart, KolobokCrown, ShLikeBlue(), KolobokNote, KolobokSpark)
    colors.forEachIndexed { i, color ->
        val a = phase * 2f * PI.toFloat() + i * 0.9f
        drawCircle(
            color,
            r * 0.05f,
            Offset(cx + cos(a) * r * 0.95f, cy - r * 0.15f + sin(a * 1.4f) * r * 0.55f),
        )
    }
}

private fun ShLikeBlue(): Color = Color(0xFF5AC8FA)

private fun DrawScope.drawStars(cx: Float, cy: Float, r: Float, phase: Float) {
    val a = phase * 2f * PI.toFloat()
    drawCircle(KolobokCrown, r * 0.07f, Offset(cx + cos(a) * r * 0.9f, cy - r * 0.65f))
    drawCircle(KolobokSpark, r * 0.05f, Offset(cx - cos(a) * r * 0.85f, cy - r * 0.40f))
    drawCircle(KolobokCrown, r * 0.04f, Offset(cx + sin(a) * r * 0.70f, cy + r * 0.10f))
}

private fun DrawScope.drawNotes(cx: Float, cy: Float, r: Float, phase: Float) {
    val lift = sin(phase * 2f * PI.toFloat()) * r * 0.10f
    drawCircle(KolobokNote, r * 0.08f, Offset(cx + r * 0.68f, cy - r * 0.15f - lift))
    drawLine(
        KolobokNote,
        Offset(cx + r * 0.76f, cy - r * 0.15f - lift),
        Offset(cx + r * 0.76f, cy - r * 0.48f - lift),
        strokeWidth = r * 0.05f,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawShades(cx: Float, cy: Float, r: Float) {
    drawOval(
        color = Color(0xFF111111),
        topLeft = Offset(cx - r * 0.48f, cy - r * 0.22f),
        size = Size(r * 0.38f, r * 0.22f),
    )
    drawOval(
        color = Color(0xFF111111),
        topLeft = Offset(cx + r * 0.10f, cy - r * 0.22f),
        size = Size(r * 0.38f, r * 0.22f),
    )
    drawLine(
        Color(0xFF111111),
        Offset(cx - r * 0.10f, cy - r * 0.12f),
        Offset(cx + r * 0.10f, cy - r * 0.12f),
        strokeWidth = r * 0.05f,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawCrown(cx: Float, cy: Float, r: Float, halo: Boolean) {
    if (halo) {
        drawCircle(
            color = KolobokCrown.copy(alpha = 0.55f),
            radius = r * 0.22f,
            center = Offset(cx, cy - r * 1.05f),
            style = Stroke(width = r * 0.07f),
        )
    } else {
        val path = Path().apply {
            moveTo(cx - r * 0.28f, cy - r * 0.95f)
            lineTo(cx - r * 0.18f, cy - r * 1.18f)
            lineTo(cx, cy - r * 0.98f)
            lineTo(cx + r * 0.18f, cy - r * 1.18f)
            lineTo(cx + r * 0.28f, cy - r * 0.95f)
            close()
        }
        drawPath(path, KolobokCrown)
    }
}

private fun DrawScope.drawMark(cx: Float, cy: Float, r: Float, mark: String) {
    // Tiny cartoon glyph as dots — keep drawable-only, no text paint.
    if (mark == "?") {
        drawCircle(KolobokNote, r * 0.07f, Offset(cx + r * 0.70f, cy - r * 0.70f))
        drawCircle(KolobokNote, r * 0.035f, Offset(cx + r * 0.70f, cy - r * 0.52f))
    } else {
        drawRoundRectLikeBang(cx + r * 0.70f, cy - r * 0.72f, r)
    }
}

private fun DrawScope.drawRoundRectLikeBang(x: Float, y: Float, r: Float) {
    drawLine(
        KolobokHeart,
        Offset(x, y),
        Offset(x, y + r * 0.22f),
        strokeWidth = r * 0.08f,
        cap = StrokeCap.Round,
    )
    drawCircle(KolobokHeart, r * 0.04f, Offset(x, y + r * 0.32f))
}

private fun DrawScope.drawSteam(cx: Float, cy: Float, r: Float, phase: Float) {
    val lift = phase * r * 0.28f
    drawCircle(KolobokSteam, r * 0.10f, Offset(cx - r * 0.20f, cy - r * 0.85f - lift))
    drawCircle(KolobokSteam, r * 0.08f, Offset(cx + r * 0.08f, cy - r * 0.95f - lift * 0.7f))
    drawCircle(KolobokSteam, r * 0.06f, Offset(cx + r * 0.28f, cy - r * 0.80f - lift * 0.5f))
}

private fun DrawScope.drawSnow(cx: Float, cy: Float, r: Float, phase: Float) {
    val fall = (phase % 1f) * r * 0.9f
    drawCircle(Color.White, r * 0.04f, Offset(cx - r * 0.55f, cy - r * 0.70f + fall))
    drawCircle(Color.White, r * 0.035f, Offset(cx + r * 0.50f, cy - r * 0.90f + fall * 0.7f))
    drawCircle(Color.White, r * 0.03f, Offset(cx + r * 0.10f, cy - r * 0.80f + fall * 1.1f))
}

private fun DrawScope.drawVictoryHands(cx: Float, cy: Float, r: Float) {
    drawCircle(KolobokYellow, r * 0.15f, Offset(cx - r * 0.78f, cy - r * 0.55f))
    drawCircle(KolobokOutline, r * 0.15f, Offset(cx - r * 0.78f, cy - r * 0.55f), style = Stroke(r * 0.05f))
    drawCircle(KolobokYellow, r * 0.15f, Offset(cx + r * 0.78f, cy - r * 0.55f))
    drawCircle(KolobokOutline, r * 0.15f, Offset(cx + r * 0.78f, cy - r * 0.55f), style = Stroke(r * 0.05f))
}
