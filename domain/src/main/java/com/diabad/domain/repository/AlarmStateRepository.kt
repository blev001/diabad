package com.diabad.domain.repository

import com.diabad.domain.model.AlarmState
import kotlinx.coroutines.flow.Flow

interface AlarmStateRepository {
    fun observe(): Flow<AlarmState>

    suspend fun get(): AlarmState

    suspend fun save(state: AlarmState)
}
