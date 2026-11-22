package com.sllauncher.pro.data.settings

import kotlinx.serialization.Serializable

enum class ThemeMode { Light, Dark, Auto }

/**
 * Curated accents, all free. Each has a deep tone for the light theme
 * (white text on it, at least 4.5:1) and a pale tone for the dark theme
 * (dark text on it).
 */
enum class AccentPreset(val lightArgb: Long, val darkArgb: Long) {
    Blue(0xFF3A55D9, 0xFF8FA2FF),
    Teal(0xFF00695C, 0xFF4DD0C1),
    Coral(0xFFC2410C, 0xFFFF9E7A),
    Purple(0xFF7E22CE, 0xFFC59BFF),
    Green(0xFF1B5E20, 0xFF7FD68A),
    ;

    fun argbFor(dark: Boolean): Long = if (dark) darkArgb else lightArgb
}

enum class DisplayFont { Brand, System }

/** Shape every app icon is cut to. [System] keeps the icon's own shape. */
enum class IconShape { System, Circle, Squircle, Square }

enum class DrawerSort { Name, MostUsed, RecentlyInstalled }

enum class IslandPosition { Center, Start }

/** What a home-screen gesture does. */
enum class GestureAction(val label: String) {
    None("Nothing"),
    LockScreen("Lock the screen"),
    Notifications("Open the notification shade"),
    NotificationCenter("Open SL notifications"),
    Search("Open search"),
    Assistant("Open the SL assistant"),
    Drawer("Open the app drawer"),
}

/** A news feed the user added by URL. Shown under "My feeds". */
@Serializable
data class CustomFeed(val name: String, val url: String)

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
    val themeMode: ThemeMode = ThemeMode.Light,
    val accent: AccentPreset = AccentPreset.Blue,
    val displayFont: DisplayFont = DisplayFont.Brand,
    val iconPackPackage: String? = null,
    val iconShape: IconShape = IconShape.System,
    /** Sits each icon on a raised soft plate (the soft-UI look). */
    val iconSoftPlate: Boolean = true,

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

    // Gestures (v2)
    val gestureDoubleTap: GestureAction = GestureAction.LockScreen,
    val gestureSwipeDown: GestureAction = GestureAction.Notifications,

    // News (v2)
    val customFeeds: List<CustomFeed> = emptyList(),

    // Focus (v2)
    /** Turns on Do Not Disturb (priority only) while a focus session runs. */
    val focusDoNotDisturb: Boolean = false,
    /** Asks before opening social, games and media apps during a focus session. */
    val focusGuardDistractions: Boolean = true,

    // Drawer suggestions and wallpaper (v2)
    val drawerShowSuggestions: Boolean = true,
    /** Rotates SL's built-in soft wallpapers once a day. */
    val dailySoftWallpaper: Boolean = false,
)
