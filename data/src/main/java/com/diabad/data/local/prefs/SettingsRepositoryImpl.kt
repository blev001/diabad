package com.diabad.data.local.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.diabad.domain.model.AppSettings
import com.diabad.domain.model.ConnectionLossMode
import com.diabad.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override fun observe(): Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            hypoThresholdMmol = prefs[KEY_HYPO_THRESHOLD]
                ?: AppSettings.DEFAULT_HYPO_THRESHOLD_MMOL,
            connectionLossMode = prefs[KEY_CONNECTION_LOSS_MODE]
                ?.let { runCatching { ConnectionLossMode.valueOf(it) }.getOrNull() }
                ?: ConnectionLossMode.SILENT,
            connectionLossGraceMinutes = prefs[KEY_CONNECTION_LOSS_GRACE]
                ?: AppSettings.DEFAULT_CONNECTION_LOSS_GRACE_MINUTES,
        )
    }

    override suspend fun setHypoThresholdMmol(value: Double) {
        dataStore.edit { it[KEY_HYPO_THRESHOLD] = value }
    }

    override suspend fun setConnectionLossMode(mode: ConnectionLossMode) {
        dataStore.edit { it[KEY_CONNECTION_LOSS_MODE] = mode.name }
    }

    override suspend fun setConnectionLossGraceMinutes(minutes: Int) {
        dataStore.edit { it[KEY_CONNECTION_LOSS_GRACE] = minutes.coerceIn(1, 120) }
    }

    private companion object {
        val KEY_HYPO_THRESHOLD = doublePreferencesKey("hypo_threshold_mmol")
        val KEY_CONNECTION_LOSS_MODE = stringPreferencesKey("connection_loss_mode")
        val KEY_CONNECTION_LOSS_GRACE = intPreferencesKey("connection_loss_grace_minutes")
    }
}
