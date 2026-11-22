package com.sllauncher.pro.settings

import com.sllauncher.pro.data.settings.LauncherSettings
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupParseTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; coerceInputValues = true }

    private fun sample() = SLBackup(
        createdAt = 1L,
        settings = LauncherSettings(homeColumns = 5, onboardingComplete = true),
        homeItems = listOf(
            HomeItemDto(id = 1, container = 0, page = 0, cell = 0, spanX = 1, spanY = 1, type = 3, label = "Work"),
            HomeItemDto(id = 2, container = 2, page = 0, cell = 0, spanX = 1, spanY = 1, type = 0, appKey = "a/b#0", folderId = 1),
        ),
        notes = listOf(NoteDto("Shopping", "Bread", false, 5)),
    )

    @Test
    fun `round trip keeps everything`() {
        val restored = parseBackup(json, encodeBackup(json, sample()))
        assertEquals(sample(), restored)
    }

    @Test
    fun `wrong files are rejected with a clear reason`() {
        assertThrows(InvalidBackupException::class.java) { parseBackup(json, "not json") }
        assertThrows(InvalidBackupException::class.java) { parseBackup(json, encodeBackup(json, sample().copy(format = "something-else"))) }
    }

    @Test
    fun `backups from newer versions are refused`() {
        assertThrows(InvalidBackupException::class.java) { parseBackup(json, encodeBackup(json, sample().copy(version = BACKUP_VERSION + 1))) }
    }

    @Test
    fun `damaged layouts are refused before anything is replaced`() {
        val orphan = sample().copy(homeItems = sample().homeItems + HomeItemDto(3, 2, 0, 1, 1, 1, 0, appKey = "x/y#0", folderId = 99))
        assertThrows(InvalidBackupException::class.java) { parseBackup(json, encodeBackup(json, orphan)) }
        val duplicate = sample().copy(homeItems = sample().homeItems + sample().homeItems.first())
        assertThrows(InvalidBackupException::class.java) { parseBackup(json, encodeBackup(json, duplicate)) }
    }

    @Test
    fun `older backups missing new settings still load with defaults`() {
        val minimal = """{"format":"sl-launcher-backup","version":1,"createdAt":1,"settings":{"homeColumns":6}}"""
        val parsed = parseBackup(json, minimal)
        assertEquals(6, parsed.settings.homeColumns)
        assertEquals(LauncherSettings().drawerColumns, parsed.settings.drawerColumns)
    }
}
