package com.diabad

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier.modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diabad.alarm.DndAccessHelper
import com.diabad.alarm.HypoAlarmController
import com.diabad.alarm.HypoAlarmUiState
import com.diabad.domain.model.AlarmSoundId
import com.diabad.domain.model.AppSettings
import com.diabad.domain.model.ConnectionLossMode
import com.diabad.domain.repository.GlucoseRepository
import com.diabad.domain.repository.SettingsRepository
import com.diabad.monitor.MonitoringStarter
import com.diabad.ui.theme.DiaBADTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var glucoseRepository: GlucoseRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var hypoAlarmController: HypoAlarmController
    @Inject lateinit var dndAccessHelper: DndAccessHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DiaBADTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val latest by glucoseRepository.observeLatest()
                        .collectAsStateWithLifecycle(initialValue = null)
                    val settings by settingsRepository.observe()
                        .collectAsStateWithLifecycle(initialValue = AppSettings())
                    val alarmState by hypoAlarmController.uiState
                        .collectAsStateWithLifecycle()
                    val scope = rememberCoroutineScope()

                    var showPermissionIntro by remember {
                        mutableStateOf(!MonitoringStarter.hasNotificationPermission(this))
                    }
                    var showBatteryHint by remember { mutableStateOf(false) }

                    val permissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission(),
                    ) { granted ->
                        showPermissionIntro = false
                        if (granted) {
                            MonitoringStarter.startIfPossible(this)
                            if (!MonitoringStarter.isIgnoringBatteryOptimizations(this)) {
                                showBatteryHint = true
                            }
                        }
                    }

                    val customSoundLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.OpenDocument(),
                    ) { uri: Uri? ->
                        if (uri == null) return@rememberLauncherForActivityResult
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        )
                        scope.launch {
                            settingsRepository.setCustomAlarmUri(uri.toString())
                            settingsRepository.setAlarmSoundId(AlarmSoundId.CUSTOM)
                        }
                    }

                    LaunchedEffect(Unit) {
                        if (MonitoringStarter.hasNotificationPermission(this@MainActivity)) {
                            MonitoringStarter.startIfPossible(this@MainActivity)
                            if (!MonitoringStarter.isIgnoringBatteryOptimizations(this@MainActivity)) {
                                showBatteryHint = true
                            }
                        }
                    }

                    if (showPermissionIntro) {
                        PermissionIntroDialog(
                            onAllow = {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            onLater = { showPermissionIntro = false },
                        )
                    }

                    if (showBatteryHint) {
                        BatteryOptimizationDialog(
                            onOpenSettings = {
                                showBatteryHint = false
                                startActivity(MonitoringStarter.batteryOptimizationIntent(this))
                            },
                            onSkip = { showBatteryHint = false },
                        )
                    }

                    HomeScreen(
                        mmolText = latest?.let { "%.1f".format(it.mmol) } ?: "—",
                        trend = latest?.trend?.glyph.orEmpty(),
                        connectedHint = if (latest != null) {
                            stringResource(R.string.home_status_has_data)
                        } else {
                            stringResource(R.string.home_status_waiting)
                        },
                        monitoringOn = MonitoringStarter.hasNotificationPermission(this),
                        settings = settings,
                        alarmState = alarmState,
                        dndGranted = dndAccessHelper.hasAccess(),
                        onThresholdChange = { scope.launch { settingsRepository.setHypoThresholdMmol(it) } },
                        onSoundSelected = { scope.launch { settingsRepository.setAlarmSoundId(it) } },
                        onPickCustomSound = {
                            customSoundLauncher.launch(arrayOf("audio/*"))
                        },
                        onSnoozeMinutes = { scope.launch { settingsRepository.setSnoozeMinutes(it) } },
                        onConnectionLossMode = {
                            scope.launch { settingsRepository.setConnectionLossMode(it) }
                        },
                        onTestSound = { hypoAlarmController.testSound() },
                        onDismissAlarm = { hypoAlarmController.dismiss() },
                        onSnoozeAlarm = { hypoAlarmController.snooze() },
                        onOpenDndSettings = { startActivity(dndAccessHelper.settingsIntent()) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
        Text(
            text = if (trend.isNotEmpty()) "$mmolText $trend" else mmolText,
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(stringResource(R.string.unit_mmol), style = MaterialTheme.typography.titleMedium)
        Text(
            text = connectedHint,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = when (alarmState) {
                HypoAlarmUiState.RINGING -> stringResource(R.string.home_alarm_ringing)
                HypoAlarmUiState.SNOOZED -> stringResource(R.string.home_alarm_snoozed)
                HypoAlarmUiState.IDLE -> if (monitoringOn) {
                    stringResource(R.string.home_monitoring_on)
                } else {
                    stringResource(R.string.home_monitoring_off)
                }
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp),
        )

        if (alarmState == HypoAlarmUiState.RINGING || alarmState == HypoAlarmUiState.SNOOZED) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = onDismissAlarm, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.alarm_action_dismiss))
                }
                OutlinedButton(onClick = onSnoozeAlarm, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.alarm_action_snooze, settings.snoozeMinutes))
                }
            }
        }

        Spacer(Modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier = Modifier.height(16.dp))

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
            modifier = Modifier.fillMaxWidth(),
        )

        SectionTitle(stringResource(R.string.settings_snooze))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppSettings.SNOOZE_OPTIONS_MINUTES.forEach { mins ->
                FilterChip(
                    selected = settings.snoozeMinutes == mins,
                    onClick = { onSnoozeMinutes(mins) },
                    label = { Text("$mins мин") },
                )
            }
        }

        SectionTitle(stringResource(R.string.settings_sound))
        AlarmSoundId.entries.forEach { id ->
            val label = soundLabel(id)
            val selected = settings.alarmSoundId == id
            Text(
                text = if (selected) "● $label" else "○ $label",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (id == AlarmSoundId.CUSTOM) onPickCustomSound()
                        else onSoundSelected(id)
                    }
                    .padding(vertical = 6.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Button(onClick = onTestSound, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_test_sound))
        }

        SectionTitle(stringResource(R.string.settings_connection_loss))
        ConnectionLossMode.entries.forEach { mode ->
            val label = connectionLossLabel(mode)
            val selected = settings.connectionLossMode == mode
            Text(
                text = if (selected) "● $label" else "○ $label",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onConnectionLossMode(mode) }
                    .padding(vertical = 6.dp),
                style = MaterialTheme.typography.bodyLarge,
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
        )
        if (!dndGranted) {
            OutlinedButton(onClick = onOpenDndSettings, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_dnd_open))
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 8.dp),
    )
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

@Composable
private fun PermissionIntroDialog(onAllow: () -> Unit, onLater: () -> Unit) {
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text(stringResource(R.string.perm_notifications_title)) },
        text = { Text(stringResource(R.string.perm_notifications_body)) },
        confirmButton = {
            TextButton(onClick = onAllow) { Text(stringResource(R.string.perm_allow)) }
        },
        dismissButton = {
            TextButton(onClick = onLater) { Text(stringResource(R.string.perm_later)) }
        },
    )
}

@Composable
private fun BatteryOptimizationDialog(onOpenSettings: () -> Unit, onSkip: () -> Unit) {
    AlertDialog(
        onDismissRequest = onSkip,
        title = { Text(stringResource(R.string.perm_battery_title)) },
        text = { Text(stringResource(R.string.perm_battery_body)) },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text(stringResource(R.string.perm_battery_allow))
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip) { Text(stringResource(R.string.perm_later)) }
        },
    )
}
