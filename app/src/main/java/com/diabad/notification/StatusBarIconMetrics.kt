package com.diabad.notification

/**
 * Layout for the status-bar badge: a huge mmol value and, when present, a
 * wide arrow lane. Kept free of Android types so unit tests can lock the
 * proportions that used to make "5.4 ↑" unreadable at 24 dp.
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
    val pad = canvas * 0.04f
    val content = canvas - pad * 2f
    val midY = canvas / 2f

    if (!hasArrow) {
        val textSize = fitTextSize(value, content * 0.98f, content * 0.90f)
        return StatusBarIconMetrics(
            sizePx = canvas,
            valueCenterX = canvas / 2f,
            valueBaselineY = midY + textSize * 0.35f,
            valueTextSize = textSize,
            arrowLeft = 0f,
            arrowTop = 0f,
            arrowWidth = 0f,
            arrowHeight = 0f,
            hasArrow = false,
        )
    }

    val gap = canvas * 0.02f
    val arrowW = content * 0.32f
    val valueW = content - arrowW - gap
    val textSize = fitTextSize(value, valueW, content * 0.92f)
    val valueCenterX = pad + valueW / 2f
    val arrowLeft = pad + valueW + gap
    val arrowH = content * 0.86f
    val arrowTop = (canvas - arrowH) / 2f

    return StatusBarIconMetrics(
        sizePx = canvas,
        valueCenterX = valueCenterX,
        valueBaselineY = midY + textSize * 0.35f,
        valueTextSize = textSize,
        arrowLeft = arrowLeft,
        arrowTop = arrowTop,
        arrowWidth = arrowW,
        arrowHeight = arrowH,
        hasArrow = true,
    )
}

internal fun fitTextSize(value: String, maxWidth: Float, maxHeight: Float): Float {
    val chars = value.length.coerceAtLeast(1)
    // Bold condensed digits are ~0.55–0.62 em wide; stay conservative so "12.4" fits.
    val byWidth = maxWidth / (chars * 0.50f)
    return minOf(byWidth, maxHeight)
}
