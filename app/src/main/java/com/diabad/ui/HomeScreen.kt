package com.diabad.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.diabad.R
import com.diabad.core.glucose.FLAT_DELTA_MMOL
import com.diabad.alarm.HypoAlarmUiState
import com.diabad.domain.model.AlarmSoundId
import com.diabad.domain.model.AppSettings
import com.diabad.domain.model.ConnectionLossMode
import com.diabad.domain.model.GlucoseAlarmKind
import com.diabad.domain.model.GlucoseZone
import com.diabad.domain.model.ThemeMode
import com.diabad.domain.model.TrendArrow
import com.diabad.jokes.Type1DiabetesJokes
import com.diabad.jokes.WarmWords
import com.diabad.ui.theme.ShBlue
import com.diabad.ui.theme.ShDanger
import com.diabad.ui.theme.ShGreen
import com.diabad.ui.theme.ShGreenSoft
import com.diabad.ui.theme.ShOrange
import com.diabad.ui.theme.ShPurple
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val CardShape = RoundedCornerShape(28.dp)
private val PillShape = RoundedCornerShape(100.dp)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    mmolText: String,
    mmol: Double?,
    trend: TrendArrow,
    deltaText: String?,
    deltaMmol: Double?,
    connectedHint: String,
    monitoringOn: Boolean,
    settings: AppSettings,
    alarmState: HypoAlarmUiState,
    alarmKind: GlucoseAlarmKind?,
    dndGranted: Boolean,
    ottaiListenerGranted: Boolean,
    onHypoThresholdChange: (Double) -> Unit,
    onHyperThresholdChange: (Double) -> Unit,
    onSoundSelected: (AlarmSoundId) -> Unit,
    onPickCustomSound: () -> Unit,
    onSnoozeMinutes: (Int) -> Unit,
    onConnectionLossMode: (ConnectionLossMode) -> Unit,
    onThemeMode: (ThemeMode) -> Unit,
    onPreviewSound: (AlarmSoundId) -> Unit,
    soundTestStatus: String,
    onDismissAlarm: () -> Unit,
    onSnoozeAlarm: () -> Unit,
    onOpenDndSettings: () -> Unit,
    onOpenOttaiListenerSettings: () -> Unit,
    onCheckUpdates: () -> Unit,
    updateStatusText: String,
    updateBusy: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    val alarming = alarmState == HypoAlarmUiState.RINGING || alarmState == HypoAlarmUiState.SNOOZED
    var jokeText by remember { mutableStateOf<String?>(null) }
    var warmText by remember { mutableStateOf<String?>(null) }
    var showGuide by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var jokeHideJob by remember { mutableStateOf<Job?>(null) }
    var warmHideJob by remember { mutableStateOf<Job?>(null) }

    fun showJoke() {
        warmHideJob?.cancel()
        warmText = null
        jokeText = Type1DiabetesJokes.random(excluding = jokeText)
        jokeHideJob?.cancel()
        jokeHideJob = scope.launch {
            delay(6500)
            jokeText = null
        }
    }

    fun showWarmWord() {
        jokeHideJob?.cancel()
        jokeText = null
        warmText = WarmWords.random(excluding = warmText)
        warmHideJob?.cancel()
        warmHideJob = scope.launch {
            delay(7000)
            warmText = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            if (alarming) ShDanger.copy(alpha = 0.22f)
                            else ShOrange.copy(alpha = 0.14f),
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
            Header(
                onSyringeClick = ::showJoke,
                onWarmWordsClick = ::showWarmWord,
            )

            AnimatedVisibility(
                visible = jokeText != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                JokeBubble(
                    text = jokeText.orEmpty(),
                    onDismiss = {
                        jokeHideJob?.cancel()
                        jokeText = null
                    },
                )
            }

            AnimatedVisibility(
                visible = warmText != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                WarmWordsBubble(
                    text = warmText.orEmpty(),
                    onDismiss = {
                        warmHideJob?.cancel()
                        warmText = null
                    },
                )
            }

            Spacer(Modifier.height(16.dp))

            GlucoseHeroCard(
                mmolText = mmolText,
                mmol = mmol,
                trend = trend,
                deltaText = deltaText,
                deltaMmol = deltaMmol,
                alarming = alarming,
                connectedHint = connectedHint,
                alarmState = alarmState,
                alarmKind = alarmKind,
                monitoringOn = monitoringOn,
                hypoThreshold = settings.hypoThresholdMmol,
                hyperThreshold = settings.hyperThresholdMmol,
                onOpenGuide = { showGuide = true },
            )

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

            PillButton(
                text = stringResource(R.string.guide_open),
                onClick = { showGuide = true },
                container = ShBlue.copy(alpha = 0.18f),
                content = colors.onSurface,
                modifier = Modifier.fillMaxWidth(),
            )

            if (showGuide) {
                GlucoseGuideDialog(
                    hypoThreshold = settings.hypoThresholdMmol,
                    hyperThreshold = settings.hyperThresholdMmol,
                    onDismiss = { showGuide = false },
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
                SectionLabel(stringResource(R.string.settings_sound))
                Text(
                    text = stringResource(R.string.settings_sound_preview_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
                )
                Text(
                    text = soundTestStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                SoundDropdown(
                    selected = settings.alarmSoundId,
                    onSoundSelected = onSoundSelected,
                    onPickCustomSound = onPickCustomSound,
                    onPreviewSound = onPreviewSound,
                )
            }

            Spacer(Modifier.height(12.dp))

            SettingsCard {
                SectionLabel(stringResource(R.string.settings_updates))
                Text(
                    text = stringResource(R.string.settings_updates_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                Text(
                    text = updateStatusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
                )
                PillButton(
                    text = stringResource(
                        if (updateBusy) R.string.settings_updates_checking
                        else R.string.settings_updates_check,
                    ),
                    onClick = onCheckUpdates,
                    container = ShGreen,
                    content = Color.Black,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }

            Spacer(Modifier.height(12.dp))

            SettingsCard {
                SectionLabel(stringResource(R.string.settings_theme))
                ThemeModeSelector(
                    selected = settings.themeMode,
                    onSelected = onThemeMode,
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
                    color = colors.onSurface,
                )
                Text(
                    text = stringResource(R.string.settings_threshold_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                )
                Slider(
                    value = settings.hypoThresholdMmol.toFloat(),
                    onValueChange = { onHypoThresholdChange(it.toDouble()) },
                    valueRange = 2.5f..5.5f,
                    steps = 29,
                    colors = SliderDefaults.colors(
                        thumbColor = ShGreen,
                        activeTrackColor = ShGreen,
                        inactiveTrackColor = colors.surfaceVariant,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(12.dp))

            SettingsCard {
                SectionLabel(stringResource(R.string.settings_hyper_threshold))
                Text(
                    text = stringResource(
                        R.string.settings_hyper_threshold_value,
                        "%.1f".format(settings.hyperThresholdMmol),
                    ),
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.onSurface,
                )
                Text(
                    text = stringResource(R.string.settings_hyper_threshold_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                )
                Slider(
                    value = settings.hyperThresholdMmol.toFloat(),
                    onValueChange = { onHyperThresholdChange(it.toDouble()) },
                    valueRange = 7.0f..16.0f,
                    steps = 89,
                    colors = SliderDefaults.colors(
                        thumbColor = ShOrange,
                        activeTrackColor = ShOrange,
                        inactiveTrackColor = colors.surfaceVariant,
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
                SectionLabel(stringResource(R.string.settings_watch))
                Text(
                    text = stringResource(R.string.settings_watch_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
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
                        color = colors.onSurfaceVariant,
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
                        color = colors.onSurfaceVariant,
                    )
                    PillButton(
                        text = stringResource(R.string.settings_dnd_open),
                        onClick = onOpenDndSettings,
                        container = colors.surfaceVariant,
                        content = colors.onSurface,
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
private fun Header(
    onSyringeClick: () -> Unit,
    onWarmWordsClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            color = colors.onBackground,
            modifier = Modifier.weight(1f),
        )
        WarmWordsButton(onClick = onWarmWordsClick)
        AntiStressSyringeButton(onClick = onSyringeClick)
    }
}

@Composable
private fun WarmWordsButton(onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(PillShape)
            .background(ShPurple.copy(alpha = 0.18f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.warm_words_button),
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurface,
        )
    }
}

@Composable
private fun AntiStressSyringeButton(onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val idle = rememberInfiniteTransition(label = "syringeIdle")
    val bob by idle.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bob",
    )
    val punch = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(colors.surfaceVariant)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                scope.launch {
                    punch.snapTo(0f)
                    punch.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 500f))
                    punch.animateTo(0f, tween(220))
                }
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        InsulinSyringeIcon(
            modifier = Modifier
                .size(28.dp)
                .rotate(bob + punch.value * -18f),
            accent = ShBlue,
            body = colors.onSurface,
            liquid = ShGreenSoft,
            plungerProgress = 0.35f + punch.value * 0.45f,
        )
    }
}

@Composable
private fun InsulinSyringeIcon(
    modifier: Modifier = Modifier,
    accent: Color,
    body: Color,
    liquid: Color,
    plungerProgress: Float,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f

        // Needle
        drawLine(
            color = body.copy(alpha = 0.85f),
            start = Offset(cx, h * 0.78f),
            end = Offset(cx, h * 0.98f),
            strokeWidth = w * 0.06f,
            cap = StrokeCap.Round,
        )
        // Barrel
        val barrelTop = h * 0.22f
        val barrelBottom = h * 0.78f
        val barrelW = w * 0.42f
        drawRoundRect(
            color = body.copy(alpha = 0.18f),
            topLeft = Offset(cx - barrelW / 2f, barrelTop),
            size = Size(barrelW, barrelBottom - barrelTop),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
        )
        drawRoundRect(
            color = body,
            topLeft = Offset(cx - barrelW / 2f, barrelTop),
            size = Size(barrelW, barrelBottom - barrelTop),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
            style = Stroke(width = w * 0.07f),
        )
        // Liquid
        val liquidTop = barrelTop + (barrelBottom - barrelTop) * (1f - plungerProgress.coerceIn(0.15f, 0.95f))
        drawRoundRect(
            color = liquid.copy(alpha = 0.85f),
            topLeft = Offset(cx - barrelW / 2f + w * 0.04f, liquidTop),
            size = Size(barrelW - w * 0.08f, barrelBottom - liquidTop - w * 0.02f),
            cornerRadius = CornerRadius(w * 0.05f, w * 0.05f),
        )
        // Plunger rod + head
        val plungerY = liquidTop - h * 0.02f
        drawLine(
            color = accent,
            start = Offset(cx, h * 0.02f),
            end = Offset(cx, plungerY),
            strokeWidth = w * 0.08f,
            cap = StrokeCap.Round,
        )
        drawRoundRect(
            color = accent,
            topLeft = Offset(cx - barrelW * 0.55f, h * 0.01f),
            size = Size(barrelW * 1.1f, h * 0.08f),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f),
        )
        // Drop
        val drop = Path().apply {
            moveTo(cx, h * 0.88f)
            quadraticBezierTo(cx + w * 0.08f, h * 0.93f, cx, h * 0.99f)
            quadraticBezierTo(cx - w * 0.08f, h * 0.93f, cx, h * 0.88f)
            close()
        }
        drawPath(drop, color = ShDanger.copy(alpha = 0.9f))
    }
}

@Composable
private fun JokeBubble(text: String, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(CardShape)
            .background(colors.surface)
            .border(1.dp, colors.outline.copy(alpha = 0.35f), CardShape)
            .clickable(onClick = onDismiss)
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.antistress_title),
            style = MaterialTheme.typography.labelLarge,
            color = ShGreen,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.antistress_hint, Type1DiabetesJokes.count),
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun WarmWordsBubble(text: String, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(CardShape)
            .background(colors.surface)
            .border(1.dp, ShPurple.copy(alpha = 0.35f), CardShape)
            .clickable(onClick = onDismiss)
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.warm_words_title),
            style = MaterialTheme.typography.labelLarge,
            color = ShPurple,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.warm_words_hint, WarmWords.count),
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun ThemeModeSelector(
    selected: ThemeMode,
    onSelected: (ThemeMode) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PillShape)
            .background(colors.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ThemeMode.entries.forEach { mode ->
            val isSelected = mode == selected
            Text(
                text = themeModeLabel(mode),
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) Color.Black else colors.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(PillShape)
                    .background(if (isSelected) ShGreen else Color.Transparent)
                    .clickable { onSelected(mode) }
                    .padding(vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun themeModeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.LIGHT -> stringResource(R.string.theme_light)
    ThemeMode.DARK -> stringResource(R.string.theme_dark)
    ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
}

@Composable
private fun GlucoseHeroCard(
    mmolText: String,
    mmol: Double?,
    trend: TrendArrow,
    deltaText: String?,
    deltaMmol: Double?,
    alarming: Boolean,
    connectedHint: String,
    alarmState: HypoAlarmUiState,
    alarmKind: GlucoseAlarmKind?,
    monitoringOn: Boolean,
    hypoThreshold: Double,
    hyperThreshold: Double,
    onOpenGuide: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val zone = GlucoseZone.classify(mmol, hypoThreshold, hyperThreshold)
    val statusText = when (alarmState) {
        HypoAlarmUiState.RINGING -> when (alarmKind) {
            GlucoseAlarmKind.HYPER -> stringResource(R.string.home_alarm_ringing_hyper)
            else -> stringResource(R.string.home_alarm_ringing_hypo)
        }
        HypoAlarmUiState.SNOOZED -> stringResource(R.string.home_alarm_snoozed)
        HypoAlarmUiState.IDLE -> when {
            !monitoringOn -> stringResource(R.string.home_monitoring_off)
            else -> zoneStatusLabel(zone)
        }
    }
    val statusColor = when {
        alarming -> ShDanger
        zone == GlucoseZone.HIGH || zone == GlucoseZone.VERY_HIGH -> ShOrange
        zone == GlucoseZone.LOW || zone == GlucoseZone.VERY_LOW -> ShDanger
        monitoringOn && zone == GlucoseZone.IN_RANGE -> ShGreen
        else -> ShOrange
    }
    val valueColor = when {
        alarming || zone == GlucoseZone.VERY_LOW || zone == GlucoseZone.LOW -> ShDanger
        zone == GlucoseZone.HIGH || zone == GlucoseZone.VERY_HIGH -> ShOrange
        else -> colors.onSurface
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(
                if (alarming) {
                    Brush.verticalGradient(listOf(colors.errorContainer, colors.surface))
                } else {
                    Brush.verticalGradient(
                        listOf(colors.surface.copy(alpha = 0.92f), colors.surface),
                    )
                },
            )
            .padding(horizontal = 22.dp, vertical = 24.dp),
    ) {
        Text(
            text = stringResource(R.string.home_glucose_title),
            style = MaterialTheme.typography.titleMedium,
            color = colors.onSurfaceVariant,
        )

        Spacer(Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = mmolText,
                style = MaterialTheme.typography.displayLarge,
                color = valueColor,
            )
            if (trend.glyph.isNotEmpty()) {
                AnimatedTrendArrow(
                    trend = trend,
                    alarming = alarming,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .clickable(onClick = onOpenGuide),
                )
            }
            if (deltaText != null) {
                val deltaColor = when {
                    alarming -> ShDanger
                    deltaMmol == null || kotlin.math.abs(deltaMmol) < FLAT_DELTA_MMOL -> colors.onSurfaceVariant
                    deltaMmol > 0 -> ShOrange
                    else -> ShBlue
                }
                Text(
                    text = deltaText,
                    style = MaterialTheme.typography.headlineMedium,
                    color = deltaColor,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
            KolobokMascot(
                zone = zone,
                alarming = alarming,
                modifier = Modifier.padding(start = 4.dp),
            )
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleMedium,
                    color = statusColor,
                    textAlign = TextAlign.End,
                )
                Text(
                    text = stringResource(R.string.unit_mmol),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = connectedHint,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )

        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenGuide),
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
                color = colors.onSurfaceVariant,
                modifier = Modifier
                    .clip(PillShape)
                    .background(colors.surfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun zoneStatusLabel(zone: GlucoseZone): String = when (zone) {
    GlucoseZone.UNKNOWN -> stringResource(R.string.home_status_waiting_zone)
    GlucoseZone.VERY_LOW -> stringResource(R.string.home_status_very_low)
    GlucoseZone.LOW -> stringResource(R.string.home_status_low)
    GlucoseZone.IN_RANGE -> stringResource(R.string.home_status_good)
    GlucoseZone.HIGH -> stringResource(R.string.home_status_high)
    GlucoseZone.VERY_HIGH -> stringResource(R.string.home_status_very_high)
}

@Composable
private fun AnimatedTrendArrow(
    trend: TrendArrow,
    alarming: Boolean,
    modifier: Modifier = Modifier,
) {
    val infinite = rememberInfiniteTransition(label = "trendArrow")
    val amp = when (trend) {
        TrendArrow.DOUBLE_UP, TrendArrow.DOUBLE_DOWN -> 8f
        TrendArrow.SINGLE_UP, TrendArrow.SINGLE_DOWN -> 5f
        TrendArrow.FORTY_FIVE_UP, TrendArrow.FORTY_FIVE_DOWN -> 4f
        TrendArrow.FLAT -> 3f
        else -> 0f
    }
    val period = when (trend) {
        TrendArrow.DOUBLE_UP, TrendArrow.DOUBLE_DOWN -> 450
        TrendArrow.SINGLE_UP, TrendArrow.SINGLE_DOWN -> 650
        else -> 1100
    }
    val drift by infinite.animateFloat(
        initialValue = -amp,
        targetValue = amp,
        animationSpec = infiniteRepeatable(
            animation = tween(period, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "trendDrift",
    )
    val color = if (alarming) ShDanger else ShBlue

    Text(
        text = trend.glyph,
        style = MaterialTheme.typography.displayMedium,
        color = color,
        modifier = modifier.graphicsLayer {
            when (trend) {
                TrendArrow.DOUBLE_UP, TrendArrow.SINGLE_UP, TrendArrow.FORTY_FIVE_UP -> {
                    translationY = -drift
                }
                TrendArrow.DOUBLE_DOWN, TrendArrow.SINGLE_DOWN, TrendArrow.FORTY_FIVE_DOWN -> {
                    translationY = drift
                }
                TrendArrow.FLAT -> {
                    translationX = drift
                }
                else -> Unit
            }
        },
    )
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
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(CardShape)
            .background(colors.surface)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(18.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant,
            maxLines = 2,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineLarge,
            color = colors.onSurface,
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
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(CardShape)
            .background(colors.errorContainer)
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
            color = colors.onErrorContainer,
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
            container = colors.surfaceVariant,
            content = colors.onSurface,
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
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp),
        content = content,
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun SelectPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) Color.Black else colors.onSurface,
        modifier = Modifier
            .clip(PillShape)
            .background(if (selected) ShGreen else colors.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

@Composable
private fun SoundDropdown(
    selected: AlarmSoundId,
    onSoundSelected: (AlarmSoundId) -> Unit,
    onPickCustomSound: () -> Unit,
    onPreviewSound: (AlarmSoundId) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(top = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.primaryContainer)
                .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { expanded = !expanded }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (expanded) "▴" else "▾",
                    style = MaterialTheme.typography.titleLarge,
                    color = ShGreen,
                    modifier = Modifier.padding(end = 10.dp),
                )
                Text(
                    text = soundLabel(selected),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurface,
                )
            }
            TextButton(
                onClick = { onPreviewSound(selected) },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_sound_preview),
                    style = MaterialTheme.typography.labelLarge,
                    color = ShGreen,
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                AlarmSoundId.entries.forEach { id ->
                    SoundRow(
                        label = soundLabel(id),
                        selected = selected == id,
                        onClick = {
                            if (id == AlarmSoundId.CUSTOM) onPickCustomSound()
                            else onSoundSelected(id)
                            expanded = false
                        },
                        onPreview = { onPreviewSound(id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SoundRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    onPreview: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) colors.primaryContainer else Color.Transparent)
            .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp, horizontal = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        color = if (selected) ShGreen else colors.onSurfaceVariant,
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
                color = colors.onSurface,
            )
        }
        if (onPreview != null) {
            TextButton(
                onClick = onPreview,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_sound_preview),
                    style = MaterialTheme.typography.labelLarge,
                    color = ShGreen,
                )
            }
        }
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
    AlarmSoundId.TWO_TONE -> stringResource(R.string.sound_two_tone)
    AlarmSoundId.KLAXON -> stringResource(R.string.sound_klaxon)
    AlarmSoundId.SOS_ROBOT -> stringResource(R.string.sound_sos_robot)
    AlarmSoundId.RISING_PANIC -> stringResource(R.string.sound_rising_panic)
    AlarmSoundId.COIN_RUSH -> stringResource(R.string.sound_coin_rush)
    AlarmSoundId.POWERUP -> stringResource(R.string.sound_powerup)
    AlarmSoundId.BOSS_ALERT -> stringResource(R.string.sound_boss_alert)
    AlarmSoundId.LASER_ZAP -> stringResource(R.string.sound_laser_zap)
    AlarmSoundId.FANFARE_8BIT -> stringResource(R.string.sound_fanfare_8bit)
    AlarmSoundId.SPACE_BLIPS -> stringResource(R.string.sound_space_blips)
    AlarmSoundId.LEVEL_UP -> stringResource(R.string.sound_level_up)
    AlarmSoundId.GAME_OVER -> stringResource(R.string.sound_game_over)
    AlarmSoundId.BUMP_BEEP -> stringResource(R.string.sound_bump_beep)
    AlarmSoundId.BOING -> stringResource(R.string.sound_boing)
    AlarmSoundId.DUCK -> stringResource(R.string.sound_duck)
    AlarmSoundId.MEOW -> stringResource(R.string.sound_meow)
    AlarmSoundId.GIGGLE -> stringResource(R.string.sound_giggle)
    AlarmSoundId.BUBBLES -> stringResource(R.string.sound_bubbles)
    AlarmSoundId.RETRO_PHONE -> stringResource(R.string.sound_retro_phone)
    AlarmSoundId.XYLOPHONE -> stringResource(R.string.sound_xylophone)
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
