package com.diabad

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier.modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diabad.domain.repository.GlucoseRepository
import com.diabad.monitor.MonitoringStarter
import com.diabad.ui.theme.DiaBADTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var glucoseRepository: GlucoseRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DiaBADTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val latest by glucoseRepository.observeLatest()
                        .collectAsStateWithLifecycle(initialValue = null)

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

                    CoreStatusScreen(
                        mmolText = latest?.let { "%.1f".format(it.mmol) } ?: "—",
                        trend = latest?.trend?.glyph.orEmpty(),
                        connectedHint = if (latest != null) {
                            stringResource(R.string.home_status_has_data)
                        } else {
                            stringResource(R.string.home_status_waiting)
                        },
                        monitoringOn = MonitoringStarter.hasNotificationPermission(this),
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionIntroDialog(
    onAllow: () -> Unit,
    onLater: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text(stringResource(R.string.perm_notifications_title)) },
        text = { Text(stringResource(R.string.perm_notifications_body)) },
        confirmButton = {
            TextButton(onClick = onAllow) {
                Text(stringResource(R.string.perm_allow))
            }
        },
        dismissButton = {
            TextButton(onClick = onLater) {
                Text(stringResource(R.string.perm_later))
            }
        },
    )
}

@Composable
private fun BatteryOptimizationDialog(
    onOpenSettings: () -> Unit,
    onSkip: () -> Unit,
) {
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
            TextButton(onClick = onSkip) {
                Text(stringResource(R.string.perm_later))
            }
        },
    )
}

@Composable
private fun CoreStatusScreen(
    mmolText: String,
    trend: String,
    connectedHint: String,
    monitoringOn: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = if (trend.isNotEmpty()) "$mmolText $trend" else mmolText,
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = stringResource(R.string.unit_mmol),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = connectedHint,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 24.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (monitoringOn) {
                stringResource(R.string.home_monitoring_on)
            } else {
                stringResource(R.string.home_monitoring_off)
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
