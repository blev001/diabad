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
import androidx.compose.material3.MaterialTheme
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
import com.diabad.alarm.AlarmPlayer
import com.diabad.alarm.DndAccessHelper
import com.diabad.alarm.HypoAlarmController
import com.diabad.core.glucose.formatDeltaMmol
import com.diabad.core.glucose.glucoseDeltaMmol
import com.diabad.domain.model.AlarmSoundId
import com.diabad.domain.model.AppSettings
import com.diabad.domain.model.TrendArrow
import com.diabad.domain.model.previousReading
import com.diabad.domain.repository.GlucoseRepository
import com.diabad.domain.repository.SettingsRepository
import com.diabad.monitor.MonitoringStarter
import com.diabad.ottai.OttaiNotificationListener
import com.diabad.ui.HomeScreen
import com.diabad.ui.theme.DiaBADTheme
import com.diabad.update.ApkInstaller
import com.diabad.update.AppUpdateChecker
import com.diabad.update.UpdateCheckResult
import dagger.hilt.android.AndroidEntryPoint
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var glucoseRepository: GlucoseRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var hypoAlarmController: HypoAlarmController
    @Inject lateinit var alarmPlayer: AlarmPlayer
    @Inject lateinit var dndAccessHelper: DndAccessHelper
    @Inject lateinit var appUpdateChecker: AppUpdateChecker
    @Inject lateinit var apkInstaller: ApkInstaller

    private fun runSoundTest(settings: AppSettings): String {
        Log.e("DiaBAD_SOUND", "TEST BUTTON PRESSED sound=${settings.alarmSoundId}")
        return try {
            val result = alarmPlayer.preview(settings)
            val msg = if (result.ok) {
                "Сработало: ${result.method}"
            } else {
                "Тишина. ${result.method}: ${result.detail}"
            }
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            msg
        } catch (t: Throwable) {
            Log.e("DiaBAD_SOUND", "preview crashed", t)
            val msg = "Крах: ${t.javaClass.simpleName}: ${t.message}"
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            msg
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by settingsRepository.observe()
                .collectAsStateWithLifecycle(initialValue = AppSettings())

            DiaBADTheme(themeMode = settings.themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val latest by glucoseRepository.observeLatest()
                        .collectAsStateWithLifecycle(initialValue = null)
                    val history by glucoseRepository.observeHistory()
                        .collectAsStateWithLifecycle(initialValue = emptyList())
                    val previous = previousReading(latest, history)
                    val deltaMmol = latest?.let { glucoseDeltaMmol(it.mmol, previous?.mmol) }
                    val deltaText = latest?.let { formatDeltaMmol(it.mmol, previous?.mmol) }
                    val alarmState by hypoAlarmController.uiState
                        .collectAsStateWithLifecycle()
                    val alarmKind by hypoAlarmController.alarmKind
                        .collectAsStateWithLifecycle()
                    val scope = rememberCoroutineScope()

                    var showPermissionIntro by remember {
                        mutableStateOf(!MonitoringStarter.hasNotificationPermission(this))
                    }
                    var showBatteryHint by remember { mutableStateOf(false) }
                    var soundTestStatus by remember {
                        mutableStateOf("Нажмите ▶ у мелодии")
                    }
                    var updateBusy by remember { mutableStateOf(false) }
                    var updateStatusText by remember {
                        mutableStateOf(
                            getString(
                                R.string.settings_updates_current,
                                BuildConfig.VERSION_NAME,
                                BuildConfig.VERSION_CODE,
                            ),
                        )
                    }
                    var pendingUpdate by remember {
                        mutableStateOf<UpdateCheckResult.Available?>(null)
                    }
                    var showInstallDialog by remember { mutableStateOf(false) }

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

                    fun checkForUpdates(manual: Boolean) {
                        scope.launch {
                            updateBusy = true
                            if (manual) {
                                updateStatusText = getString(R.string.settings_updates_checking)
                            }
                            when (val result = appUpdateChecker.check()) {
                                is UpdateCheckResult.UpToDate -> {
                                    updateStatusText = getString(R.string.settings_updates_uptodate)
                                    if (manual) {
                                        Toast.makeText(
                                            this@MainActivity,
                                            R.string.settings_updates_uptodate,
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                }
                                is UpdateCheckResult.Available -> {
                                    pendingUpdate = result
                                    updateStatusText = getString(
                                        R.string.settings_updates_available,
                                        result.versionName,
                                    )
                                    showInstallDialog = true
                                }
                                is UpdateCheckResult.Error -> {
                                    updateStatusText = getString(
                                        R.string.settings_updates_error,
                                        result.message,
                                    )
                                    if (manual) {
                                        Toast.makeText(
                                            this@MainActivity,
                                            updateStatusText,
                                            Toast.LENGTH_LONG,
                                        ).show()
                                    }
                                }
                            }
                            updateBusy = false
                        }
                    }

                    fun startDownloadAndInstall(update: UpdateCheckResult.Available) {
                        scope.launch {
                            updateBusy = true
                            updateStatusText = getString(R.string.settings_updates_downloading, 0)
                            try {
                                if (!apkInstaller.canInstallPackages()) {
                                    updateStatusText =
                                        getString(R.string.settings_updates_need_permission)
                                    startActivity(apkInstaller.unknownSourcesSettingsIntent())
                                    return@launch
                                }
                                val file = withContext(Dispatchers.IO) {
                                    appUpdateChecker.downloadApk(update.apkUrl) { progress ->
                                        updateStatusText = getString(
                                            R.string.settings_updates_downloading,
                                            (progress * 100).toInt(),
                                        )
                                    }
                                }
                                updateStatusText = getString(
                                    R.string.settings_updates_available,
                                    update.versionName,
                                )
                                apkInstaller.install(file)
                            } catch (t: Throwable) {
                                Log.w("DiaBAD_UPDATE", "download/install failed", t)
                                updateStatusText = getString(
                                    R.string.settings_updates_error,
                                    t.message ?: t.javaClass.simpleName,
                                )
                            } finally {
                                updateBusy = false
                            }
                        }
                    }

                    LaunchedEffect(Unit) {
                        if (MonitoringStarter.hasNotificationPermission(this@MainActivity)) {
                            MonitoringStarter.startIfPossible(this@MainActivity)
                            if (!MonitoringStarter.isIgnoringBatteryOptimizations(this@MainActivity)) {
                                showBatteryHint = true
                            }
                        }
                        checkForUpdates(manual = false)
                    }

                    if (showInstallDialog) {
                        val update = pendingUpdate
                        if (update != null) {
                            AlertDialog(
                                onDismissRequest = { showInstallDialog = false },
                                title = { Text(stringResource(R.string.settings_updates_install_title)) },
                                text = {
                                    Text(
                                        stringResource(
                                            R.string.settings_updates_install_body,
                                            update.versionName,
                                        ),
                                    )
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            showInstallDialog = false
                                            startDownloadAndInstall(update)
                                        },
                                    ) {
                                        Text(stringResource(R.string.settings_updates_install))
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showInstallDialog = false }) {
                                        Text(stringResource(R.string.settings_updates_later))
                                    }
                                },
                            )
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
                        mmol = latest?.mmol,
                        trend = latest?.trend ?: TrendArrow.NONE,
                        deltaText = deltaText,
                        deltaMmol = deltaMmol,
                        connectedHint = if (latest != null) {
                            stringResource(R.string.home_status_has_data)
                        } else {
                            stringResource(R.string.home_status_waiting)
                        },
                        monitoringOn = MonitoringStarter.hasNotificationPermission(this),
                        settings = settings,
                        alarmState = alarmState,
                        alarmKind = alarmKind,
                        dndGranted = dndAccessHelper.hasAccess(),
                        onHypoThresholdChange = {
                            scope.launch { settingsRepository.setHypoThresholdMmol(it) }
                        },
                        onHyperThresholdChange = {
                            scope.launch { settingsRepository.setHyperThresholdMmol(it) }
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
                        onThemeMode = {
                            scope.launch { settingsRepository.setThemeMode(it) }
                        },
                        onPreviewSound = { id ->
                            soundTestStatus = "Запуск…"
                            soundTestStatus = runSoundTest(
                                settings.copy(alarmSoundId = id),
                            )
                        },
                        soundTestStatus = soundTestStatus,
                        onDismissAlarm = { hypoAlarmController.dismiss() },
                        onSnoozeAlarm = { hypoAlarmController.snooze() },
                        onOpenDndSettings = { startActivity(dndAccessHelper.settingsIntent()) },
                        ottaiListenerGranted = OttaiNotificationListener.isEnabled(this),
                        onOpenOttaiListenerSettings = {
                            startActivity(OttaiNotificationListener.settingsIntent())
                        },
                        onCheckUpdates = { checkForUpdates(manual = true) },
                        updateStatusText = updateStatusText,
                        updateBusy = updateBusy,
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
