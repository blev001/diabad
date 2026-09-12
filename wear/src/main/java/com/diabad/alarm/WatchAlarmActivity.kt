package com.diabad.alarm

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier.modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * Full-screen watch alarm UI — closest analogue to the Galaxy Watch Clock alarm:
 * large reading, Stop / Snooze, screen stays on. Sound stays on the phone only.
 */
class WatchAlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
        )

        val mmol = intent.getDoubleExtra(EXTRA_MMOL, 0.0)
        val threshold = intent.getDoubleExtra(EXTRA_THRESHOLD, 3.9)
        val snoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE, 10)

        setContent {
            WatchAlarmScreen(
                mmolLabel = formatMmol(mmol),
                thresholdLabel = formatMmol(threshold),
                snoozeMinutes = snoozeMinutes,
                onDismiss = {
                    startService(
                        Intent(this, WatchAlarmService::class.java)
                            .setAction(WatchAlarmService.ACTION_DISMISS),
                    )
                    finish()
                },
                onSnooze = {
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
    }
}

@Composable
private fun WatchAlarmScreen(
    mmolLabel: String,
    thresholdLabel: String,
    snoozeMinutes: Int,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A0505))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Тревога",
            color = Color(0xFFFF6B6B),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier = Modifier.height(4.dp))
        Text(
            text = "$mmolLabel ммоль/л",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Порог $thresholdLabel",
            color = Color(0xFFB0B0B0),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier = Modifier.height(16.dp))
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE53935),
                contentColor = Color.White,
            ),
        ) {
            Text(text = "Стоп", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onSnooze,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF37474F),
                contentColor = Color.White,
            ),
        ) {
            Text(text = "Отложить $snoozeMinutes мин")
        }
    }
}

private fun formatMmol(value: Double): String =
    String.format(Locale.US, "%.1f", value)
