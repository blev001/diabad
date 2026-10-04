package com.diabad.health

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.BloodGlucose
import com.diabad.core.di.ApplicationScope
import com.diabad.domain.model.GlucoseReading
import com.diabad.domain.repository.GlucoseRepository
import com.diabad.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

enum class HealthConnectStatus { UNAVAILABLE, NEEDS_INSTALL, AVAILABLE }

/**
 * Mirrors CGM readings into Health Connect (read by Samsung Health) as interstitial
 * blood glucose. Records use `diabad-<timestamp>` client ids, so re-sending is an upsert.
 */
@Singleton
class HealthConnectSync @Inject constructor(
    @ApplicationContext private val context: Context,
    private val glucoseRepository: GlucoseRepository,
    private val settingsRepository: SettingsRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {
    val permissions: Set<String> = setOf(HealthPermission.getWritePermission(BloodGlucoseRecord::class))

    private val prefs by lazy { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    private val mutex = Mutex()
    private var job: Job? = null

    fun status(): HealthConnectStatus = when (HealthConnectClient.getSdkStatus(context)) {
        HealthConnectClient.SDK_AVAILABLE -> HealthConnectStatus.AVAILABLE
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectStatus.NEEDS_INSTALL
        else -> HealthConnectStatus.UNAVAILABLE
    }

    suspend fun hasPermissions(): Boolean {
        if (status() != HealthConnectStatus.AVAILABLE) return false
        return runCatching {
            client().permissionController.getGrantedPermissions().containsAll(permissions)
        }.getOrDefault(false)
    }

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            combine(
                glucoseRepository.observeLatest(),
                settingsRepository.observe(),
            ) { latest, settings -> latest?.timestampMillis to settings.healthConnectEnabled }
                .distinctUntilChanged()
                .collect { (_, enabled) -> if (enabled) syncPending() }
        }
    }

    private suspend fun syncPending() = mutex.withLock {
        try {
            if (!hasPermissions()) return@withLock
            val now = System.currentTimeMillis()
            val from = maxOf(prefs.getLong(KEY_LAST_SYNCED, 0L) + 1, now - BACKFILL_MS)
            val readings = glucoseRepository.getRange(from, now + 1)
            if (readings.isEmpty()) return@withLock
            readings.chunked(CHUNK).forEach { chunk ->
                client().insertRecords(chunk.map(::toRecord))
            }
            prefs.edit().putLong(KEY_LAST_SYNCED, readings.last().timestampMillis).apply()
            Log.i(TAG, "Wrote ${readings.size} reading(s) to Health Connect")
        } catch (t: Throwable) {
            Log.w(TAG, "Health Connect sync failed", t)
        }
    }

    private fun toRecord(r: GlucoseReading): BloodGlucoseRecord {
        val time = Instant.ofEpochMilli(r.timestampMillis)
        return BloodGlucoseRecord(
            time = time,
            zoneOffset = ZoneId.systemDefault().rules.getOffset(time),
            metadata = Metadata.autoRecorded(
                device = Device(type = Device.TYPE_PHONE),
                clientRecordId = "diabad-${r.timestampMillis}",
                clientRecordVersion = 1L,
            ),
            level = BloodGlucose.millimolesPerLiter(r.mmol),
            specimenSource = BloodGlucoseRecord.SPECIMEN_SOURCE_INTERSTITIAL_FLUID,
            mealType = MealType.MEAL_TYPE_UNKNOWN,
            relationToMeal = BloodGlucoseRecord.RELATION_TO_MEAL_GENERAL,
        )
    }

    private fun client() = HealthConnectClient.getOrCreate(context)

    private companion object {
        const val TAG = "HealthConnectSync"
        const val PREFS = "health_connect"
        const val KEY_LAST_SYNCED = "last_synced_ts"
        const val BACKFILL_MS = 7L * 24 * 60 * 60 * 1000
        const val CHUNK = 500
    }
}
