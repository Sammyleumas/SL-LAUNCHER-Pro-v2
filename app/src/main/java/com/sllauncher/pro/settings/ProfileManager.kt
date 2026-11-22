package com.sllauncher.pro.settings

import com.sllauncher.pro.data.db.ProfileDao
import com.sllauncher.pro.data.db.ProfileEntity
import com.sllauncher.pro.data.settings.LauncherSettings
import com.sllauncher.pro.data.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Built-in starting points. Each changes only layout and attention settings, never personal data. */
val BuiltInProfiles: Map<String, (LauncherSettings) -> LauncherSettings> = mapOf(
    "Minimal" to { s ->
        s.copy(homeShowLabels = false, dockEnabled = false, homeShowSearchBar = false, homeShowOverview = false,
            islandShowCharging = false, drawerListMode = true, drawerShowRecents = false)
    },
    "Work" to { s ->
        s.copy(homeShowLabels = true, dockEnabled = true, homeShowOverview = true, islandShowMessages = true,
            drawerHiddenCategories = setOf("Games"))
    },
    "Study" to { s ->
        s.copy(homeShowOverview = true, islandShowMessages = false, drawerHiddenCategories = setOf("Games", "Social"))
    },
    "Gaming" to { s ->
        s.copy(islandShowMessages = false, islandShowCharging = true, drawerHiddenCategories = emptySet(), lowPerformanceMode = false)
    },
    "Personal" to { s ->
        s.copy(homeShowLabels = true, dockEnabled = true, homeShowSearchBar = true, homeShowOverview = true,
            islandShowMessages = true, islandShowCharging = true, drawerListMode = false, drawerShowRecents = true, drawerHiddenCategories = emptySet())
    },
)

private const val UNDO_SLOT = "__before_profile__"

/**
 * Saves and switches customization profiles. A profile is a snapshot of
 * the launcher settings; onboarding state and privacy choices are never
 * overwritten by switching.
 */
@Singleton
class ProfileManager @Inject constructor(
    private val dao: ProfileDao,
    private val settings: SettingsRepository,
) {
    val saved: Flow<List<ProfileEntity>> = dao.observeAll()

    fun builtInNames(): List<String> = BuiltInProfiles.keys.toList()

    /** Applies a built-in or saved profile. Returns false if no profile has that name. */
    suspend fun apply(name: String): Boolean {
        val current = settings.current
        val target = BuiltInProfiles.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.let { (key, transform) ->
            transform(current).copy(activeProfile = key)
        } ?: dao.get(name)?.let { settings.decodeOrNull(it.settingsJson) }?.copy(activeProfile = name)
            ?: return false
        dao.upsert(ProfileEntity(UNDO_SLOT, settings.encode(current), System.currentTimeMillis()))
        settings.replace(keepPrivateChoices(target, current))
        return true
    }

    /** Restores the settings from before the last profile switch. */
    suspend fun undo(): Boolean {
        val previous = dao.get(UNDO_SLOT)?.let { settings.decodeOrNull(it.settingsJson) } ?: return false
        settings.replace(keepPrivateChoices(previous, settings.current))
        return true
    }

    suspend fun saveCurrentAs(name: String) {
        val clean = name.trim().take(24)
        if (clean.isEmpty() || clean == UNDO_SLOT) return
        dao.upsert(ProfileEntity(clean, settings.encode(settings.current.copy(activeProfile = clean)), System.currentTimeMillis()))
    }

    suspend fun delete(profile: ProfileEntity) = dao.delete(profile)

    /** Profiles change appearance, not privacy or setup state. */
    private fun keepPrivateChoices(target: LauncherSettings, current: LauncherSettings) = target.copy(
        onboardingComplete = current.onboardingComplete,
        notificationHistoryEnabled = current.notificationHistoryEnabled,
        hideNotificationContent = current.hideNotificationContent,
        searchContacts = current.searchContacts,
        searchMusic = current.searchMusic,
        searchHistoryEnabled = current.searchHistoryEnabled,
    )
}
