package com.diabad.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier.modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.diabad.R
import com.diabad.alarm.HypoAlarmUiState
import com.diabad.domain.model.AlarmSoundId
import com.diabad.domain.model.AppSettings
import com.diabad.domain.model.ConnectionLossMode
import com.diabad.ui.theme.DiaDanger
import com.diabad.ui.theme.DiaMint
import com.diabad.ui.theme.DiaSoftBg

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    mmolText: String,
    trend: String,
    connectedHint: String,
    monitoringOn: Boolean,
    settings: AppSettings,
    alarmState: HypoAlarmUiState,
    dndGranted: Boolean,
    onThresholdChange: (Double) -> Unit,
    onSoundSelected: (AlarmSoundId) -> Unit,
    onPickCustomSound: () -> Unit,
    onSnoozeMinutes: (Int) -> Unit,
    onConnectionLossMode: (ConnectionLossMode) -> Unit,
    onTestSound: () -> Unit,
    onDismissAlarm: () -> Unit,
    onSnoozeAlarm: () -> Unit,
    onOpenDndSettings: () -> Unit,
) {
    val alarming = alarmState == HypoAlarmUiState.RINGING || alarmState == HypoAlarmUiState.SNOOZED

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(DiaMint.copy(alpha = 0.45f), DiaSoftBg, DiaSoftBg),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.home_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )

            Spacer(Modifier.height(20.dp))

            GlucoseHero(
                mmolText = mmolText,
                trend = trend,
                alarming = alarming,
            )

            Text(
                text = stringResource(R.string.unit_mmol),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )

            Text(
                text = connectedHint,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 14.dp),
            )

            StatusPill(
                text = when (alarmState) {
                    HypoAlarmUiState.RINGING -> stringResource(R.string.home_alarm_ringing)
                    HypoAlarmUiState.SNOOZED -> stringResource(R.string.home_alarm_snoozed)
                    HypoAlarmUiState.IDLE -> if (monitoringOn) {
                        stringResource(R.string.home_monitoring_on)
                    } else {
                        stringResource(R.string.home_monitoring_off)
                    }
                },
                danger = alarming,
                modifier = Modifier.padding(top = 12.dp),
            )

            AnimatedVisibility(
                visible = alarming,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                AlarmActions(
                    snoozeMinutes = settings.snoozeMinutes,
                    ringing = alarmState == HypoAlarmUiState.RINGING,
                    onDismiss = onDismissAlarm,
                    onSnooze = onSnoozeAlarm,
                )
            }

            Spacer(Modifier.height(28.dp))

            SettingsPanel {
                SectionTitle(stringResource(R.string.settings_threshold))
                Text(
                    text = stringResource(
                        R.string.settings_threshold_value,
                        "%.1f".format(settings.hypoThresholdMmol),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
                Slider(
                    value = settings.hypoThresholdMmol.toFloat(),
                    onValueChange = { onThresholdChange(it.toDouble()) },
                    valueRange = 2.5f..5.5f,
                    steps = 29,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                SectionTitle(stringResource(R.string.settings_snooze))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AppSettings.SNOOZE_OPTIONS_MINUTES.forEach { mins ->
                        FilterChip(
                            selected = settings.snoozeMinutes == mins,
                            onClick = { onSnoozeMinutes(mins) },
                            label = { Text("$mins мин") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                }

                SectionTitle(stringResource(R.string.settings_sound))
                AlarmSoundId.entries.forEach { id ->
                    SoundRow(
                        label = soundLabel(id),
                        selected = settings.alarmSoundId == id,
                        onClick = {
                            if (id == AlarmSoundId.CUSTOM) onPickCustomSound()
                            else onSoundSelected(id)
                        },
                    )
                }
                Button(
                    onClick = onTestSound,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(stringResource(R.string.settings_test_sound))
                }

                SectionTitle(stringResource(R.string.settings_connection_loss))
                ConnectionLossMode.entries.forEach { mode ->
                    SoundRow(
                        label = connectionLossLabel(mode),
                        selected = settings.connectionLossMode == mode,
                        onClick = { onConnectionLossMode(mode) },
                    )
                }

                SectionTitle(stringResource(R.string.settings_dnd))
                Text(
                    text = if (dndGranted) {
                        stringResource(R.string.settings_dnd_ok)
                    } else {
                        stringResource(R.string.settings_dnd_need)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!dndGranted) {
                    OutlinedButton(
                        onClick = onOpenDndSettings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(stringResource(R.string.settings_dnd_open))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun GlucoseHero(
    mmolText: String,
    trend: String,
    alarming: Boolean,
) {
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (alarming) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            )
            .padding(vertical = 28.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (trend.isNotEmpty()) "$mmolText $trend" else mmolText,
            style = MaterialTheme.typography.displayLarge,
            color = if (alarming) DiaDanger else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StatusPill(
    text: String,
    danger: Boolean,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = if (danger) DiaDanger else MaterialTheme.colorScheme.primary,
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(
                if (danger) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun AlarmActions(
    snoozeMinutes: Int,
    ringing: Boolean,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = if (ringing) {
                stringResource(R.string.home_alarm_actions_hint)
            } else {
                stringResource(R.string.home_alarm_snoozed_hint)
            },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DiaDanger,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
            contentPadding = PaddingValues(horizontal = 20.dp),
        ) {
            Text(
                text = stringResource(R.string.alarm_action_dismiss),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        Button(
            onClick = onSnooze,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(
                text = stringResource(R.string.alarm_action_snooze, snoozeMinutes),
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@Composable
private fun SettingsPanel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            .padding(20.dp),
        content = content,
    )
}

@Composable
private fun SectionTitle(text: String) {
    HorizontalDivider(
        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
    )
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
    )
}

@Composable
private fun SoundRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                else MaterialTheme.colorScheme.surface,
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (selected) "●  $label" else "○  $label",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun soundLabel(id: AlarmSoundId): String = when (id) {
    AlarmSoundId.SIREN -> stringResource(R.string.sound_siren)
    AlarmSoundId.MEDICAL -> stringResource(R.string.sound_medical)
    AlarmSoundId.CLOCK -> stringResource(R.string.sound_clock)
    AlarmSoundId.CARTOON_HORN -> stringResource(R.string.sound_cartoon_horn)
    AlarmSoundId.CARTOON_RING -> stringResource(R.string.sound_cartoon_ring)
    AlarmSoundId.CARTOON_CHIRP -> stringResource(R.string.sound_cartoon_chirp)
    AlarmSoundId.CUSTOM -> stringResource(R.string.sound_custom)
}

@Composable
private fun connectionLossLabel(mode: ConnectionLossMode): String = when (mode) {
    ConnectionLossMode.SILENT -> stringResource(R.string.connection_loss_silent)
    ConnectionLossMode.REMIND -> stringResource(R.string.connection_loss_remind)
    ConnectionLossMode.ALARM -> stringResource(R.string.connection_loss_alarm)
}
