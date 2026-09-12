package com.diabad.domain.repository

import com.diabad.domain.model.AppSettings
import com.diabad.domain.model.ConnectionLossMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observe(): Flow<AppSettings>

    suspend fun setHypoThresholdMmol(value: Double)

    suspend fun setConnectionLossMode(mode: ConnectionLossMode)

    suspend fun setConnectionLossGraceMinutes(minutes: Int)
}
