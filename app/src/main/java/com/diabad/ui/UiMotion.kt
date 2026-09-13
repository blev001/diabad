package com.diabad.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState

@Composable
fun animationsEnabled(): Boolean {
    val state by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    return state.isAtLeast(Lifecycle.State.RESUMED)
}

@Composable
fun loopingFloat(
    enabled: Boolean,
    from: Float,
    to: Float,
    durationMs: Int,
    label: String,
    resting: Float,
    restart: Boolean = false,
): Float {
    if (!enabled) return resting
    val infinite = rememberInfiniteTransition(label = label)
    val value by infinite.animateFloat(
        initialValue = from,
        targetValue = to,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = LinearEasing),
            repeatMode = if (restart) RepeatMode.Restart else RepeatMode.Reverse,
        ),
        label = label,
    )
    return value
}
