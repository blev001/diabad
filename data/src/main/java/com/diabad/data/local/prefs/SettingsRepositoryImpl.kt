package com.diabad.data.local.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.diabad.core.alarm.AlarmVibrationId
import com.diabad.domain.model.AlarmSoundId
import com.diabad.domain.model.AppSettings
import com.diabad.domain.model.ConnectionLossMode
import com.diabad.domain.model.ThemeMode
import com.diabad.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
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
            hyperThresholdMmol = prefs[KEY_HYPER_THRESHOLD]
                ?: AppSettings.DEFAULT_HYPER_THRESHOLD_MMOL,
            connectionLossMode = prefs[KEY_CONNECTION_LOSS_MODE]
                ?.let { runCatching { ConnectionLossMode.valueOf(it) }.getOrNull() }
                ?: ConnectionLossMode.SILENT,
            connectionLossGraceMinutes = prefs[KEY_CONNECTION_LOSS_GRACE]
                ?: AppSettings.DEFAULT_CONNECTION_LOSS_GRACE_MINUTES,
            alarmSoundId = prefs[KEY_ALARM_SOUND]
                ?.let { runCatching { AlarmSoundId.valueOf(it) }.getOrNull() }
                ?: AlarmSoundId.SIREN,
            alarmVibrationId = AlarmVibrationId.fromName(prefs[KEY_ALARM_VIBRATION]),
            customAlarmUri = prefs[KEY_CUSTOM_ALARM_URI],
            snoozeMinutes = prefs[KEY_SNOOZE_MINUTES]
                ?: AppSettings.DEFAULT_SNOOZE_MINUTES,
            themeMode = prefs[KEY_THEME_MODE]
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
        )
    }.distinctUntilChanged()

    override suspend fun setHypoThresholdMmol(value: Double) {
        dataStore.edit { it[KEY_HYPO_THRESHOLD] = value.coerceIn(2.0, 6.0) }
    }

    override suspend fun setHyperThresholdMmol(value: Double) {
        dataStore.edit { it[KEY_HYPER_THRESHOLD] = value.coerceIn(7.0, 20.0) }
    }

    override suspend fun setConnectionLossMode(mode: ConnectionLossMode) {
        dataStore.edit { it[KEY_CONNECTION_LOSS_MODE] = mode.name }
    }

    override suspend fun setConnectionLossGraceMinutes(minutes: Int) {
        dataStore.edit { it[KEY_CONNECTION_LOSS_GRACE] = minutes.coerceIn(1, 120) }
    }

    override suspend fun setAlarmSoundId(id: AlarmSoundId) {
        dataStore.edit { it[KEY_ALARM_SOUND] = id.name }
    }

    override suspend fun setAlarmVibrationId(id: AlarmVibrationId) {
        dataStore.edit { it[KEY_ALARM_VIBRATION] = id.name }
    }

    override suspend fun setCustomAlarmUri(uri: String?) {
        dataStore.edit {
            if (uri.isNullOrBlank()) it.remove(KEY_CUSTOM_ALARM_URI)
            else it[KEY_CUSTOM_ALARM_URI] = uri
        }
    }

    override suspend fun setSnoozeMinutes(minutes: Int) {
        val allowed = AppSettings.SNOOZE_OPTIONS_MINUTES
        dataStore.edit {
            it[KEY_SNOOZE_MINUTES] = if (minutes in allowed) minutes else AppSettings.DEFAULT_SNOOZE_MINUTES
        }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    private companion object {
        val KEY_HYPO_THRESHOLD = doublePreferencesKey("hypo_threshold_mmol")
        val KEY_HYPER_THRESHOLD = doublePreferencesKey("hyper_threshold_mmol")
        val KEY_CONNECTION_LOSS_MODE = stringPreferencesKey("connection_loss_mode")
        val KEY_CONNECTION_LOSS_GRACE = intPreferencesKey("connection_loss_grace_minutes")
        val KEY_ALARM_SOUND = stringPreferencesKey("alarm_sound_id")
        val KEY_ALARM_VIBRATION = stringPreferencesKey("alarm_vibration_id")
        val KEY_CUSTOM_ALARM_URI = stringPreferencesKey("custom_alarm_uri")
        val KEY_SNOOZE_MINUTES = intPreferencesKey("snooze_minutes")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
