package com.sllauncher.pro.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface HomeItemDao {
    @Query("SELECT * FROM home_items ORDER BY container, page, cell")
    fun observeAll(): Flow<List<HomeItemEntity>>

    @Query("SELECT * FROM home_items")
    suspend fun getAll(): List<HomeItemEntity>

    @Query("SELECT COUNT(*) FROM home_items")
    suspend fun count(): Int

    @Insert
    suspend fun insert(item: HomeItemEntity): Long

    @Insert
    suspend fun insertAll(items: List<HomeItemEntity>)

    @Update
    suspend fun update(item: HomeItemEntity)

    @Update
    suspend fun updateAll(items: List<HomeItemEntity>)

    @Query("DELETE FROM home_items WHERE id = :id OR folderId = :id")
    suspend fun deleteWithChildren(id: Long)

    @Query("DELETE FROM home_items WHERE type = 0 AND appKey = :appKey")
    suspend fun deleteAppEverywhere(appKey: String)

    @Query("DELETE FROM home_items")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(items: List<HomeItemEntity>) {
        clear()
        insertAll(items)
    }
}

@Dao
interface AppMetaDao {
    @Query("SELECT * FROM app_meta")
    fun observeAll(): Flow<List<AppMetaEntity>>

    @Query("SELECT * FROM app_meta")
    suspend fun getAll(): List<AppMetaEntity>

    @Query("SELECT * FROM app_meta WHERE appKey = :key")
    suspend fun get(key: String): AppMetaEntity?

    @Upsert
    suspend fun upsert(meta: AppMetaEntity)

    @Upsert
    suspend fun upsertAll(meta: List<AppMetaEntity>)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM custom_categories ORDER BY position, name")
    fun observeAll(): Flow<List<CustomCategoryEntity>>

    @Query("SELECT * FROM custom_categories")
    suspend fun getAll(): List<CustomCategoryEntity>

    @Insert
    suspend fun insert(category: CustomCategoryEntity): Long

    @Delete
    suspend fun delete(category: CustomCategoryEntity)

    @Query("DELETE FROM custom_categories")
    suspend fun clear()

    @Insert
    suspend fun insertAll(categories: List<CustomCategoryEntity>)
}

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history ORDER BY searchedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 20): Flow<List<SearchHistoryEntity>>

    @Query("INSERT OR REPLACE INTO search_history (query, searchedAt) VALUES (:query, :at)")
    suspend fun record(query: String, at: Long)

    @Query("DELETE FROM search_history WHERE query = :query")
    suspend fun delete(query: String)

    @Query("DELETE FROM search_history")
    suspend fun clear()
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY pinned DESC, updatedAt DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes")
    suspend fun getAll(): List<NoteEntity>

    @Upsert
    suspend fun upsert(note: NoteEntity): Long

    @Delete
    suspend fun delete(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<NoteEntity>)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY done, CASE WHEN dueAt IS NULL THEN 1 ELSE 0 END, dueAt, createdAt DESC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks")
    suspend fun getAll(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun get(id: Long): TaskEntity?

    @Upsert
    suspend fun upsert(task: TaskEntity): Long

    @Delete
    suspend fun delete(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>)
}

@Dao
interface CountdownDao {
    @Query("SELECT * FROM countdowns ORDER BY targetAt")
    fun observeAll(): Flow<List<CountdownEntity>>

    @Upsert
    suspend fun upsert(countdown: CountdownEntity)

    @Delete
    suspend fun delete(countdown: CountdownEntity)
}

@Dao
interface ArticleDao {
    @Query("SELECT * FROM articles ORDER BY publishedAt DESC LIMIT 300")
    fun observeAll(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE saved = 1 ORDER BY publishedAt DESC")
    fun observeSaved(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE readAt IS NOT NULL ORDER BY readAt DESC LIMIT 100")
    fun observeHistory(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE url = :url")
    suspend fun get(url: String): ArticleEntity?

    @Query("SELECT * FROM articles ORDER BY publishedAt DESC LIMIT :limit")
    suspend fun latest(limit: Int): List<ArticleEntity>

    @Query("SELECT * FROM articles WHERE title LIKE '%' || :q || '%' OR summary LIKE '%' || :q || '%' ORDER BY publishedAt DESC LIMIT 20")
    suspend fun search(q: String): List<ArticleEntity>

    /** Inserts new articles; existing rows keep their saved/read flags. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNew(articles: List<ArticleEntity>)

    @Query("UPDATE articles SET saved = :saved WHERE url = :url")
    suspend fun setSaved(url: String, saved: Boolean)

    @Query("UPDATE articles SET readAt = :at WHERE url = :url")
    suspend fun markRead(url: String, at: Long)

    /** Drops old unsaved, unread headlines so the cache stays small. */
    @Query("DELETE FROM articles WHERE saved = 0 AND readAt IS NULL AND fetchedAt < :before")
    suspend fun pruneBefore(before: Long)
}

@Dao
interface MusicDao {
    @Query("SELECT * FROM playlists ORDER BY name")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Insert
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY position")
    fun observePlaylistTracks(playlistId: Long): Flow<List<PlaylistTrackEntity>>

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun lastPosition(playlistId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: PlaylistTrackEntity)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND position = :position")
    suspend fun removeTrack(playlistId: Long, position: Int)

    @Query("SELECT * FROM track_stats")
    fun observeStats(): Flow<List<TrackStatsEntity>>

    @Query("SELECT * FROM track_stats WHERE mediaId = :id")
    suspend fun stats(id: Long): TrackStatsEntity?

    @Upsert
    suspend fun upsertStats(stats: TrackStatsEntity)
}

@Dao
interface NotificationHistoryDao {
    @Query("SELECT * FROM notification_history ORDER BY postedAt DESC LIMIT 300")
    fun observeAll(): Flow<List<NotificationHistoryEntity>>

    @Insert
    suspend fun insert(entry: NotificationHistoryEntity)

    @Query("DELETE FROM notification_history WHERE postedAt < :before")
    suspend fun pruneBefore(before: Long)

    @Query("DELETE FROM notification_history")
    suspend fun clear()
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY name")
    fun observeAll(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE name = :name")
    suspend fun get(name: String): ProfileEntity?

    @Upsert
    suspend fun upsert(profile: ProfileEntity)

    @Delete
    suspend fun delete(profile: ProfileEntity)
}
