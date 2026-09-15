package com.diabad.glucose

import android.util.Log
import com.diabad.core.wear.WearGlucosePaths
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

/** Receives CGM snapshots from the phone for tiles and complications. */
class WatchDataListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.use { buffer ->
            for (event in buffer) {
                if (event.type != DataEvent.TYPE_CHANGED) continue
                val path = event.dataItem.uri.path ?: continue
                if (path != WearGlucosePaths.LATEST) continue
                val map = DataMapItem.fromDataItem(event.dataItem).dataMap
                WatchGlucoseStore.write(
                    context = this,
                    mmol = map.getDouble(WearGlucosePaths.KEY_MMOL),
                    trend = map.getString(WearGlucosePaths.KEY_TREND) ?: "NONE",
                    hasDelta = map.getBoolean(WearGlucosePaths.KEY_HAS_DELTA),
                    delta = map.getDouble(WearGlucosePaths.KEY_DELTA),
                    timestampMillis = map.getLong(WearGlucosePaths.KEY_TIMESTAMP),
                    thresholdMmol = map.getDouble(WearGlucosePaths.KEY_THRESHOLD, 3.9),
                    hyperThresholdMmol = map.getDouble(WearGlucosePaths.KEY_HYPER_THRESHOLD, 10.0),
                    alarming = map.getBoolean(WearGlucosePaths.KEY_ALARMING),
                    approaching = map.getBoolean(WearGlucosePaths.KEY_APPROACHING),
                )
                Log.i(TAG, "Glucose data updated on watch")
            }
        }
    }

    private companion object {
        const val TAG = "WatchDataListener"
    }
}
