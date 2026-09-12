package com.diabad.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.diabad.domain.model.GlucoseReading
import com.diabad.domain.model.GlucoseSource
import com.diabad.domain.model.TrendArrow

@Entity(tableName = "glucose_readings")
data class GlucoseReadingEntity(
    @PrimaryKey val timestampMillis: Long,
    val mmol: Double,
    val trend: String,
    val source: String,
)

fun GlucoseReadingEntity.toDomain(): GlucoseReading = GlucoseReading(
    mmol = mmol,
    timestampMillis = timestampMillis,
    trend = TrendArrow.entries.firstOrNull { it.name == trend } ?: TrendArrow.NONE,
    source = GlucoseSource.entries.firstOrNull { it.name == source } ?: GlucoseSource.OTTAI,
)

fun GlucoseReading.toEntity(): GlucoseReadingEntity = GlucoseReadingEntity(
    timestampMillis = timestampMillis,
    mmol = mmol,
    trend = trend.name,
    source = source.name,
)
