package com.diabad.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.diabad.R
import com.diabad.domain.analysis.AgpBucket
import com.diabad.domain.analysis.GlucoseStats
import com.diabad.ui.charts.AgpChart
import com.diabad.ui.components.HintText
import com.diabad.ui.components.OptionPills
import com.diabad.ui.components.PillButton
import com.diabad.ui.components.SectionLabel
import com.diabad.ui.components.SettingsCard
import com.diabad.ui.theme.ShBlue
import com.diabad.ui.theme.ShDanger
import com.diabad.ui.theme.ShGreen
import com.diabad.ui.theme.ShOrange

data class StatsUiState(
    val periodDays: Int = 14,
    val loading: Boolean = true,
    val stats: GlucoseStats? = null,
    val agp: List<AgpBucket> = emptyList(),
)

val STATS_PERIOD_OPTIONS_DAYS = listOf(1, 7, 14, 30, 90)

private val VeryLowColor = Color(0xFFB71C1C)
private val VeryHighColor = Color(0xFFE65100)

@Composable
fun StatsScreen(
    state: StatsUiState,
    onPeriodSelected: (Int) -> Unit,
    onExportCsv: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PillButton(
            text = stringResource(R.string.stats_back),
            onClick = onClose,
            container = MaterialTheme.colorScheme.surfaceVariant,
            content = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.tab_stats),
            style = MaterialTheme.typography.headlineMedium,
            color = colors.onBackground,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        )
        OptionPills(
            options = STATS_PERIOD_OPTIONS_DAYS,
            selected = state.periodDays,
            label = { stringResource(R.string.stats_period_days, it) },
            onSelected = onPeriodSelected,
        )

        val stats = state.stats
        when {
            state.loading && stats == null -> SettingsCard { HintText(stringResource(R.string.stats_loading)) }
            stats == null -> SettingsCard { HintText(stringResource(R.string.stats_empty)) }
            else -> {
                TimeInRangeCard(stats)
                MetricsCard(stats)
                SettingsCard {
                    SectionLabel(stringResource(R.string.stats_agp_title))
                    HintText(stringResource(R.string.stats_agp_hint), modifier = Modifier.padding(bottom = 8.dp))
                    AgpChart(
                        buckets = state.agp,
                        hypo = GlucoseStats.RANGE_LOW_MMOL,
                        hyper = GlucoseStats.RANGE_HIGH_MMOL,
                    )
                }
            }
        }

        SettingsCard {
            SectionLabel(stringResource(R.string.stats_export_title))
            HintText(stringResource(R.string.stats_export_hint, state.periodDays))
            PillButton(
                text = stringResource(R.string.stats_export_button),
                onClick = onExportCsv,
                container = ShGreen,
                content = Color.Black,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            )
        }
        HintText(
            stringResource(R.string.stats_targets_note),
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun TimeInRangeCard(stats: GlucoseStats) {
    val segments = listOf(
        Triple(stats.veryHighPercent, VeryHighColor, R.string.stats_very_high),
        Triple(stats.highPercent, ShOrange, R.string.stats_high),
        Triple(stats.inRangePercent, ShGreen, R.string.stats_in_range),
        Triple(stats.lowPercent, ShDanger, R.string.stats_low),
        Triple(stats.veryLowPercent, VeryLowColor, R.string.stats_very_low),
    )
    SettingsCard {
        SectionLabel(stringResource(R.string.stats_tir_title))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            segments.forEach { (pct, color, _) ->
                if (pct > 0.0) {
                    Box(
                        modifier = Modifier
                            .weight(pct.toFloat())
                            .fillMaxSize()
                            .background(color),
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        segments.forEach { (pct, color, label) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 3.dp),
            ) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(color),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(label),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "%.0f %%".format(pct),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        HintText(stringResource(R.string.stats_tir_goals), modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun MetricsCard(stats: GlucoseStats) {
    SettingsCard {
        SectionLabel(stringResource(R.string.stats_metrics_title))
        val rows = listOf(
            Metric(stringResource(R.string.stats_mean), "%.1f".format(stats.meanMmol), stringResource(R.string.unit_mmol)),
            Metric(stringResource(R.string.stats_gmi), "%.1f %%".format(stats.gmiPercent), stringResource(R.string.stats_gmi_hint)),
            Metric(
                stringResource(R.string.stats_cv),
                "%.0f %%".format(stats.cvPercent),
                stringResource(if (stats.cvPercent <= 36.0) R.string.stats_cv_stable else R.string.stats_cv_unstable),
                accent = if (stats.cvPercent <= 36.0) ShGreen else ShOrange,
            ),
            Metric(stringResource(R.string.stats_sd), "%.1f".format(stats.sdMmol), stringResource(R.string.unit_mmol)),
            Metric(
                stringResource(R.string.stats_coverage),
                "%.0f %%".format(stats.coveragePercent),
                stringResource(R.string.stats_readings, stats.readingCount),
                accent = if (stats.coveragePercent >= 70.0) ShGreen else ShOrange,
            ),
        )
        rows.chunked(2).forEach { pair ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(vertical = 6.dp),
            ) {
                pair.forEach { MetricCell(it, Modifier.weight(1f)) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private data class Metric(val title: String, val value: String, val caption: String, val accent: Color = ShBlue)

@Composable
private fun MetricCell(metric: Metric, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp),
    ) {
        Text(metric.title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(metric.value, style = MaterialTheme.typography.headlineSmall, color = metric.accent)
        Text(metric.caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
