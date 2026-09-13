package com.diabad.domain.repository

import com.diabad.core.alarm.AlarmVibrationId
import com.diabad.domain.model.AlarmSoundId
import com.diabad.domain.model.AppSettings
import com.diabad.domain.model.ConnectionLossMode
import com.diabad.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observe(): Flow<AppSettings>

    suspend fun setHypoThresholdMmol(value: Double)

    suspend fun setHyperThresholdMmol(value: Double)

    suspend fun setConnectionLossMode(mode: ConnectionLossMode)

    suspend fun setConnectionLossGraceMinutes(minutes: Int)

    suspend fun setAlarmSoundId(id: AlarmSoundId)

    suspend fun setAlarmVibrationId(id: AlarmVibrationId)

    suspend fun setCustomAlarmUri(uri: String?)

    suspend fun setSnoozeMinutes(minutes: Int)

    suspend fun setThemeMode(mode: ThemeMode)
}
