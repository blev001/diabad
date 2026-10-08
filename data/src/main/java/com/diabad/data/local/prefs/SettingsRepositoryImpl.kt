package com.diabad.data.local.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.diabad.core.alarm.AlarmSnoozeSlots
import com.diabad.core.alarm.AlarmVibrationId
import com.diabad.domain.model.AlarmAlertMode
import com.diabad.domain.model.AlarmSoundId
import com.diabad.domain.model.AppSettings
import com.diabad.domain.model.ConnectionLossMode
import com.diabad.domain.model.ThemeMode
import com.diabad.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override fun observe(): Flow<AppSettings> = dataStore.data.map { it.toSettings() }

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { prefs ->
            val next = transform(prefs.toSettings())
            prefs[KEY_HYPO_THRESHOLD] = next.hypoThresholdMmol
            prefs[KEY_HYPER_THRESHOLD] = next.hyperThresholdMmol
            prefs[KEY_APPROACHING_HYPO_ENABLED] = next.approachingHypoEnabled
            prefs[KEY_APPROACHING_HYPO_THRESHOLD] = next.approachingHypoThresholdMmol
            prefs[KEY_CONNECTION_LOSS_MODE] = next.connectionLossMode.name
            prefs[KEY_CONNECTION_LOSS_GRACE] = next.connectionLossGraceMinutes
            prefs[KEY_ALARM_ALERT_MODE] = next.alarmAlertMode.name
            prefs[KEY_ALARM_SOUND] = next.alarmSoundId.name
            prefs[KEY_ALARM_VIBRATION] = next.alarmVibrationId.name
            val customUri = next.customAlarmUri
            if (customUri.isNullOrBlank()) prefs.remove(KEY_CUSTOM_ALARM_URI)
            else prefs[KEY_CUSTOM_ALARM_URI] = customUri
            prefs[KEY_SNOOZE_MINUTES] = AlarmSnoozeSlots.normalize(next.snoozeMinutes)
            if (next.alarmSnoozedUntilMillis <= 0L) prefs.remove(KEY_ALARM_SNOOZED_UNTIL)
            else prefs[KEY_ALARM_SNOOZED_UNTIL] = next.alarmSnoozedUntilMillis
            prefs[KEY_THEME_MODE] = next.themeMode.name
            prefs[KEY_RE_ALARM] = next.reAlarmMinutes
            prefs[KEY_VIBRATE_FIRST] = next.vibrateFirstSeconds
            prefs[KEY_HYPER_ENABLED] = next.hyperAlarmEnabled
            prefs[KEY_HYPER_DELAY] = next.hyperDelayMinutes
            prefs[KEY_HYPER_VIBRATE_ONLY] = next.hyperVibrateOnly
            prefs[KEY_PREDICTIVE] = next.predictiveLowEnabled
            prefs[KEY_PREDICTIVE_MINUTES] = next.predictiveLowMinutes
            prefs[KEY_FAST_DROP] = next.fastDropEnabled
            prefs[KEY_NIGHT] = next.nightProfileEnabled
            prefs[KEY_NIGHT_START] = next.nightStartMinute
            prefs[KEY_NIGHT_END] = next.nightEndMinute
            prefs[KEY_NIGHT_HYPO] = next.nightHypoThresholdMmol
            prefs[KEY_NIGHT_HYPER] = next.nightHyperThresholdMmol
            prefs[KEY_NIGHT_HYPER_ENABLED] = next.nightHyperAlarmEnabled
            prefs[KEY_HEALTH] = next.healthConnectEnabled
        }
    }

    private fun Preferences.toSettings(): AppSettings = AppSettings(
            hypoThresholdMmol = this[KEY_HYPO_THRESHOLD]
                ?: AppSettings.DEFAULT_HYPO_THRESHOLD_MMOL,
            hyperThresholdMmol = this[KEY_HYPER_THRESHOLD]
                ?: AppSettings.DEFAULT_HYPER_THRESHOLD_MMOL,
            approachingHypoEnabled = this[KEY_APPROACHING_HYPO_ENABLED] ?: true,
            approachingHypoThresholdMmol = this[KEY_APPROACHING_HYPO_THRESHOLD]
                ?: AppSettings.DEFAULT_APPROACHING_HYPO_THRESHOLD_MMOL,
            connectionLossMode = this[KEY_CONNECTION_LOSS_MODE]
                ?.let { runCatching { ConnectionLossMode.valueOf(it) }.getOrNull() }
                ?: ConnectionLossMode.SILENT,
            connectionLossGraceMinutes = this[KEY_CONNECTION_LOSS_GRACE]
                ?: AppSettings.DEFAULT_CONNECTION_LOSS_GRACE_MINUTES,
            alarmAlertMode = this[KEY_ALARM_ALERT_MODE]
                ?.let { runCatching { AlarmAlertMode.valueOf(it) }.getOrNull() }
                ?: AlarmAlertMode.SOUND,
            alarmSoundId = this[KEY_ALARM_SOUND]
                ?.let { runCatching { AlarmSoundId.valueOf(it) }.getOrNull() }
                ?: AlarmSoundId.SIREN,
            alarmVibrationId = AlarmVibrationId.fromName(this[KEY_ALARM_VIBRATION]),
            customAlarmUri = this[KEY_CUSTOM_ALARM_URI],
            snoozeMinutes = AlarmSnoozeSlots.normalize(
                this[KEY_SNOOZE_MINUTES] ?: AppSettings.DEFAULT_SNOOZE_MINUTES,
            ),
            alarmSnoozedUntilMillis = this[KEY_ALARM_SNOOZED_UNTIL] ?: 0L,
            themeMode = this[KEY_THEME_MODE]
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            reAlarmMinutes = this[KEY_RE_ALARM] ?: AppSettings.DEFAULT_RE_ALARM_MINUTES,
            vibrateFirstSeconds = this[KEY_VIBRATE_FIRST] ?: 0,
            hyperAlarmEnabled = this[KEY_HYPER_ENABLED] ?: true,
            hyperDelayMinutes = this[KEY_HYPER_DELAY] ?: 0,
            hyperVibrateOnly = this[KEY_HYPER_VIBRATE_ONLY] ?: false,
            // Retired features stay disabled even when an older version saved them as enabled.
            predictiveLowEnabled = false,
            predictiveLowMinutes = this[KEY_PREDICTIVE_MINUTES]
                ?: AppSettings.DEFAULT_PREDICTIVE_LOW_MINUTES,
            fastDropEnabled = false,
            nightProfileEnabled = false,
            nightStartMinute = this[KEY_NIGHT_START] ?: AppSettings.DEFAULT_NIGHT_START_MINUTE,
            nightEndMinute = this[KEY_NIGHT_END] ?: AppSettings.DEFAULT_NIGHT_END_MINUTE,
            nightHypoThresholdMmol = this[KEY_NIGHT_HYPO] ?: AppSettings.DEFAULT_HYPO_THRESHOLD_MMOL,
            nightHyperThresholdMmol = this[KEY_NIGHT_HYPER]
                ?: AppSettings.DEFAULT_NIGHT_HYPER_THRESHOLD_MMOL,
            nightHyperAlarmEnabled = this[KEY_NIGHT_HYPER_ENABLED] ?: true,
            healthConnectEnabled = false,
        )

    override suspend fun setHypoThresholdMmol(value: Double) {
        dataStore.edit { prefs ->
            val hypo = value.coerceIn(2.0, 6.0)
            prefs[KEY_HYPO_THRESHOLD] = hypo
            val approaching = prefs[KEY_APPROACHING_HYPO_THRESHOLD]
                ?: AppSettings.DEFAULT_APPROACHING_HYPO_THRESHOLD_MMOL
            if (approaching <= hypo) {
                prefs[KEY_APPROACHING_HYPO_THRESHOLD] =
                    (hypo + 0.3).coerceAtMost(6.5)
            }
        }
    }

    override suspend fun setHyperThresholdMmol(value: Double) {
        dataStore.edit { it[KEY_HYPER_THRESHOLD] = value.coerceIn(7.0, 20.0) }
    }

    override suspend fun setApproachingHypoEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_APPROACHING_HYPO_ENABLED] = enabled }
    }

    override suspend fun setApproachingHypoThresholdMmol(value: Double) {
        dataStore.edit { prefs ->
            val hypo = prefs[KEY_HYPO_THRESHOLD] ?: AppSettings.DEFAULT_HYPO_THRESHOLD_MMOL
            prefs[KEY_APPROACHING_HYPO_THRESHOLD] =
                value.coerceIn((hypo + 0.1).coerceAtMost(6.5), 6.5)
        }
    }

    override suspend fun setConnectionLossMode(mode: ConnectionLossMode) {
        dataStore.edit { it[KEY_CONNECTION_LOSS_MODE] = mode.name }
    }

    override suspend fun setConnectionLossGraceMinutes(minutes: Int) {
        dataStore.edit { it[KEY_CONNECTION_LOSS_GRACE] = minutes.coerceIn(1, 120) }
    }

    override suspend fun setAlarmAlertMode(mode: AlarmAlertMode) {
        dataStore.edit { it[KEY_ALARM_ALERT_MODE] = mode.name }
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
        dataStore.edit {
            it[KEY_SNOOZE_MINUTES] = AlarmSnoozeSlots.normalize(minutes)
        }
    }

    override suspend fun setAlarmSnoozedUntilMillis(untilMillis: Long) {
        dataStore.edit {
            if (untilMillis <= 0L) it.remove(KEY_ALARM_SNOOZED_UNTIL)
            else it[KEY_ALARM_SNOOZED_UNTIL] = untilMillis
        }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    private companion object {
        val KEY_HYPO_THRESHOLD = doublePreferencesKey("hypo_threshold_mmol")
        val KEY_HYPER_THRESHOLD = doublePreferencesKey("hyper_threshold_mmol")
        val KEY_APPROACHING_HYPO_ENABLED = booleanPreferencesKey("approaching_hypo_enabled")
        val KEY_APPROACHING_HYPO_THRESHOLD = doublePreferencesKey("approaching_hypo_threshold_mmol")
        val KEY_CONNECTION_LOSS_MODE = stringPreferencesKey("connection_loss_mode")
        val KEY_CONNECTION_LOSS_GRACE = intPreferencesKey("connection_loss_grace_minutes")
        val KEY_ALARM_ALERT_MODE = stringPreferencesKey("alarm_alert_mode")
        val KEY_ALARM_SOUND = stringPreferencesKey("alarm_sound_id")
        val KEY_ALARM_VIBRATION = stringPreferencesKey("alarm_vibration_id")
        val KEY_CUSTOM_ALARM_URI = stringPreferencesKey("custom_alarm_uri")
        val KEY_SNOOZE_MINUTES = intPreferencesKey("snooze_minutes")
        val KEY_ALARM_SNOOZED_UNTIL = longPreferencesKey("alarm_snoozed_until_millis")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_RE_ALARM = intPreferencesKey("re_alarm_minutes")
        val KEY_VIBRATE_FIRST = intPreferencesKey("vibrate_first_seconds")
        val KEY_HYPER_ENABLED = booleanPreferencesKey("hyper_alarm_enabled")
        val KEY_HYPER_DELAY = intPreferencesKey("hyper_delay_minutes")
        val KEY_HYPER_VIBRATE_ONLY = booleanPreferencesKey("hyper_vibrate_only")
        val KEY_PREDICTIVE = booleanPreferencesKey("predictive_low_enabled")
        val KEY_PREDICTIVE_MINUTES = intPreferencesKey("predictive_low_minutes")
        val KEY_FAST_DROP = booleanPreferencesKey("fast_drop_enabled")
        val KEY_NIGHT = booleanPreferencesKey("night_profile_enabled")
        val KEY_NIGHT_START = intPreferencesKey("night_start_minute")
        val KEY_NIGHT_END = intPreferencesKey("night_end_minute")
        val KEY_NIGHT_HYPO = doublePreferencesKey("night_hypo_threshold_mmol")
        val KEY_NIGHT_HYPER = doublePreferencesKey("night_hyper_threshold_mmol")
        val KEY_NIGHT_HYPER_ENABLED = booleanPreferencesKey("night_hyper_alarm_enabled")
        val KEY_HEALTH = booleanPreferencesKey("health_connect_enabled")
    }
}
