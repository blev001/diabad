package com.diabad.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [GlucoseReadingEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class DiabadDatabase : RoomDatabase() {
    abstract fun glucoseDao(): GlucoseDao
}
