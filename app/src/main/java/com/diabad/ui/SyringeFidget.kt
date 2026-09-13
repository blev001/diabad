package com.diabad.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.diabad.ui.theme.ShBlue
import com.diabad.ui.theme.ShGreenSoft
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Interactive syringe fidget: random haptic + animation on each tap (20 variants).
 * Does not show jokes — that's a separate «Шутка» control.
 */
@Composable
fun FidgetSyringeButton(
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }
    val rotation = remember { Animatable(0f) }
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }
    val plunger = remember { Animatable(0.4f) }

    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(colors.surfaceVariant)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                val variant = Random.nextInt(SyringeFidget.VARIANT_COUNT)
                SyringeFidget.vibrate(context, variant)
                scope.launch {
                    SyringeFidget.animate(
                        variant = variant,
                        scale = scale,
                        rotation = rotation,
                        offsetX = offsetX,
                        offsetY = offsetY,
                        alpha = alpha,
                        plunger = plunger,
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        InsulinSyringeIcon(
            modifier = Modifier
                .size(26.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    rotationZ = rotation.value
                    translationX = offsetX.value
                    translationY = offsetY.value
                    this.alpha = alpha.value
                },
            accent = ShBlue,
            body = colors.onSurface,
            liquid = ShGreenSoft,
            plungerProgress = plunger.value,
        )
    }
}

internal object SyringeFidget {
    const val VARIANT_COUNT = 20

