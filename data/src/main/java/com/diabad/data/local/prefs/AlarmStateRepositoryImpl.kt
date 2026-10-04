package com.diabad.data.local.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.diabad.domain.model.AlarmReason
import com.diabad.domain.model.AlarmState
import com.diabad.domain.repository.AlarmStateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmStateRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : AlarmStateRepository {

    override fun observe(): Flow<AlarmState> =
        dataStore.data.map { read(it) }.distinctUntilChanged()

    override suspend fun get(): AlarmState = observe().first()

    override suspend fun save(state: AlarmState) {
        dataStore.edit { p ->
            p[KEY_SNOOZED_UNTIL] = state.snoozedUntilMillis
            p.putOrRemove(KEY_SNOOZED_REASON, state.snoozedReason?.name)
            p[KEY_DISMISSED_AT] = state.dismissedAtMillis
            p.putOrRemove(KEY_DISMISSED_REASON, state.dismissedReason?.name)
            val mmol = state.dismissedMmol
            if (mmol == null) p.remove(KEY_DISMISSED_MMOL) else p[KEY_DISMISSED_MMOL] = mmol
            p[KEY_RINGING_SINCE] = state.ringingSinceMillis
            p.putOrRemove(KEY_RINGING_REASON, state.ringingReason?.name)
            p[KEY_LAST_REMIND] = state.lastRemindAtMillis
        }
    }

    private fun read(p: Preferences) = AlarmState(
        snoozedUntilMillis = p[KEY_SNOOZED_UNTIL] ?: 0L,
        snoozedReason = p[KEY_SNOOZED_REASON].toReason(),
        dismissedAtMillis = p[KEY_DISMISSED_AT] ?: 0L,
        dismissedReason = p[KEY_DISMISSED_REASON].toReason(),
        dismissedMmol = p[KEY_DISMISSED_MMOL],
        ringingSinceMillis = p[KEY_RINGING_SINCE] ?: 0L,
        ringingReason = p[KEY_RINGING_REASON].toReason(),
        lastRemindAtMillis = p[KEY_LAST_REMIND] ?: 0L,
    )

    private fun String?.toReason(): AlarmReason? =
        this?.let { runCatching { AlarmReason.valueOf(it) }.getOrNull() }

    private fun MutablePreferences.putOrRemove(
        key: Preferences.Key<String>,
        value: String?,
    ) {
        if (value == null) remove(key) else this[key] = value
    }

    private companion object {
        val KEY_SNOOZED_UNTIL = longPreferencesKey("alarm_snoozed_until")
        val KEY_SNOOZED_REASON = stringPreferencesKey("alarm_snoozed_reason")
        val KEY_DISMISSED_AT = longPreferencesKey("alarm_dismissed_at")
        val KEY_DISMISSED_REASON = stringPreferencesKey("alarm_dismissed_reason")
        val KEY_DISMISSED_MMOL = doublePreferencesKey("alarm_dismissed_mmol")
        val KEY_RINGING_SINCE = longPreferencesKey("alarm_ringing_since")
        val KEY_RINGING_REASON = stringPreferencesKey("alarm_ringing_reason")
        val KEY_LAST_REMIND = longPreferencesKey("alarm_last_remind")
    }
}
