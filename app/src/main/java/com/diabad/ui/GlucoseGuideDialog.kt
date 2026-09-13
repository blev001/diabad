package com.diabad.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.diabad.R
import com.diabad.domain.model.GlucoseGuideBand
import com.diabad.domain.model.GlucoseZone
import com.diabad.domain.model.TrendArrow
import com.diabad.domain.model.glucoseGuideBands
import com.diabad.ui.theme.ShBlue
import com.diabad.ui.theme.ShDanger
import com.diabad.ui.theme.ShGreen
import com.diabad.ui.theme.ShOrange
import com.diabad.ui.theme.ShPurple

private val GuideRowShape = RoundedCornerShape(18.dp)

@Composable
fun GlucoseGuideDialog(
    hypoThreshold: Double,
    hyperThreshold: Double,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val bands = glucoseGuideBands(hypoThreshold, hyperThreshold)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.guide_title))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GuideSectionTitle(stringResource(R.string.guide_section_zones))
                Text(
                    text = stringResource(R.string.guide_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                bands.forEachIndexed { index, band ->
                    GuideZoneRow(
                        zone = band.zone,
                        range = guideRangeLabel(band),
                        hint = guideHint(band.zone),
                        accent = guideAccent(band.zone),
                        varietyKey = index + 1,
                    )
                }
                Text(
                    text = stringResource(
                        R.string.guide_thresholds_note,
                        formatMmol(hypoThreshold),
                        formatMmol(hyperThreshold),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )

                Spacer(Modifier.height(6.dp))
                GuideSectionTitle(stringResource(R.string.guide_section_colors))
                Text(
                    text = stringResource(R.string.guide_colors_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                GuideColorRow(
                    accent = ShGreen,
                    title = stringResource(R.string.guide_color_green_title),
                    hint = stringResource(R.string.guide_color_green_hint),
                )
                GuideColorRow(
                    accent = ShBlue,
                    title = stringResource(R.string.guide_color_blue_title),
                    hint = stringResource(R.string.guide_color_blue_hint),
                )
                GuideColorRow(
                    accent = ShPurple,
                    title = stringResource(R.string.guide_color_purple_title),
                    hint = stringResource(R.string.guide_color_purple_hint),
                )
                GuideColorRow(
                    accent = ShOrange,
                    title = stringResource(R.string.guide_color_orange_title),
                    hint = stringResource(R.string.guide_color_orange_hint),
                )
                GuideColorRow(
                    accent = ShDanger,
                    title = stringResource(R.string.guide_color_red_title),
                    hint = stringResource(R.string.guide_color_red_hint),
                )

                Spacer(Modifier.height(6.dp))
                GuideSectionTitle(stringResource(R.string.guide_section_trend))
                Text(
                    text = stringResource(R.string.guide_trend_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                TrendArrow.GUIDE_ARROWS.forEach { arrow ->
                    GuideTrendRow(
                        arrow = arrow,
                        title = trendTitle(arrow),
                        rate = trendRate(arrow),
                        hint = trendHint(arrow),
                        accent = trendAccent(arrow),
                    )
                }
                Text(
                    text = stringResource(R.string.guide_trend_delta_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.guide_close))
            }
        },
    )
}

@Composable
private fun GuideSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun GuideZoneRow(
    zone: GlucoseZone,
    range: String,
    hint: String,
    accent: Color,
    varietyKey: Int,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GuideRowShape)
            .background(colors.surfaceVariant.copy(alpha = 0.55f))
            .border(1.dp, accent.copy(alpha = 0.35f), GuideRowShape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        KolobokMascot(
            zone = zone,
            alarming = zone == GlucoseZone.VERY_LOW || zone == GlucoseZone.VERY_HIGH,
            varietyKey = varietyKey,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = zoneGuideTitle(zone),
                style = MaterialTheme.typography.titleMedium,
                color = accent,
            )
            Text(
                text = range,
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GuideTrendRow(
    arrow: TrendArrow,
    title: String,
    rate: String,
    hint: String,
    accent: Color,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GuideRowShape)
            .background(colors.surfaceVariant.copy(alpha = 0.55f))
            .border(1.dp, accent.copy(alpha = 0.35f), GuideRowShape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = arrow.glyph,
            style = MaterialTheme.typography.headlineMedium,
            color = accent,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(36.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = accent,
            )
            Text(
                text = rate,
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GuideColorRow(
    accent: Color,
    title: String,
    hint: String,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GuideRowShape)
            .background(colors.surfaceVariant.copy(alpha = 0.55f))
            .border(1.dp, accent.copy(alpha = 0.35f), GuideRowShape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .width(18.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(accent),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = accent,
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun zoneGuideTitle(zone: GlucoseZone): String = when (zone) {
    GlucoseZone.UNKNOWN -> stringResource(R.string.home_status_waiting_zone)
    GlucoseZone.VERY_LOW -> stringResource(R.string.guide_zone_very_low)
    GlucoseZone.LOW -> stringResource(R.string.guide_zone_low)
    GlucoseZone.IN_RANGE -> stringResource(R.string.guide_zone_in_range)
    GlucoseZone.HIGH -> stringResource(R.string.guide_zone_high)
    GlucoseZone.VERY_HIGH -> stringResource(R.string.guide_zone_very_high)
}

@Composable
private fun guideHint(zone: GlucoseZone): String = when (zone) {
    GlucoseZone.VERY_LOW -> stringResource(R.string.guide_hint_very_low)
    GlucoseZone.LOW -> stringResource(R.string.guide_hint_low)
    GlucoseZone.IN_RANGE -> stringResource(R.string.guide_hint_in_range)
    GlucoseZone.HIGH -> stringResource(R.string.guide_hint_high)
    GlucoseZone.VERY_HIGH -> stringResource(R.string.guide_hint_very_high)
    GlucoseZone.UNKNOWN -> stringResource(R.string.home_status_waiting_zone)
}

@Composable
private fun guideRangeLabel(band: GlucoseGuideBand): String = when (band.kind) {
    GlucoseGuideBand.Kind.BELOW -> stringResource(
        R.string.guide_range_very_low,
        formatMmol(band.firstMmol),
    )
    GlucoseGuideBand.Kind.ABOVE -> stringResource(
        R.string.guide_range_very_high,
        formatMmol(band.firstMmol),
    )
    GlucoseGuideBand.Kind.BETWEEN -> {
        val rangeRes = when (band.zone) {
            GlucoseZone.LOW -> R.string.guide_range_low
            GlucoseZone.HIGH -> R.string.guide_range_high
            else -> R.string.guide_range_in_range
        }
        stringResource(
            rangeRes,
            formatMmol(band.firstMmol),
            formatMmol(band.secondMmol ?: band.firstMmol),
        )
    }
}

@Composable
private fun trendTitle(arrow: TrendArrow): String = when (arrow) {
    TrendArrow.DOUBLE_UP -> stringResource(R.string.guide_trend_double_up_title)
    TrendArrow.SINGLE_UP -> stringResource(R.string.guide_trend_single_up_title)
    TrendArrow.FORTY_FIVE_UP -> stringResource(R.string.guide_trend_forty_up_title)
    TrendArrow.FLAT -> stringResource(R.string.guide_trend_flat_title)
    TrendArrow.FORTY_FIVE_DOWN -> stringResource(R.string.guide_trend_forty_down_title)
    TrendArrow.SINGLE_DOWN -> stringResource(R.string.guide_trend_single_down_title)
    TrendArrow.DOUBLE_DOWN -> stringResource(R.string.guide_trend_double_down_title)
    else -> ""
}

@Composable
private fun trendRate(arrow: TrendArrow): String = when (arrow) {
    TrendArrow.DOUBLE_UP -> stringResource(R.string.guide_trend_double_up_rate)
    TrendArrow.SINGLE_UP -> stringResource(R.string.guide_trend_single_up_rate)
    TrendArrow.FORTY_FIVE_UP -> stringResource(R.string.guide_trend_forty_up_rate)
    TrendArrow.FLAT -> stringResource(R.string.guide_trend_flat_rate)
    TrendArrow.FORTY_FIVE_DOWN -> stringResource(R.string.guide_trend_forty_down_rate)
    TrendArrow.SINGLE_DOWN -> stringResource(R.string.guide_trend_single_down_rate)
    TrendArrow.DOUBLE_DOWN -> stringResource(R.string.guide_trend_double_down_rate)
    else -> ""
}

@Composable
private fun trendHint(arrow: TrendArrow): String = when (arrow) {
    TrendArrow.DOUBLE_UP -> stringResource(R.string.guide_trend_double_up_hint)
    TrendArrow.SINGLE_UP -> stringResource(R.string.guide_trend_single_up_hint)
    TrendArrow.FORTY_FIVE_UP -> stringResource(R.string.guide_trend_forty_up_hint)
    TrendArrow.FLAT -> stringResource(R.string.guide_trend_flat_hint)
    TrendArrow.FORTY_FIVE_DOWN -> stringResource(R.string.guide_trend_forty_down_hint)
    TrendArrow.SINGLE_DOWN -> stringResource(R.string.guide_trend_single_down_hint)
    TrendArrow.DOUBLE_DOWN -> stringResource(R.string.guide_trend_double_down_hint)
    else -> ""
}

private fun guideAccent(zone: GlucoseZone): Color = when (zone) {
    GlucoseZone.VERY_LOW, GlucoseZone.VERY_HIGH -> ShDanger
    GlucoseZone.LOW -> ShDanger.copy(alpha = 0.75f)
    GlucoseZone.IN_RANGE -> ShGreen
    GlucoseZone.HIGH -> ShOrange
    GlucoseZone.UNKNOWN -> ShOrange
}

private fun trendAccent(arrow: TrendArrow): Color = when (arrow) {
    TrendArrow.DOUBLE_UP, TrendArrow.SINGLE_UP -> ShOrange
    TrendArrow.FORTY_FIVE_UP -> ShOrange.copy(alpha = 0.85f)
    TrendArrow.FLAT -> ShGreen
    TrendArrow.FORTY_FIVE_DOWN -> ShBlue
    TrendArrow.SINGLE_DOWN, TrendArrow.DOUBLE_DOWN -> ShDanger
    else -> ShOrange
}

private fun formatMmol(value: Double): String = "%.1f".format(value)
