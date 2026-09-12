package com.diabad.wear

import android.content.Context
import android.util.Log
import com.diabad.core.wear.WearGlucosePaths
import com.diabad.domain.model.AppSettings
import com.diabad.domain.model.GlucoseReading
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Pushes latest glucose to Galaxy Watch for tiles and complications. */
@Singleton
class WatchGlucoseSync @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataClient by lazy { Wearable.getDataClient(context) }

    suspend fun push(
        latest: GlucoseReading?,
        previous: GlucoseReading?,
        settings: AppSettings,
        alarming: Boolean,
    ) {
        if (latest == null) return
        try {
            val request = PutDataMapRequest.create(WearGlucosePaths.LATEST).apply {
                dataMap.putDouble(WearGlucosePaths.KEY_MMOL, latest.mmol)
                dataMap.putString(WearGlucosePaths.KEY_TREND, latest.trend.name)
                dataMap.putLong(WearGlucosePaths.KEY_TIMESTAMP, latest.timestampMillis)
                dataMap.putDouble(WearGlucosePaths.KEY_THRESHOLD, settings.hypoThresholdMmol)
                dataMap.putDouble(WearGlucosePaths.KEY_HYPER_THRESHOLD, settings.hyperThresholdMmol)
                dataMap.putBoolean(WearGlucosePaths.KEY_ALARMING, alarming)
                if (previous != null) {
                    dataMap.putBoolean(WearGlucosePaths.KEY_HAS_DELTA, true)
                    dataMap.putDouble(WearGlucosePaths.KEY_DELTA, latest.mmol - previous.mmol)
                } else {
                    dataMap.putBoolean(WearGlucosePaths.KEY_HAS_DELTA, false)
                    dataMap.putDouble(WearGlucosePaths.KEY_DELTA, 0.0)
                }
            }.asPutDataRequest().setUrgent()
            dataClient.putDataItem(request).await()
            Log.d(TAG, "Synced glucose ${latest.mmol} → watch")
        } catch (e: Exception) {
            Log.w(TAG, "Glucose sync failed: ${e.message}")
        }
    }

    private companion object {
        const val TAG = "WatchGlucoseSync"
    }
}
