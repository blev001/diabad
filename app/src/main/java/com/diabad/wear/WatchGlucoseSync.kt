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

    @Volatile
    private var lastValueKey: ValueKey? = null

    @Volatile
    private var lastPushedAtMillis: Long = 0L

    suspend fun push(
        latest: GlucoseReading?,
        previous: GlucoseReading?,
        settings: AppSettings,
        alarming: Boolean,
    ) {
        if (latest == null) return
        val hasDelta = previous != null
        val delta = if (previous != null) latest.mmol - previous.mmol else 0.0
        val key = ValueKey(
            mmolBits = latest.mmol.toBits(),
            trend = latest.trend.name,
            thresholdBits = settings.hypoThresholdMmol.toBits(),
            hyperBits = settings.hyperThresholdMmol.toBits(),
            alarming = alarming,
            hasDelta = hasDelta,
            deltaBits = delta.toBits(),
        )
        val now = System.currentTimeMillis()
        val valueUnchanged = key == lastValueKey
        if (valueUnchanged && now - lastPushedAtMillis < HEARTBEAT_MS) return
        lastValueKey = key
        lastPushedAtMillis = now
        try {
            val request = PutDataMapRequest.create(WearGlucosePaths.LATEST).apply {
                dataMap.putDouble(WearGlucosePaths.KEY_MMOL, latest.mmol)
                dataMap.putString(WearGlucosePaths.KEY_TREND, latest.trend.name)
                dataMap.putLong(WearGlucosePaths.KEY_TIMESTAMP, latest.timestampMillis)
                dataMap.putDouble(WearGlucosePaths.KEY_THRESHOLD, settings.hypoThresholdMmol)
                dataMap.putDouble(WearGlucosePaths.KEY_HYPER_THRESHOLD, settings.hyperThresholdMmol)
                dataMap.putBoolean(WearGlucosePaths.KEY_ALARMING, alarming)
                dataMap.putBoolean(WearGlucosePaths.KEY_HAS_DELTA, hasDelta)
                dataMap.putDouble(WearGlucosePaths.KEY_DELTA, delta)
            }.asPutDataRequest()
            val outOfRange = latest.mmol < settings.hypoThresholdMmol ||
                latest.mmol > settings.hyperThresholdMmol
            if (alarming || outOfRange) {
                request.setUrgent()
            }
            dataClient.putDataItem(request).await()
        } catch (e: Exception) {
            lastValueKey = null
            lastPushedAtMillis = 0L
            Log.w(TAG, "Glucose sync failed: ${e.message}")
        }
    }

    private data class ValueKey(
        val mmolBits: Long,
        val trend: String,
        val thresholdBits: Long,
        val hyperBits: Long,
        val alarming: Boolean,
        val hasDelta: Boolean,
        val deltaBits: Long,
    )

    private companion object {
        const val TAG = "WatchGlucoseSync"
        const val HEARTBEAT_MS = 15L * 60L * 1000L
    }
}
