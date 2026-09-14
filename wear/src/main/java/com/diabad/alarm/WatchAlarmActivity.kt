package com.diabad.alarm

import android.app.KeyguardManager
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.diabad.R
import com.diabad.core.wear.WearAlarmPaths
import java.util.Locale

/**
 * Full-screen watch alarm — same Stop / Snooze layout as the phone Clock-style
 * screen, fitted to a round Galaxy Watch Ultra.
 */
class WatchAlarmActivity : ComponentActivity() {

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
        render(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == ACTION_FINISH) {
            finish()
            return
        }
        render(intent)
    }

    private fun render(intent: Intent) {
        if (intent.action == ACTION_FINISH) {
            finish()
            return
        }
        val mmol = intent.getDoubleExtra(EXTRA_MMOL, 0.0)
        val threshold = intent.getDoubleExtra(EXTRA_THRESHOLD, 3.9)
        val snoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE, 10)
        val kind = intent.getStringExtra(EXTRA_KIND) ?: WearAlarmPaths.KIND_HYPO
        val isHyper = kind == WearAlarmPaths.KIND_HYPER
        val isTest = intent.getBooleanExtra(EXTRA_TEST, false)

        setContent {
            var closing by remember { mutableStateOf(false) }
            BackHandler {
                // Clock alarms are dismissed only with Stop / Snooze.
            }
            WatchAlarmScreen(
                mmolLabel = formatMmol(mmol),
                thresholdLabel = formatMmol(threshold),
                snoozeMinutes = snoozeMinutes,
                isHyper = isHyper,
                isTest = isTest,
                onDismiss = {
                    if (closing) return@WatchAlarmScreen
                    closing = true
                    startService(
                        Intent(this, WatchAlarmService::class.java)
                            .setAction(WatchAlarmService.ACTION_DISMISS),
                    )
                    finish()
                },
                onSnooze = {
                    if (closing) return@WatchAlarmScreen
                    closing = true
                    startService(
                        Intent(this, WatchAlarmService::class.java)
                            .setAction(WatchAlarmService.ACTION_SNOOZE),
                    )
                    finish()
                },
            )
        }
    }

    companion object {
        const val EXTRA_MMOL = "mmol"
        const val EXTRA_THRESHOLD = "threshold"
        const val EXTRA_SNOOZE = "snooze"
        const val EXTRA_KIND = "kind"
        const val EXTRA_TEST = "test"
        const val ACTION_FINISH = "com.diabad.wear.FINISH_UI"
    }
}

private val AlarmBg = Color(0xFF140303)
private val AlarmMuted = Color(0xFFB8A4A4)
private val AlarmRed = Color(0xFFFF453A)
private val SnoozeBg = Color(0xFF37474F)

@Composable
private fun WatchAlarmScreen(
    mmolLabel: String,
    thresholdLabel: String,
    snoozeMinutes: Int,
    isHyper: Boolean,
    isTest: Boolean,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
) {
    val headline = if (isHyper) {
        stringResource(R.string.watch_alarm_title_hyper)
    } else {
        stringResource(R.string.watch_alarm_title)
    }
    val thresholdText = if (isHyper) {
        stringResource(R.string.watch_alarm_threshold_hyper, thresholdLabel)
    } else {
        stringResource(R.string.watch_alarm_threshold_hypo, thresholdLabel)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AlarmBg)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = headline,
            color = AlarmRed,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        if (isTest) {
            Text(
                text = stringResource(R.string.watch_alarm_test_badge),
                color = AlarmMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
            )
        }
        Text(
            text = stringResource(R.string.watch_alarm_mmol, mmolLabel),
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 28.sp,
            modifier = Modifier.padding(top = 2.dp),
        )
        Text(
            text = thresholdText,
            color = AlarmMuted,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AlarmRed,
                contentColor = Color.White,
            ),
        ) {
            Text(
                text = stringResource(R.string.watch_alarm_dismiss),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Button(
            onClick = onSnooze,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SnoozeBg,
                contentColor = Color.White,
            ),
        ) {
            Text(
                text = stringResource(R.string.watch_alarm_snooze, snoozeMinutes),
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
        }
    }
}

private fun formatMmol(value: Double): String =
    String.format(Locale.US, "%.1f", value)