    fun vibrate(context: Context, variant: Int) {
        val vibrator = vibrator(context) ?: return
        if (!vibrator.hasVibrator()) return
        val effect = effects[variant.coerceIn(0, VARIANT_COUNT - 1)]
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(effect)
            }
        } catch (_: Throwable) {
            // Ignore missing vibrator permission / OEM quirks.
        }
    }

    suspend fun animate(
        variant: Int,
        scale: Animatable<Float, AnimationVector1D>,
        rotation: Animatable<Float, AnimationVector1D>,
        offsetX: Animatable<Float, AnimationVector1D>,
        offsetY: Animatable<Float, AnimationVector1D>,
        alpha: Animatable<Float, AnimationVector1D>,
        plunger: Animatable<Float, AnimationVector1D>,
    ) {
        // Reset to a calm baseline first so variants feel distinct.
        scale.snapTo(1f)
        rotation.snapTo(0f)
        offsetX.snapTo(0f)
        offsetY.snapTo(0f)
        alpha.snapTo(1f)
        plunger.snapTo(0.4f)

        when (variant % VARIANT_COUNT) {
            0 -> { // soft press
                scale.animateTo(0.82f, spring(dampingRatio = 0.45f, stiffness = 600f))
                plunger.animateTo(0.85f, tween(120))
                scale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 380f))
                plunger.animateTo(0.4f, tween(180))
            }
            1 -> { // poke left
                offsetX.animateTo(-14f, tween(70))
                offsetX.animateTo(8f, tween(90))
                offsetX.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 400f))
            }
            2 -> { // poke right
                offsetX.animateTo(14f, tween(70))
                offsetX.animateTo(-8f, tween(90))
                offsetX.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 400f))
            }
            3 -> { // bounce up
                offsetY.animateTo(-18f, spring(dampingRatio = 0.4f, stiffness = 500f))
                offsetY.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 320f))
            }
            4 -> { // spin half
                rotation.animateTo(180f, tween(280, easing = FastOutSlowInEasing))
                rotation.snapTo(-180f)
                rotation.animateTo(0f, tween(220))
            }
            5 -> { // full twirl
                rotation.animateTo(360f, tween(420, easing = LinearEasing))
                rotation.snapTo(0f)
            }
            6 -> { // shake
                repeat(4) {
                    offsetX.animateTo(if (it % 2 == 0) 10f else -10f, tween(45))
                }
                offsetX.animateTo(0f, tween(60))
            }
            7 -> { // nod
                repeat(3) {
                    rotation.animateTo(16f, tween(70))
                    rotation.animateTo(-16f, tween(70))
                }
                rotation.animateTo(0f, tween(80))
            }
            8 -> { // pulse grow
                scale.animateTo(1.28f, spring(dampingRatio = 0.35f, stiffness = 350f))
                scale.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 280f))
            }
            9 -> { // shrink pop
                scale.animateTo(0.55f, tween(90))
                scale.animateTo(1.15f, spring(dampingRatio = 0.4f, stiffness = 450f))
                scale.animateTo(1f, tween(120))
            }
            10 -> { // inject plunge
                plunger.animateTo(0.95f, tween(160))
                offsetY.animateTo(10f, tween(160))
                plunger.animateTo(0.25f, tween(200))
                offsetY.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 360f))
                plunger.animateTo(0.4f, tween(140))
            }
            11 -> { // fade blink
                alpha.animateTo(0.25f, tween(90))
                alpha.animateTo(1f, tween(140))
                alpha.animateTo(0.4f, tween(80))
                alpha.animateTo(1f, tween(120))
            }
            12 -> { // diagonal hop
                offsetX.animateTo(12f, tween(80))
                offsetY.animateTo(-14f, tween(80))
                offsetX.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 380f))
                offsetY.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 380f))
            }
            13 -> { // wiggle roll
                rotation.animateTo(-28f, tween(80))
                rotation.animateTo(28f, tween(100))
                rotation.animateTo(-12f, tween(80))
                rotation.animateTo(0f, tween(90))
            }
            14 -> { // drop in
                offsetY.snapTo(-28f)
                alpha.snapTo(0.2f)
                offsetY.animateTo(0f, spring(dampingRatio = 0.42f, stiffness = 420f))
                alpha.animateTo(1f, tween(160))
            }
            15 -> { // jelly
                scale.animateTo(1.2f, tween(70))
                // squash
                scale.animateTo(0.85f, tween(90))
                scale.animateTo(1.08f, tween(80))
                scale.animateTo(1f, tween(90))
            }
            16 -> { // reverse spin
                rotation.animateTo(-360f, tween(380, easing = LinearEasing))
                rotation.snapTo(0f)
            }
            17 -> { // double tap bounce
                scale.animateTo(0.75f, tween(60))
                scale.animateTo(1.1f, tween(70))
                scale.animateTo(0.8f, tween(60))
                scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 400f))
            }
            18 -> { // float sway
                offsetY.animateTo(-10f, tween(120))
                offsetX.animateTo(8f, tween(100))
                offsetX.animateTo(-8f, tween(120))
                offsetX.animateTo(0f, tween(100))
                offsetY.animateTo(0f, tween(120))
            }
            else -> { // tip salute
                rotation.animateTo(-40f, tween(100))
                plunger.animateTo(0.7f, tween(100))
                rotation.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = 380f))
                plunger.animateTo(0.4f, tween(140))
            }
        }
    }

    private val effects: List<VibrationEffect> = listOf(
        oneShot(18, 80),
        oneShot(28, 140),
        oneShot(40, 220),
        waveform(longArrayOf(0, 20, 40, 20), intArrayOf(0, 120, 0, 80)),
        waveform(longArrayOf(0, 12, 30, 12, 30, 12), intArrayOf(0, 90, 0, 120, 0, 70)),
        waveform(longArrayOf(0, 35, 50, 35), intArrayOf(0, 180, 0, 100)),
        waveform(longArrayOf(0, 15, 15, 15, 15, 15), intArrayOf(0, 60, 0, 90, 0, 120)),
        waveform(longArrayOf(0, 60), intArrayOf(0, 255)),
        waveform(longArrayOf(0, 10, 20, 40), intArrayOf(0, 50, 0, 200)),
        waveform(longArrayOf(0, 8, 8, 8, 8, 8, 8, 8), intArrayOf(0, 70, 0, 90, 0, 110, 0, 140)),
        waveform(longArrayOf(0, 25, 10, 45), intArrayOf(0, 160, 0, 90)),
        waveform(longArrayOf(0, 50, 30, 20), intArrayOf(0, 40, 0, 200)),
        waveform(longArrayOf(0, 18, 18, 40), intArrayOf(0, 100, 0, 180)),
        waveform(longArrayOf(0, 12, 12, 12, 40), intArrayOf(0, 80, 0, 80, 200)),
        waveform(longArrayOf(0, 70, 20, 30), intArrayOf(0, 220, 0, 60)),
        waveform(longArrayOf(0, 5, 5, 5, 5, 30), intArrayOf(0, 60, 0, 80, 0, 160)),
        waveform(longArrayOf(0, 22, 22, 22), intArrayOf(0, 140, 0, 140)),
        waveform(longArrayOf(0, 40, 15, 15, 15), intArrayOf(0, 200, 0, 80, 80)),
        waveform(longArrayOf(0, 16, 40, 16, 40, 16), intArrayOf(0, 90, 0, 140, 0, 200)),
        waveform(longArrayOf(0, 30, 20, 30, 20, 50), intArrayOf(0, 100, 0, 150, 0, 220)),
    )

    private fun oneShot(ms: Long, amplitude: Int): VibrationEffect =
        VibrationEffect.createOneShot(ms, amplitude.coerceIn(1, 255))

    private fun waveform(timings: LongArray, amps: IntArray): VibrationEffect =
        VibrationEffect.createWaveform(timings, amps, -1)

    private fun vibrator(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
}
