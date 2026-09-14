package com.diabad.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diabad.R
import com.diabad.core.glucose.formatMmol
import com.diabad.domain.model.AppSettings
import com.diabad.domain.model.GlucoseAlarmKind
import com.diabad.domain.model.GlucoseZone
import com.diabad.domain.repository.GlucoseRepository
import com.diabad.domain.repository.SettingsRepository
import com.diabad.ui.KolobokMascot
import com.diabad.ui.theme.ShDanger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * Clock-style full-screen alarm on the phone: large reading, Stop / Snooze,
 * shows over the lock screen and turns the display on.
 */
@AndroidEntryPoint
class PhoneAlarmActivity : ComponentActivity() {

    @Inject lateinit var hypoAlarmController: HypoAlarmController
    @Inject lateinit var glucoseRepository: GlucoseRepository
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
        )
        getSystemService(KeyguardManager::class.java)?.requestDismissKeyguard(this, null)

        enableEdgeToEdge()
        setContent {
            val alarmState by hypoAlarmController.uiState.collectAsStateWithLifecycle()
            val alarmKind by hypoAlarmController.alarmKind.collectAsStateWithLifecycle()
            val isTest by hypoAlarmController.isTestAlarm.collectAsStateWithLifecycle()
            val ringing by hypoAlarmController.ringingReading.collectAsStateWithLifecycle()
            val latest by glucoseRepository.observeLatest()
                .collectAsStateWithLifecycle(initialValue = null)
            val settings by settingsRepository.observe()
                .collectAsStateWithLifecycle(initialValue = AppSettings())
            var armed by remember { mutableStateOf(false) }

            LaunchedEffect(alarmState) {
                if (alarmState == HypoAlarmUiState.RINGING) {
                    armed = true
                } else if (armed) {
                    finish()
                }
            }
            LaunchedEffect(Unit) {
                delay(1_000)
                if (!armed && hypoAlarmController.uiState.value != HypoAlarmUiState.RINGING) {
                    finish()
                }
            }

            if (alarmState != HypoAlarmUiState.RINGING && !armed) {
                return@setContent
            }

            val kind = alarmKind
                ?: intent.getStringExtra(EXTRA_KIND)?.toAlarmKind()
                ?: GlucoseAlarmKind.HYPO
            val mmol = when {
                isTest -> ringing?.mmol ?: intent.getDoubleExtra(EXTRA_MMOL, 0.0)
                else -> latest?.mmol
                    ?: ringing?.mmol
                    ?: intent.getDoubleExtra(EXTRA_MMOL, 0.0)
            }
            val threshold = when (kind) {
                GlucoseAlarmKind.HYPO -> settings.hypoThresholdMmol
                GlucoseAlarmKind.HYPER -> settings.hyperThresholdMmol
            }
            val snoozeMinutes = settings.snoozeMinutes
            val zone = GlucoseZone.classify(
                mmol,
                settings.hypoThresholdMmol,
                settings.hyperThresholdMmol,
            )

            BackHandler {
                // Clock alarms are dismissed only with Stop / Snooze.
            }

            PhoneAlarmScreen(
                mmolLabel = formatMmol(mmol),
                thresholdLabel = formatMmol(threshold),
                snoozeMinutes = snoozeMinutes,
                kind = kind,
                zone = zone,
                isTest = isTest,
                onDismiss = { hypoAlarmController.dismiss() },
                onSnooze = { hypoAlarmController.snooze() },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    companion object {
        const val EXTRA_MMOL = "mmol"
        const val EXTRA_THRESHOLD = "threshold"
        const val EXTRA_SNOOZE = "snooze"
        const val EXTRA_KIND = "kind"
        private const val ACTION_FULLSCREEN = "com.diabad.alarm.FULLSCREEN"

        fun createIntent(
            context: Context,
            mmol: Double = 0.0,
            threshold: Double = 3.9,
            snoozeMinutes: Int = AppSettings.DEFAULT_SNOOZE_MINUTES,
            kind: GlucoseAlarmKind = GlucoseAlarmKind.HYPO,
        ): Intent = Intent(context, PhoneAlarmActivity::class.java)
            .setAction(ACTION_FULLSCREEN)
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION,
            )
            .putExtra(EXTRA_MMOL, mmol)
            .putExtra(EXTRA_THRESHOLD, threshold)
            .putExtra(EXTRA_SNOOZE, snoozeMinutes)
            .putExtra(EXTRA_KIND, kind.name)
    }
}

private fun String.toAlarmKind(): GlucoseAlarmKind? =
    runCatching { GlucoseAlarmKind.valueOf(this) }.getOrNull()

private val AlarmBg = Color(0xFF140303)
private val AlarmMuted = Color(0xFFB8A4A4)
private val SnoozeBg = Color(0xFF37474F)

@Composable
private fun PhoneAlarmScreen(
    mmolLabel: String,
    thresholdLabel: String,
    snoozeMinutes: Int,
    kind: GlucoseAlarmKind,
    zone: GlucoseZone,
    isTest: Boolean,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
) {
    val title = when (kind) {
        GlucoseAlarmKind.HYPO -> stringResource(R.string.phone_alarm_title_hypo)
        GlucoseAlarmKind.HYPER -> stringResource(R.string.phone_alarm_title_hyper)
    }
    val thresholdText = when (kind) {
        GlucoseAlarmKind.HYPO -> stringResource(R.string.phone_alarm_threshold_hypo, thresholdLabel)
        GlucoseAlarmKind.HYPER -> stringResource(R.string.phone_alarm_threshold_hyper, thresholdLabel)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AlarmBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            color = ShDanger,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        if (isTest) {
            Text(
                text = stringResource(R.string.phone_alarm_test_badge),
                color = AlarmMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        KolobokMascot(
            zone = zone,
            alarming = true,
            modifier = Modifier.size(88.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.phone_alarm_mmol, mmolLabel),
            color = Color.White,
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 60.sp,
        )
        Text(
            text = thresholdText,
            color = AlarmMuted,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = stringResource(R.string.phone_alarm_hint),
            color = AlarmMuted,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp, bottom = 28.dp),
        )
        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(32.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ShDanger,
                contentColor = Color.White,
            ),
        ) {
            Text(
                text = stringResource(R.string.alarm_action_dismiss),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onSnooze,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(32.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SnoozeBg,
                contentColor = Color.White,
            ),
        ) {
            Text(
                text = stringResource(R.string.alarm_action_snooze, snoozeMinutes),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
