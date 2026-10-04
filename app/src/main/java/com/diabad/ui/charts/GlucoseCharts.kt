package com.diabad.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diabad.domain.analysis.AgpBucket
import com.diabad.domain.model.GlucoseReading
import com.diabad.ui.theme.ShBlue
import com.diabad.ui.theme.ShDanger
import com.diabad.ui.theme.ShGreen
import com.diabad.ui.theme.ShOrange
import java.time.Instant
import java.time.ZoneId
import kotlin.math.ceil
import kotlin.math.max

private const val Y_MIN = 2.0
private const val Y_MAX_DEFAULT = 15.0
private const val Y_MAX_LIMIT = 25.0
private const val GAP_MS = 15 * 60_000L
private val LEFT_AXIS = 28.dp
private val BOTTOM_AXIS = 18.dp

private class ChartFrame(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val yMax: Double,
) {
    val width get() = right - left
    val height get() = bottom - top
    fun y(mmol: Double): Float {
        val t = ((mmol.coerceIn(Y_MIN, yMax) - Y_MIN) / (yMax - Y_MIN)).toFloat()
        return bottom - t * height
    }
}

private fun DrawScope.frame(yMax: Double) = ChartFrame(
    left = LEFT_AXIS.toPx(),
    top = 6.dp.toPx(),
    right = size.width - 4.dp.toPx(),
    bottom = size.height - BOTTOM_AXIS.toPx(),
    yMax = yMax,
)

private fun yMaxFor(maxValue: Double?, hyper: Double): Double =
    ceil(max(max(Y_MAX_DEFAULT, hyper + 2), (maxValue ?: 0.0) + 1)).coerceAtMost(Y_MAX_LIMIT)

private fun DrawScope.drawRangeAndGrid(
    f: ChartFrame,
    hypo: Double,
    hyper: Double,
    grid: Color,
    label: Color,
    measurer: TextMeasurer,
) {
    drawRect(
        color = ShGreen.copy(alpha = 0.12f),
        topLeft = Offset(f.left, f.y(hyper)),
        size = Size(f.width, f.y(hypo) - f.y(hyper)),
    )
    val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
    drawLine(ShDanger.copy(alpha = 0.7f), Offset(f.left, f.y(hypo)), Offset(f.right, f.y(hypo)), 2f, pathEffect = dash)
    drawLine(ShOrange.copy(alpha = 0.7f), Offset(f.left, f.y(hyper)), Offset(f.right, f.y(hyper)), 2f, pathEffect = dash)

    val style = TextStyle(color = label, fontSize = 10.sp)
    listOf(3.0, 6.0, 10.0, 15.0, 20.0).filter { it < f.yMax }.forEach { v ->
        val y = f.y(v)
        drawLine(grid, Offset(f.left, y), Offset(f.right, y), 1f)
        val text = measurer.measure(v.toInt().toString(), style)
        drawText(text, topLeft = Offset(f.left - text.size.width - 6f, y - text.size.height / 2f))
    }
}

private fun zoneColor(mmol: Double, hypo: Double, hyper: Double): Color = when {
    mmol < hypo -> ShDanger
    mmol > hyper -> ShOrange
    else -> ShGreen
}

