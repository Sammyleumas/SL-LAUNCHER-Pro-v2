package com.sllauncher.pro.data.apps

import android.content.ComponentName
import android.content.pm.ApplicationInfo
import android.os.UserHandle

/**
 * Identifies one launchable activity for one user profile (personal or
 * work). Stored as a flat string: "package/class#userSerial".
 */
data class AppKey(val packageName: String, val className: String, val userSerial: Long) {
    val component: ComponentName get() = ComponentName(packageName, className)

    fun flatten(): String = "$packageName/$className#$userSerial"

    companion object {
        fun parse(raw: String): AppKey? {
            val slash = raw.indexOf('/')
            val hash = raw.lastIndexOf('#')
            if (slash <= 0 || hash <= slash + 1) return null
            val serial = raw.substring(hash + 1).toLongOrNull() ?: return null
            return AppKey(raw.substring(0, slash), raw.substring(slash + 1, hash), serial)
        }
    }
}

/** SL's drawer categories. [Other] apps appear only under "All". */
enum class AppCategory { Social, Media, Tools, Games, Productivity, Education, Finance, Other }

data class LauncherApp(
    val key: AppKey,
    val label: String,
    val user: UserHandle,
    val isWorkProfile: Boolean,
    val autoCategory: AppCategory,
    val firstInstallTime: Long,
)

/** An app plus the user's launcher-level data for it. */
data class AppEntry(
    val app: LauncherApp,
    val hidden: Boolean,
    val favorite: Boolean,
    /** Built-in category after any user override. */
    val category: AppCategory,
    /** Set when the user moved the app into a custom category. */
    val customCategoryId: Long?,
    val launchCount: Int,
    val lastLaunchedAt: Long,
)

/**
 * Picks a category for an app.
 *
 * Android 8+ apps can declare a category (games, audio, video, social,
 * productivity...). Education and finance have no system category, so for
 * those, and for apps that declare nothing, a short list of package-name
 * keywords is used. It is a best guess: users can move any app.
 */
fun classifyApp(androidCategory: Int, isGame: Boolean, packageName: String): AppCategory {
    if (isGame) return AppCategory.Games
    val pkg = packageName.lowercase()
    keywordCategory(pkg, onlyStrong = true)?.let { return it }
    return when (androidCategory) {
        ApplicationInfo.CATEGORY_GAME -> AppCategory.Games
        ApplicationInfo.CATEGORY_AUDIO,
        ApplicationInfo.CATEGORY_VIDEO,
        ApplicationInfo.CATEGORY_IMAGE,
        ApplicationInfo.CATEGORY_NEWS -> AppCategory.Media
        ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.Social
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.Productivity
        ApplicationInfo.CATEGORY_MAPS,
        ApplicationInfo.CATEGORY_ACCESSIBILITY -> AppCategory.Tools
        else -> keywordCategory(pkg, onlyStrong = false) ?: AppCategory.Other
    }
}

/** Finance and education win over a declared category (a bank app may declare "productivity"). */
private val StrongKeywords = listOf(
    AppCategory.Finance to listOf("bank", "wallet", "opay", "palmpay", "kuda", "moniepoint", "paystack", "flutterwave", "binance", "crypto", "finance", "money", "paypal", "chipper"),
    AppCategory.Education to listOf("edu", "learn", "school", "academy", "classroom", "duolingo", "course", "study", "quizlet", "khan"),
)

private val WeakKeywords = listOf(
    AppCategory.Social to listOf("whatsapp", "telegram", "facebook", "instagram", "twitter", "tiktok", "snapchat", "messenger", "discord", "signal", "linkedin", "threads", "mms", "messaging"),
    AppCategory.Media to listOf("music", "video", "player", "spotify", "youtube", "audiomack", "boomplay", "netflix", "gallery", "photo", "camera", "podcast", "radio", "showmax"),
    AppCategory.Productivity to listOf("docs", "office", "mail", "gmail", "calendar", "notes", "drive", "sheets", "slides", "outlook", "teams", "zoom", "meet", "keep", "notion"),
    AppCategory.Tools to listOf("calculator", "clock", "files", "filemanager", "settings", "flashlight", "scanner", "browser", "chrome", "vpn", "weather", "contacts", "dialer", "phone"),
)

private fun keywordCategory(pkg: String, onlyStrong: Boolean): AppCategory? {
    val lists = if (onlyStrong) StrongKeywords else StrongKeywords + WeakKeywords
    return lists.firstOrNull { (_, words) -> words.any { pkg.contains(it) } }?.first
}
