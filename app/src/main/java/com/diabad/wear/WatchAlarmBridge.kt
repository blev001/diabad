package com.diabad.wear

import android.content.Context
import android.util.Log
import com.diabad.core.wear.WearAlarmPaths
import com.diabad.domain.model.AppSettings
import com.diabad.domain.model.GlucoseAlarmKind
import com.diabad.domain.model.GlucoseReading
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sends glucose alarm state to a paired Galaxy Watch (Wear OS) so the watch can
 * behave like the system Clock alarm: strong vibration, Stop / Snooze, no sound.
 */
@Singleton
class WatchAlarmBridge @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val messageClient by lazy { Wearable.getMessageClient(context) }
    private val nodeClient by lazy { Wearable.getNodeClient(context) }

    suspend fun ring(latest: GlucoseReading, settings: AppSettings, kind: GlucoseAlarmKind) {
        val threshold = when (kind) {
            GlucoseAlarmKind.HYPO -> settings.hypoThresholdMmol
            GlucoseAlarmKind.HYPER -> settings.hyperThresholdMmol
        }
        val payload = WearAlarmPaths.encodeRing(
            mmol = latest.mmol,
            thresholdMmol = threshold,
            snoozeMinutes = settings.snoozeMinutes,
            kind = when (kind) {
                GlucoseAlarmKind.HYPO -> WearAlarmPaths.KIND_HYPO
                GlucoseAlarmKind.HYPER -> WearAlarmPaths.KIND_HYPER
            },
            vibration = settings.alarmVibrationId.name,
        )
        send(WearAlarmPaths.RING, payload)
    }

    suspend fun clear() {
        send(WearAlarmPaths.CLEAR, ByteArray(0))
    }

    private suspend fun send(path: String, payload: ByteArray) {
        try {
            val nodes = nodeClient.connectedNodes.await()
            if (nodes.isEmpty()) {
                Log.d(TAG, "No Wear nodes for $path")
                return
            }
            for (node in nodes) {
                messageClient.sendMessage(node.id, path, payload).await()
                Log.i(TAG, "Sent $path → ${node.displayName}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Wear send failed for $path: ${e.message}")
        }
    }

    private companion object {
        const val TAG = "WatchAlarmBridge"
    }
}
