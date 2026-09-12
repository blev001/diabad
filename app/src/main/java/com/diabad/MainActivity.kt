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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diabad.alarm.DndAccessHelper
import com.diabad.alarm.HypoAlarmController
import com.diabad.domain.model.AlarmSoundId
import com.diabad.domain.model.AppSettings
import com.diabad.domain.repository.GlucoseRepository
import com.diabad.domain.repository.SettingsRepository
import com.diabad.monitor.MonitoringStarter
import com.diabad.ui.HomeScreen
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
                        onThresholdChange = {
                            scope.launch { settingsRepository.setHypoThresholdMmol(it) }
                        },
                        onSoundSelected = {
                            scope.launch { settingsRepository.setAlarmSoundId(it) }
                        },
                        onPickCustomSound = {
                            customSoundLauncher.launch(arrayOf("audio/*"))
                        },
                        onSnoozeMinutes = {
                            scope.launch { settingsRepository.setSnoozeMinutes(it) }
                        },
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