/** Line chart for the last [hours] ending at [nowMillis]; segments are colored by zone. */
@Composable
fun GlucoseLineChart(
    readings: List<GlucoseReading>,
    hypo: Double,
    hyper: Double,
    hours: Int,
    nowMillis: Long,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp,
) {
    val measurer = rememberTextMeasurer()
    val grid = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    val label = MaterialTheme.colorScheme.onSurfaceVariant
    val start = nowMillis - hours * 3_600_000L
    val visible = readings.filter { it.timestampMillis in start..nowMillis }
    val yMax = yMaxFor(visible.maxOfOrNull { it.mmol }, hyper)
    val zone = ZoneId.systemDefault()

    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        val f = frame(yMax)
        drawRangeAndGrid(f, hypo, hyper, grid, label, measurer)

        fun x(ts: Long) = f.left + ((ts - start).toFloat() / (nowMillis - start)) * f.width

        val stepHours = when {
            hours <= 3 -> 1
            hours <= 6 -> 2
            hours <= 12 -> 3
            else -> 6
        }
        val style = TextStyle(color = label, fontSize = 10.sp)
        val firstHour = Instant.ofEpochMilli(start).atZone(zone).withMinute(0).withSecond(0).withNano(0).plusHours(1)
        var tick = firstHour
        while (tick.toInstant().toEpochMilli() <= nowMillis) {
            if (tick.hour % stepHours == 0) {
                val tx = x(tick.toInstant().toEpochMilli())
                drawLine(grid, Offset(tx, f.top), Offset(tx, f.bottom), 1f)
                val text = measurer.measure("%02d:00".format(tick.hour), style)
                drawText(text, topLeft = Offset(tx - text.size.width / 2f, f.bottom + 4f))
            }
            tick = tick.plusHours(1)
        }

        val stroke = 3.dp.toPx()
        visible.zipWithNext { a, b ->
            if (b.timestampMillis - a.timestampMillis <= GAP_MS) {
                drawLine(
                    color = zoneColor(b.mmol, hypo, hyper),
                    start = Offset(x(a.timestampMillis), f.y(a.mmol)),
                    end = Offset(x(b.timestampMillis), f.y(b.mmol)),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }
        }
        visible.lastOrNull()?.let { last ->
            drawCircle(
                color = zoneColor(last.mmol, hypo, hyper),
                radius = 5.dp.toPx(),
                center = Offset(x(last.timestampMillis), f.y(last.mmol)),
            )
        }
    }
}

/** Ambulatory Glucose Profile: 5–95 % and 25–75 % bands with the median line. */
@Composable
fun AgpChart(
    buckets: List<AgpBucket>,
    hypo: Double,
    hyper: Double,
    modifier: Modifier = Modifier,
    height: Dp = 220.dp,
) {
    val measurer = rememberTextMeasurer()
    val grid = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    val label = MaterialTheme.colorScheme.onSurfaceVariant
    val yMax = yMaxFor(buckets.maxOfOrNull { it.p95 }, hyper)

    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        val f = frame(yMax)
        drawRangeAndGrid(f, hypo, hyper, grid, label, measurer)

        fun x(hour: Double) = f.left + (hour / 24.0).toFloat() * f.width

        val style = TextStyle(color = label, fontSize = 10.sp)
        for (h in 0..24 step 6) {
            val tx = x(h.toDouble())
            drawLine(grid, Offset(tx, f.top), Offset(tx, f.bottom), 1f)
            val text = measurer.measure("%02d".format(h % 24), style)
            drawText(text, topLeft = Offset(tx - text.size.width / 2f, f.bottom + 4f))
        }
        if (buckets.size < 2) return@Canvas

        fun band(lo: (AgpBucket) -> Double, hi: (AgpBucket) -> Double, color: Color) {
            val path = Path()
            buckets.forEachIndexed { i, b ->
                val px = x(b.hour + 0.5)
                if (i == 0) path.moveTo(px, f.y(hi(b))) else path.lineTo(px, f.y(hi(b)))
            }
            buckets.asReversed().forEach { b -> path.lineTo(x(b.hour + 0.5), f.y(lo(b))) }
            path.close()
            drawPath(path, color)
        }
        band({ it.p5 }, { it.p95 }, ShBlue.copy(alpha = 0.16f))
        band({ it.p25 }, { it.p75 }, ShBlue.copy(alpha = 0.34f))

        val median = Path()
        buckets.forEachIndexed { i, b ->
            val px = x(b.hour + 0.5)
            if (i == 0) median.moveTo(px, f.y(b.p50)) else median.lineTo(px, f.y(b.p50))
        }
        drawPath(median, ShBlue, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
    }
}
