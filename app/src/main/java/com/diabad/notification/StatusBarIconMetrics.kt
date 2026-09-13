package com.diabad.notification

/**
 * Vertical badge: the mmol value uses the full width, the trend sits in a
 * short band underneath. Side-by-side triangles overflowed the 24 dp slot
 * and made both the number and the arrow look tiny.
 */
data class StatusBarIconMetrics(
    val sizePx: Int,
    val valueCenterX: Float,
    val valueBaselineY: Float,
    val valueTextSize: Float,
    val arrowLeft: Float,
    val arrowTop: Float,
    val arrowWidth: Float,
    val arrowHeight: Float,
    val hasArrow: Boolean,
)

fun computeStatusBarIconMetrics(
    sizePx: Int,
    value: String,
    hasArrow: Boolean,
): StatusBarIconMetrics {
    val canvas = sizePx.coerceAtLeast(48)
    val pad = canvas * 0.06f
    val content = canvas - pad * 2f
    val midX = canvas / 2f

    if (!hasArrow) {
        val textSize = fitTextSize(value, content, content * 0.92f)
        return StatusBarIconMetrics(
            sizePx = canvas,
            valueCenterX = midX,
            valueBaselineY = canvas / 2f + textSize * 0.35f,
            valueTextSize = textSize,
            arrowLeft = 0f,
            arrowTop = 0f,
            arrowWidth = 0f,
            arrowHeight = 0f,
            hasArrow = false,
        )
    }

    val arrowH = content * 0.30f
    val gap = canvas * 0.04f
    val valueH = content - arrowH - gap
    val textSize = fitTextSize(value, content, valueH)
    val valueCenterY = pad + valueH / 2f
    val arrowTop = pad + valueH + gap

    return StatusBarIconMetrics(
        sizePx = canvas,
        valueCenterX = midX,
        valueBaselineY = valueCenterY + textSize * 0.35f,
        valueTextSize = textSize,
        arrowLeft = pad,
        arrowTop = arrowTop,
        arrowWidth = content,
        arrowHeight = arrowH,
        hasArrow = true,
    )
}

internal fun fitTextSize(value: String, maxWidth: Float, maxHeight: Float): Float {
    val chars = value.length.coerceAtLeast(1)
    val byWidth = maxWidth / (chars * 0.48f)
    return minOf(byWidth, maxHeight)
}
