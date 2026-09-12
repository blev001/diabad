package com.diabad

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diabad.domain.repository.GlucoseRepository
import com.diabad.ui.theme.DiaBADTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Temporary shell screen for Core/Data iteration.
 * Real One UI home screen comes in the UI iteration.
 */
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
                    CoreStatusScreen(
                        mmolText = latest?.let { "%.1f".format(it.mmol) } ?: "—",
                        connectedHint = if (latest != null) {
                            "Есть данные от OtTai"
                        } else {
                            "Ожидание данных от OtTai…\nВключите Share with AAPS в OtTai Hub"
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CoreStatusScreen(
    mmolText: String,
    connectedHint: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "DiaBAD",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = mmolText,
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = "ммоль/л",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = connectedHint,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 24.dp),
        )
    }
}
