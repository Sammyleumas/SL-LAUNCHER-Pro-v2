package com.sllauncher.pro.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Where a home item lives. */
object HomeContainer {
    const val PAGE = 0
    const val DOCK = 1
    const val FOLDER = 2
}

/** What a home item is. */
object HomeItemType {
    const val APP = 0
    const val SHORTCUT = 1
    const val WIDGET = 2
    const val FOLDER = 3
}

/**
 * One thing placed on the home screen, dock or inside a folder.
 *
 * Grid position is [cell] = row * columns + column on [page]. Widgets cover
 * [spanX] x [spanY] cells starting at [cell]. Folder children use
 * container FOLDER, [folderId] = the folder item's id and [cell] = order.
 */
@Entity(tableName = "home_items", indices = [Index("container", "page"), Index("folderId")])
data class HomeItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val container: Int,
    val page: Int = 0,
    val cell: Int,
    val spanX: Int = 1,
    val spanY: Int = 1,
    val type: Int,
    /** Flattened [com.sllauncher.pro.data.apps.AppKey] for APP items. */
    val appKey: String? = null,
    val shortcutPackage: String? = null,
    val shortcutId: String? = null,
    val userSerial: Long = 0,
    val widgetId: Int = -1,
    val folderId: Long? = null,
    /** Folder name, or a shortcut's label. */
    val label: String? = null,
)

/** Per-app launcher data: hidden/favourite flags, category override, usage for "Recent". */
@Entity(tableName = "app_meta")
data class AppMetaEntity(
    @PrimaryKey val appKey: String,
    val hidden: Boolean = false,
    val favorite: Boolean = false,
    /** "builtin:Games" or "custom:<id>", or null to use automatic detection. */
    val categoryOverride: String? = null,
    val launchCount: Int = 0,
    val lastLaunchedAt: Long = 0,
)

@Entity(tableName = "custom_categories")
data class CustomCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val position: Int = 0,
)

@Entity(tableName = "search_history", indices = [Index(value = ["query"], unique = true)])
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val searchedAt: Long,
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String,
    val pinned: Boolean = false,
    val updatedAt: Long,
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val done: Boolean = false,
    /** Epoch millis, or null for no due time. */
    val dueAt: Long? = null,
    /** When set, a reminder notification is scheduled for [dueAt]. */
    val remind: Boolean = false,
    val createdAt: Long,
)

@Entity(tableName = "countdowns")
data class CountdownEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val targetAt: Long,
)

/** Cached, saved or read news articles. Only headline-level data is stored, never full article text. */
@Entity(tableName = "articles", indices = [Index("category"), Index("publishedAt")])
data class ArticleEntity(
    @PrimaryKey val url: String,
    val title: String,
    val summary: String,
    val imageUrl: String?,
    val publisher: String,
    val category: String,
    val publishedAt: Long,
    val fetchedAt: Long,
    val saved: Boolean = false,
    val readAt: Long? = null,
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

@Entity(tableName = "playlist_tracks", primaryKeys = ["playlistId", "position"])
data class PlaylistTrackEntity(
    val playlistId: Long,
    val position: Int,
    /** MediaStore audio id. */
    val mediaId: Long,
)

/** Local music favourites and play history (MediaStore ids). */
@Entity(tableName = "track_stats")
data class TrackStatsEntity(
    @PrimaryKey val mediaId: Long,
    val favorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedAt: Long = 0,
)

/**
 * Notification history, kept only when the user turns it on in Privacy.
 * Stored on this device only, never uploaded, deleted after 7 days.
 */
@Entity(tableName = "notification_history", indices = [Index("postedAt")])
data class NotificationHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val text: String,
    val postedAt: Long,
)

/** A saved customization profile (Work, Study, Gaming...): a settings snapshot as JSON. */
@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val name: String,
    val settingsJson: String,
    val updatedAt: Long,
)
