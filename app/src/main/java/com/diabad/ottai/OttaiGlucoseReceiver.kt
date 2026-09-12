package com.diabad.ottai

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.diabad.core.di.ApplicationScope
import com.diabad.data.ottai.OttaiBroadcastParser
import com.diabad.data.ottai.OttaiIntents
import com.diabad.domain.usecase.IngestGlucoseReadingsUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class OttaiGlucoseReceiver : BroadcastReceiver() {

    @Inject lateinit var parser: OttaiBroadcastParser
    @Inject lateinit var ingestGlucoseReadings: IngestGlucoseReadingsUseCase
    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != OttaiIntents.ACTION_OTTAI_INTL && action != OttaiIntents.ACTION_OTTAI_CN) {
            return
        }

        val collection = intent.getStringExtra(OttaiIntents.EXTRA_COLLECTION)
        if (collection != OttaiIntents.COLLECTION_ENTRIES) {
            Log.w(TAG, "Ignored OtTai broadcast: collection=$collection")
            return
        }

        val data = intent.getStringExtra(OttaiIntents.EXTRA_DATA)
        if (data.isNullOrBlank()) {
            Log.w(TAG, "Ignored OtTai broadcast: empty data")
            return
        }

        val pendingResult = goAsync()
        applicationScope.launch {
            try {
                val readings = parser.parseEntriesJson(data)
                ingestGlucoseReadings(readings)
                Log.i(TAG, "Ingested ${readings.size} OtTai reading(s)")
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to parse/ingest OtTai payload", t)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "OttaiGlucoseReceiver"
    }
}
