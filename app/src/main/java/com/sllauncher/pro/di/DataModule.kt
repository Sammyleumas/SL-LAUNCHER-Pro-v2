package com.sllauncher.pro.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.sllauncher.pro.data.db.SLDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import javax.inject.Qualifier
import javax.inject.Singleton

/** App-lifetime coroutine scope for work that must outlive a single screen. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("sl_settings") }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SLDatabase =
        Room.databaseBuilder(context, SLDatabase::class.java, "sl_launcher.db").build()

    @Provides fun homeItemDao(db: SLDatabase) = db.homeItemDao()
    @Provides fun appMetaDao(db: SLDatabase) = db.appMetaDao()
    @Provides fun categoryDao(db: SLDatabase) = db.categoryDao()
    @Provides fun searchHistoryDao(db: SLDatabase) = db.searchHistoryDao()
    @Provides fun noteDao(db: SLDatabase) = db.noteDao()
    @Provides fun taskDao(db: SLDatabase) = db.taskDao()
    @Provides fun countdownDao(db: SLDatabase) = db.countdownDao()
    @Provides fun articleDao(db: SLDatabase) = db.articleDao()
    @Provides fun musicDao(db: SLDatabase) = db.musicDao()
    @Provides fun notificationHistoryDao(db: SLDatabase) = db.notificationHistoryDao()
    @Provides fun profileDao(db: SLDatabase) = db.profileDao()
    @Provides fun appLaunchDao(db: SLDatabase) = db.appLaunchDao()
    @Provides fun habitDao(db: SLDatabase) = db.habitDao()
}
