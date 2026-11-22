package com.sllauncher.pro.data.settings

import kotlinx.serialization.Serializable

enum class ThemeMode { Dark, Light }

/** Curated accents. Yellow is the brand; others are premium theme options (Phase 17). */
enum class AccentPreset(val argb: Long, val premium: Boolean) {
    Yellow(0xFFFFD400, premium = false),
    Orange(0xFFFF8A00, premium = true),
    Lime(0xFFC6F432, premium = true),
    Cyan(0xFF2EE6E6, premium = true),
    Pink(0xFFFF5FA2, premium = true),
}

enum class DisplayFont { Brand, System }

enum class DrawerSort { Name, MostUsed, RecentlyInstalled }

enum class IslandPosition { Center, Start }

/**
 * Every user preference in one versioned document. Stored as JSON so it can
 * be backed up, exported and saved as a customization profile as a unit.
 * New fields must have defaults so older backups still load.
 */
@Serializable
data class LauncherSettings(
    val schemaVersion: Int = 1,
    val onboardingComplete: Boolean = false,

    // Home screen
    val homePages: Int = 1,
    val homeColumns: Int = 4,
    val homeRows: Int = 5,
    val homeShowLabels: Boolean = true,
    val homeIconScale: Float = 1f,
    val dockEnabled: Boolean = true,
    val homeShowClock: Boolean = true,
    val homeShowSearchBar: Boolean = true,
    val homeShowOverview: Boolean = true,

    // App drawer
    val drawerColumns: Int = 4,
    val drawerIconScale: Float = 1f,
    val drawerLabelScale: Float = 1f,
    val drawerShowLabels: Boolean = true,
    val drawerListMode: Boolean = false,
    val drawerSort: DrawerSort = DrawerSort.Name,
    val drawerShowAlphabetIndex: Boolean = true,
    val drawerShowRecents: Boolean = true,
    val drawerWidthFraction: Float = 0.8f,
    val drawerHeightFraction: Float = 0.7f,
    val drawerPanelAlpha: Float = 0.94f,
    val drawerHiddenCategories: Set<String> = emptySet(),

    // Dynamic Island
    val islandEnabled: Boolean = true,
    val islandPosition: IslandPosition = IslandPosition.Center,
    val islandHideMessageContent: Boolean = false,
    val islandShowCharging: Boolean = true,
    val islandShowMusic: Boolean = true,
    val islandShowMessages: Boolean = true,

    // Theme
    val themeMode: ThemeMode = ThemeMode.Dark,
    val accent: AccentPreset = AccentPreset.Yellow,
    val displayFont: DisplayFont = DisplayFont.Brand,
    val iconPackPackage: String? = null,

    // Performance and motion
    val lowPerformanceMode: Boolean = false,

    // Search
    val searchContacts: Boolean = false,
    val searchMusic: Boolean = false,
    val searchHistoryEnabled: Boolean = true,

    // Assistant
    val assistantSpeakReplies: Boolean = true,

    // News
    val newsCategories: Set<String> = setOf("Nigeria", "World", "Technology", "Business", "Sports"),
    val newsAutoRefresh: Boolean = true,

    // Notifications and privacy
    val notificationHistoryEnabled: Boolean = false,
    val hideNotificationContent: Boolean = false,
    val mutedNotificationPackages: Set<String> = emptySet(),

    // Profiles
    val activeProfile: String? = null,
)
