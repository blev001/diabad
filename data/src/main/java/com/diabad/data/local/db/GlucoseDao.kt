package com.diabad.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GlucoseDao {
    @Query("SELECT * FROM glucose_readings ORDER BY timestampMillis DESC LIMIT 1")
    fun observeLatest(): Flow<GlucoseReadingEntity?>

    @Query("SELECT * FROM glucose_readings ORDER BY timestampMillis DESC LIMIT 1")
    suspend fun latest(): GlucoseReadingEntity?

    @Query("SELECT * FROM glucose_readings ORDER BY timestampMillis DESC LIMIT 1 OFFSET 1")
    fun observePrevious(): Flow<GlucoseReadingEntity?>

    @Query("SELECT * FROM glucose_readings ORDER BY timestampMillis ASC")
    fun observeAll(): Flow<List<GlucoseReadingEntity>>

    @Query("SELECT * FROM glucose_readings ORDER BY timestampMillis DESC LIMIT :limit")
    fun observeNewestDesc(limit: Int): Flow<List<GlucoseReadingEntity>>

    @Query(
        "SELECT * FROM glucose_readings WHERE timestampMillis >= :fromMillis " +
            "AND timestampMillis < :toMillis ORDER BY timestampMillis ASC",
    )
    suspend fun getRange(fromMillis: Long, toMillis: Long): List<GlucoseReadingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<GlucoseReadingEntity>)

    @Query("DELETE FROM glucose_readings WHERE timestampMillis < :cutoffMillis")
    suspend fun deleteOlderThan(cutoffMillis: Long)
}
