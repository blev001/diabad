package com.diabad.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import com.diabad.R
import com.diabad.domain.model.GlucoseGuideBand
import com.diabad.domain.model.GlucoseZone
import com.diabad.domain.model.glucoseGuideBands
import com.diabad.ui.theme.ShDanger
import com.diabad.ui.theme.ShGreen
import com.diabad.ui.theme.ShOrange

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
                Text(
                    text = stringResource(R.string.guide_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                bands.forEach { band ->
                    GuideRow(
                        zone = band.zone,
                        range = guideRangeLabel(band),
                        hint = guideHint(band.zone),
                        accent = guideAccent(band.zone),
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
private fun GuideRow(
    zone: GlucoseZone,
    range: String,
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
        KolobokMascot(
            zone = zone,
            alarming = zone == GlucoseZone.VERY_LOW || zone == GlucoseZone.VERY_HIGH,
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
private fun zoneGuideTitle(zone: GlucoseZone): String = when (zone) {
    GlucoseZone.UNKNOWN -> stringResource(R.string.home_status_waiting_zone)
    GlucoseZone.VERY_LOW -> stringResource(R.string.home_status_very_low)
    GlucoseZone.LOW -> stringResource(R.string.home_status_low)
    GlucoseZone.IN_RANGE -> stringResource(R.string.home_status_good)
    GlucoseZone.HIGH -> stringResource(R.string.home_status_high)
    GlucoseZone.VERY_HIGH -> stringResource(R.string.home_status_very_high)
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

private fun guideAccent(zone: GlucoseZone): Color = when (zone) {
    GlucoseZone.VERY_LOW, GlucoseZone.VERY_HIGH -> ShDanger
    GlucoseZone.LOW -> ShDanger.copy(alpha = 0.75f)
    GlucoseZone.IN_RANGE -> ShGreen
    GlucoseZone.HIGH -> ShOrange
    GlucoseZone.UNKNOWN -> ShOrange
}

private fun formatMmol(value: Double): String = "%.1f".format(value)
