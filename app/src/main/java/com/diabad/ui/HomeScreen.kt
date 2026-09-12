package com.diabad.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.diabad.R
import com.diabad.alarm.HypoAlarmUiState
import com.diabad.domain.model.AlarmSoundId
import com.diabad.domain.model.AppSettings
import com.diabad.domain.model.ConnectionLossMode
import com.diabad.ui.theme.ShBlue
import com.diabad.ui.theme.ShCard
import com.diabad.ui.theme.ShCardElevated
import com.diabad.ui.theme.ShCardGlass
import com.diabad.ui.theme.ShDanger
import com.diabad.ui.theme.ShDangerContainer
import com.diabad.ui.theme.ShGlow
import com.diabad.ui.theme.ShGreen
import com.diabad.ui.theme.ShGreenSoft
import com.diabad.ui.theme.ShOrange
import com.diabad.ui.theme.ShPurple
import com.diabad.ui.theme.ShTextPrimary
import com.diabad.ui.theme.ShTextSecondary
import com.diabad.ui.theme.ShTextTertiary

private val CardShape = RoundedCornerShape(28.dp)
private val PillShape = RoundedCornerShape(100.dp)

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
    ottaiListenerGranted: Boolean,
    onThresholdChange: (Double) -> Unit,
    onSoundSelected: (AlarmSoundId) -> Unit,
    onPickCustomSound: () -> Unit,
    onSnoozeMinutes: (Int) -> Unit,
    onConnectionLossMode: (ConnectionLossMode) -> Unit,
    onTestSound: () -> Unit,
    onDismissAlarm: () -> Unit,
    onSnoozeAlarm: () -> Unit,
    onOpenDndSettings: () -> Unit,
    onOpenOttaiListenerSettings: () -> Unit,
) {
    val alarming = alarmState == HypoAlarmUiState.RINGING || alarmState == HypoAlarmUiState.SNOOZED

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Ambient glow like Samsung Health hero area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            if (alarming) ShDanger.copy(alpha = 0.28f) else ShGlow,
                            Color.Transparent,
                        ),
                        radius = 520f,
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Header()

            Spacer(Modifier.height(20.dp))

            GlucoseHeroCard(
                mmolText = mmolText,
                trend = trend,
                alarming = alarming,
                connectedHint = connectedHint,
                alarmState = alarmState,
                monitoringOn = monitoringOn,
            )

            Spacer(Modifier.height(12.dp))

            // Always-visible sound test — top of screen so we can verify clicks/audio.
            Button(
                onClick = onTestSound,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = PillShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ShGreen,
                    contentColor = Color.Black,
                ),
            ) {
                Text(
                    text = "▶  ПРОВЕРИТЬ ЗВУК",
                    style = MaterialTheme.typography.titleLarge,
                )
            }

            AnimatedVisibility(
                visible = alarming,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                AlarmActionsCard(
                    snoozeMinutes = settings.snoozeMinutes,
                    ringing = alarmState == HypoAlarmUiState.RINGING,
                    onDismiss = onDismissAlarm,
                    onSnooze = onSnoozeAlarm,
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatusMiniCard(
                    title = stringResource(R.string.settings_ottai_access),
                    value = if (ottaiListenerGranted) "OK" else "—",
                    subtitle = if (ottaiListenerGranted) {
                        stringResource(R.string.home_status_chip_ok)
                    } else {
                        stringResource(R.string.home_status_chip_need)
                    },
                    accent = if (ottaiListenerGranted) ShGreen else ShOrange,
                    onClick = if (!ottaiListenerGranted) onOpenOttaiListenerSettings else null,
                    modifier = Modifier.weight(1f),
                )
                StatusMiniCard(
                    title = stringResource(R.string.settings_dnd),
                    value = if (dndGranted) "OK" else "—",
                    subtitle = if (dndGranted) {
                        stringResource(R.string.home_status_chip_ok)
                    } else {
                        stringResource(R.string.home_status_chip_need)
                    },
                    accent = if (dndGranted) ShBlue else ShOrange,
                    onClick = if (!dndGranted) onOpenDndSettings else null,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(12.dp))

            SettingsCard {
                SectionLabel(stringResource(R.string.settings_threshold))
                Text(
                    text = stringResource(
                        R.string.settings_threshold_value,
                        "%.1f".format(settings.hypoThresholdMmol),
                    ),
                    style = MaterialTheme.typography.headlineMedium,
                    color = ShTextPrimary,
                )
                Text(
                    text = stringResource(R.string.unit_mmol),
                    style = MaterialTheme.typography.bodySmall,
                    color = ShTextTertiary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                )
                Slider(
                    value = settings.hypoThresholdMmol.toFloat(),
                    onValueChange = { onThresholdChange(it.toDouble()) },
                    valueRange = 2.5f..5.5f,
                    steps = 29,
                    colors = SliderDefaults.colors(
                        thumbColor = ShGreen,
                        activeTrackColor = ShGreen,
                        inactiveTrackColor = ShCardElevated,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(12.dp))

            SettingsCard {
                SectionLabel(stringResource(R.string.settings_snooze))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    AppSettings.SNOOZE_OPTIONS_MINUTES.forEach { mins ->
                        SelectPill(
                            label = "$mins мин",
                            selected = settings.snoozeMinutes == mins,
                            onClick = { onSnoozeMinutes(mins) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            SettingsCard {
                SectionLabel(stringResource(R.string.settings_sound))
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
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
                }
                PillButton(
                    text = stringResource(R.string.settings_test_sound),
                    onClick = onTestSound,
                    container = ShCardElevated,
                    content = ShTextPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
            }

            Spacer(Modifier.height(12.dp))

            SettingsCard {
                SectionLabel(stringResource(R.string.settings_connection_loss))
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    ConnectionLossMode.entries.forEach { mode ->
                        SoundRow(
                            label = connectionLossLabel(mode),
                            selected = settings.connectionLossMode == mode,
                            onClick = { onConnectionLossMode(mode) },
                        )
                    }
                }
            }

            if (!ottaiListenerGranted) {
                Spacer(Modifier.height(12.dp))
                SettingsCard {
                    SectionLabel(stringResource(R.string.settings_ottai_access))
                    Text(
                        text = stringResource(R.string.settings_ottai_access_need),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ShTextSecondary,
                    )
                    PillButton(
                        text = stringResource(R.string.settings_ottai_access_open),
                        onClick = onOpenOttaiListenerSettings,
                        container = ShGreen,
                        content = Color.Black,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                    )
                }
            }

            if (!dndGranted) {
                Spacer(Modifier.height(12.dp))
                SettingsCard {
                    SectionLabel(stringResource(R.string.settings_dnd))
                    Text(
                        text = stringResource(R.string.settings_dnd_need),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ShTextSecondary,
                    )
                    PillButton(
                        text = stringResource(R.string.settings_dnd_open),
                        onClick = onOpenDndSettings,
                        container = ShCardElevated,
                        content = ShTextPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun Header() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            color = ShTextPrimary,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(ShBlue, ShPurple)),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "D",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun GlucoseHeroCard(
    mmolText: String,
    trend: String,
    alarming: Boolean,
    connectedHint: String,
    alarmState: HypoAlarmUiState,
    monitoringOn: Boolean,
) {
    val statusText = when (alarmState) {
        HypoAlarmUiState.RINGING -> stringResource(R.string.home_alarm_ringing)
        HypoAlarmUiState.SNOOZED -> stringResource(R.string.home_alarm_snoozed)
        HypoAlarmUiState.IDLE -> if (monitoringOn) {
            stringResource(R.string.home_status_good)
        } else {
            stringResource(R.string.home_monitoring_off)
        }
    }
    val statusColor = when {
        alarming -> ShDanger
        monitoringOn -> ShGreenSoft
        else -> ShOrange
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(
                if (alarming) {
                    Brush.verticalGradient(listOf(ShDangerContainer, ShCard))
                } else {
                    Brush.verticalGradient(listOf(ShCardGlass, ShCard))
                },
            )
            .padding(horizontal = 22.dp, vertical = 24.dp),
    ) {
        Text(
            text = stringResource(R.string.home_glucose_title),
            style = MaterialTheme.typography.titleMedium,
            color = ShTextSecondary,
        )

        Spacer(Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = mmolText,
                style = MaterialTheme.typography.displayLarge,
                color = if (alarming) ShDanger else ShTextPrimary,
            )
            if (trend.isNotEmpty()) {
                Text(
                    text = " $trend",
                    style = MaterialTheme.typography.displayMedium,
                    color = if (alarming) ShDanger else ShBlue,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleMedium,
                    color = statusColor,
                )
                Text(
                    text = stringResource(R.string.unit_mmol),
                    style = MaterialTheme.typography.bodySmall,
                    color = ShTextTertiary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = connectedHint,
            style = MaterialTheme.typography.bodyMedium,
            color = ShTextSecondary,
        )

        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlowDot(ShGreen)
            GlowDot(ShBlue)
            GlowDot(ShPurple)
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.home_tagline),
                style = MaterialTheme.typography.labelMedium,
                color = ShTextTertiary,
                modifier = Modifier
                    .clip(PillShape)
                    .background(ShCardElevated)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun GlowDot(color: Color) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color),
    )
}

@Composable
private fun StatusMiniCard(
    title: String,
    value: String,
    subtitle: String,
    accent: Color,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(CardShape)
            .background(ShCard)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
            )
            .padding(18.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = ShTextSecondary,
            maxLines = 2,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineLarge,
            color = ShTextPrimary,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.titleMedium,
            color = accent,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun AlarmActionsCard(
    snoozeMinutes: Int,
    ringing: Boolean,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(CardShape)
            .background(ShDangerContainer)
            .border(1.dp, ShDanger.copy(alpha = 0.35f), CardShape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = if (ringing) {
                stringResource(R.string.home_alarm_actions_hint)
            } else {
                stringResource(R.string.home_alarm_snoozed_hint)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = ShTextSecondary,
        )
        PillButton(
            text = stringResource(R.string.alarm_action_dismiss),
            onClick = onDismiss,
            container = ShDanger,
            content = Color.White,
            tall = true,
            modifier = Modifier.fillMaxWidth(),
        )
        PillButton(
            text = stringResource(R.string.alarm_action_snooze, snoozeMinutes),
            onClick = onSnooze,
            container = ShCardElevated,
            content = ShTextPrimary,
            tall = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(ShCard)
            .padding(20.dp),
        content = content,
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = ShTextPrimary,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun SelectPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) Color.Black else ShTextPrimary,
        modifier = Modifier
            .clip(PillShape)
            .background(if (selected) ShGreen else ShCardElevated)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
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
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .background(if (selected) Color(0xFF1A3D28) else Color.Transparent)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    color = if (selected) ShGreen else ShTextTertiary,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(ShGreen),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = ShTextPrimary,
        )
    }
}

@Composable
private fun PillButton(
    text: String,
    onClick: () -> Unit,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    tall: Boolean = false,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(if (tall) 56.dp else 48.dp),
        shape = PillShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
    ) {
        Text(
            text = text,
            style = if (tall) MaterialTheme.typography.titleLarge else MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
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
