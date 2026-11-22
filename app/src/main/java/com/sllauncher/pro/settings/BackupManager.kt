package com.sllauncher.pro.settings

import androidx.room.withTransaction
import com.sllauncher.pro.data.db.AppMetaEntity
import com.sllauncher.pro.data.db.CountdownEntity
import com.sllauncher.pro.data.db.CustomCategoryEntity
import com.sllauncher.pro.data.db.HomeItemEntity
import com.sllauncher.pro.data.db.HomeItemType
import com.sllauncher.pro.data.db.NoteEntity
import com.sllauncher.pro.data.db.ProfileEntity
import com.sllauncher.pro.data.db.SLDatabase
import com.sllauncher.pro.data.db.TaskEntity
import com.sllauncher.pro.data.settings.LauncherSettings
import com.sllauncher.pro.data.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

const val BACKUP_FORMAT = "sl-launcher-backup"
const val BACKUP_VERSION = 1

@Serializable data class HomeItemDto(val id: Long, val container: Int, val page: Int, val cell: Int, val spanX: Int, val spanY: Int, val type: Int, val appKey: String? = null, val shortcutPackage: String? = null, val shortcutId: String? = null, val userSerial: Long = 0, val folderId: Long? = null, val label: String? = null)
@Serializable data class AppMetaDto(val appKey: String, val hidden: Boolean, val favorite: Boolean, val categoryOverride: String? = null)
@Serializable data class CategoryDto(val id: Long, val name: String, val position: Int)
@Serializable data class NoteDto(val title: String, val body: String, val pinned: Boolean, val updatedAt: Long)
@Serializable data class TaskDto(val title: String, val done: Boolean, val dueAt: Long? = null, val remind: Boolean = false, val createdAt: Long)
@Serializable data class CountdownDto(val title: String, val targetAt: Long)
@Serializable data class ProfileDto(val name: String, val settingsJson: String)

/** Everything SL can restore. Usage counts and notification history are deliberately not included. */
@Serializable
data class SLBackup(
    val format: String = BACKUP_FORMAT,
    val version: Int = BACKUP_VERSION,
    val createdAt: Long,
    val settings: LauncherSettings,
    val homeItems: List<HomeItemDto> = emptyList(),
    val appMeta: List<AppMetaDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val notes: List<NoteDto> = emptyList(),
    val tasks: List<TaskDto> = emptyList(),
    val countdowns: List<CountdownDto> = emptyList(),
    val profiles: List<ProfileDto> = emptyList(),
)

class InvalidBackupException(message: String) : IllegalArgumentException(message)

/** Parses and validates a backup file's text. Pure, so it is unit-tested. */
fun parseBackup(json: Json, text: String): SLBackup {
    if (text.length > 5_000_000) throw InvalidBackupException("This file is too large to be an SL backup.")
    val backup = try {
        json.decodeFromString(SLBackup.serializer(), text)
    } catch (e: IllegalArgumentException) {
        throw InvalidBackupException("This isn't an SL Launcher backup file.")
    }
    if (backup.format != BACKUP_FORMAT) throw InvalidBackupException("This isn't an SL Launcher backup file.")
    if (backup.version > BACKUP_VERSION) throw InvalidBackupException("This backup was made by a newer SL version. Update SL first.")
    val ids = backup.homeItems.map { it.id }.toSet()
    if (ids.size != backup.homeItems.size) throw InvalidBackupException("The home layout in this backup is damaged.")
    if (backup.homeItems.any { it.folderId != null && it.folderId !in ids }) throw InvalidBackupException("The home layout in this backup is damaged.")
    return backup
}

fun encodeBackup(json: Json, backup: SLBackup): String = json.encodeToString(SLBackup.serializer(), backup)

@Singleton
class BackupManager @Inject constructor(
    private val db: SLDatabase,
    private val settings: SettingsRepository,
    private val json: Json,
) {
    suspend fun export(): String {
        val backup = SLBackup(
            createdAt = System.currentTimeMillis(),
            settings = settings.current,
            // Widgets are tied to this phone's widget host and cannot be restored elsewhere.
            homeItems = db.homeItemDao().getAll().filter { it.type != HomeItemType.WIDGET }.map {
                HomeItemDto(it.id, it.container, it.page, it.cell, it.spanX, it.spanY, it.type, it.appKey, it.shortcutPackage, it.shortcutId, it.userSerial, it.folderId, it.label)
            },
            appMeta = db.appMetaDao().getAll().filter { it.hidden || it.favorite || it.categoryOverride != null }.map {
                AppMetaDto(it.appKey, it.hidden, it.favorite, it.categoryOverride)
            },
            categories = db.categoryDao().getAll().map { CategoryDto(it.id, it.name, it.position) },
            notes = db.noteDao().getAll().map { NoteDto(it.title, it.body, it.pinned, it.updatedAt) },
            tasks = db.taskDao().getAll().map { TaskDto(it.title, it.done, it.dueAt, it.remind, it.createdAt) },
            countdowns = db.countdownDao().observeAll().first().map { CountdownDto(it.title, it.targetAt) },
            profiles = db.profileDao().observeAll().first().filter { !it.name.startsWith("__") }.map { ProfileDto(it.name, it.settingsJson) },
        )
        return encodeBackup(json, backup)
    }

    /** Replaces layout and data with the backup's in one transaction; nothing changes if it fails. */
    suspend fun import(text: String): SLBackup {
        val backup = parseBackup(json, text)
        db.withTransaction {
            db.homeItemDao().replaceAll(backup.homeItems.map {
                HomeItemEntity(it.id, it.container, it.page, it.cell, it.spanX, it.spanY, it.type, it.appKey, it.shortcutPackage, it.shortcutId, it.userSerial, -1, it.folderId, it.label)
            })
            db.appMetaDao().upsertAll(backup.appMeta.map { AppMetaEntity(it.appKey, it.hidden, it.favorite, it.categoryOverride) })
            db.categoryDao().clear()
            db.categoryDao().insertAll(backup.categories.map { CustomCategoryEntity(it.id, it.name, it.position) })
            db.noteDao().insertAll(backup.notes.map { NoteEntity(title = it.title, body = it.body, pinned = it.pinned, updatedAt = it.updatedAt) })
            db.taskDao().insertAll(backup.tasks.map { TaskEntity(title = it.title, done = it.done, dueAt = it.dueAt, remind = false, createdAt = it.createdAt) })
            backup.countdowns.forEach { db.countdownDao().upsert(CountdownEntity(title = it.title, targetAt = it.targetAt)) }
            backup.profiles.forEach { db.profileDao().upsert(ProfileEntity(it.name, it.settingsJson, System.currentTimeMillis())) }
        }
        settings.replace(backup.settings.copy(onboardingComplete = true))
        return backup
    }
}
