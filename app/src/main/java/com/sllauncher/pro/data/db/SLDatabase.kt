package com.sllauncher.pro.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * SL Launcher's on-device database. Everything here stays on the phone.
 *
 * Version 1 is the first released schema; any later change must add a
 * Migration (schemas are exported to app/schemas for verification).
 */
@Database(
    entities = [
        HomeItemEntity::class,
        AppMetaEntity::class,
        CustomCategoryEntity::class,
        SearchHistoryEntity::class,
        NoteEntity::class,
        TaskEntity::class,
        CountdownEntity::class,
        ArticleEntity::class,
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
        TrackStatsEntity::class,
        NotificationHistoryEntity::class,
        ProfileEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class SLDatabase : RoomDatabase() {
    abstract fun homeItemDao(): HomeItemDao
    abstract fun appMetaDao(): AppMetaDao
    abstract fun categoryDao(): CategoryDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun noteDao(): NoteDao
    abstract fun taskDao(): TaskDao
    abstract fun countdownDao(): CountdownDao
    abstract fun articleDao(): ArticleDao
    abstract fun musicDao(): MusicDao
    abstract fun notificationHistoryDao(): NotificationHistoryDao
    abstract fun profileDao(): ProfileDao
}
