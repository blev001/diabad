package com.diabad.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import com.diabad.core.di.ApplicationScope
import com.diabad.core.di.DefaultDispatcher
import com.diabad.core.di.IoDispatcher
import com.diabad.data.local.db.DiabadDatabase
import com.diabad.data.local.db.GlucoseDao
import com.diabad.data.local.prefs.SettingsRepositoryImpl
import com.diabad.data.repository.GlucoseRepositoryImpl
import com.diabad.domain.repository.GlucoseRepository
import com.diabad.domain.repository.SettingsRepository
import com.diabad.domain.usecase.IngestGlucoseReadingsUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindingsModule {
    @Binds
    @Singleton
    abstract fun bindGlucoseRepository(impl: GlucoseRepositoryImpl): GlucoseRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): CoroutineScope = CoroutineScope(SupervisorJob() + ioDispatcher)

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): DiabadDatabase = Room.databaseBuilder(
        context,
        DiabadDatabase::class.java,
        "diabad.db",
    ).build()

    @Provides
    fun provideGlucoseDao(db: DiabadDatabase): GlucoseDao = db.glucoseDao()

    @Provides
    @Singleton
    fun provideSettingsDataStore(
        @ApplicationContext context: Context,
        @ApplicationScope scope: CoroutineScope,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { context.preferencesDataStoreFile("diabad_settings") },
    )

    @Provides
    @Singleton
    fun provideIngestGlucoseReadingsUseCase(
        glucoseRepository: GlucoseRepository,
    ): IngestGlucoseReadingsUseCase = IngestGlucoseReadingsUseCase(glucoseRepository)
}
