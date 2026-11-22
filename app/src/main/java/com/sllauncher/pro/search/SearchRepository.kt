package com.sllauncher.pro.search

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.sllauncher.pro.commands.CommandParser
import com.sllauncher.pro.commands.LauncherCommand
import com.sllauncher.pro.commands.SettingsPanel
import com.sllauncher.pro.data.apps.AppEntry
import com.sllauncher.pro.data.apps.AppRepository
import com.sllauncher.pro.data.db.ArticleDao
import com.sllauncher.pro.data.db.ArticleEntity
import com.sllauncher.pro.data.settings.LauncherSettings
import com.sllauncher.pro.drawer.matchScore
import com.sllauncher.pro.drawer.normalizeForSearch
import com.sllauncher.pro.navigation.AboutDestination
import com.sllauncher.pro.navigation.CustomizationDestination
import com.sllauncher.pro.navigation.DeviceInfoDestination
import com.sllauncher.pro.navigation.MusicDestination
import com.sllauncher.pro.navigation.NewsDestination
import com.sllauncher.pro.navigation.NotificationCenterDestination
import com.sllauncher.pro.navigation.PrivacyDestination
import com.sllauncher.pro.navigation.ProductivityDestination
import com.sllauncher.pro.navigation.QuickSettingsDestination
import com.sllauncher.pro.navigation.SettingsDestination
import com.sllauncher.pro.navigation.WidgetPickerDestination
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

enum class ResultType(val label: String) { Action("Actions"), App("Apps"), Contact("Contacts"), Setting("Settings"), Music("Music"), News("News"), Web("Web") }

sealed interface SearchResult {
    val key: String
    val title: String
    val subtitle: String?
    val type: ResultType

    data class App(val entry: AppEntry) : SearchResult {
        override val key = "app:" + entry.app.key.flatten()
        override val title = entry.app.label
        override val subtitle = if (entry.app.isWorkProfile) "Work app" else null
        override val type = ResultType.App
    }

    data class Action(val command: LauncherCommand) : SearchResult {
        override val key = "action:$command"
        override val title = command.description
        override val subtitle = if (command.needsConfirmation) "You'll be asked to confirm" else null
        override val type = ResultType.Action
    }

    data class Contact(val id: Long, val name: String, val number: String?, val lookupUri: Uri) : SearchResult {
        override val key = "contact:$id"
        override val title = name
        override val subtitle = number
        override val type = ResultType.Contact
    }

    /** An SL screen ([route]) or an Android settings panel ([panel]). */
    data class Setting(override val title: String, override val subtitle: String?, val route: Any? = null, val panel: SettingsPanel? = null) : SearchResult {
        override val key = "setting:$title"
        override val type = ResultType.Setting
    }

    data class Track(val mediaId: Long, override val title: String, val artist: String?) : SearchResult {
        override val key = "track:$mediaId"
        override val subtitle = artist
        override val type = ResultType.Music
    }

    data class Article(val article: ArticleEntity) : SearchResult {
        override val key = "news:" + article.url
        override val title = article.title
        override val subtitle = article.publisher
        override val type = ResultType.News
    }

    data class Web(val query: String) : SearchResult {
        override val key = "web:$query"
        override val title = "Search the web for \"$query\""
        override val subtitle = null
        override val type = ResultType.Web
    }
}

/** Searchable SL screens and Android panels, with extra keywords people actually type. */
private data class SettingsEntry(val title: String, val subtitle: String, val keywords: String, val route: Any? = null, val panel: SettingsPanel? = null)

private val settingsIndex = listOf(
    SettingsEntry("Wi-Fi", "Android settings", "wifi wireless internet network", panel = SettingsPanel.Wifi),
    SettingsEntry("Bluetooth", "Android settings", "bluetooth headphones speaker pair", panel = SettingsPanel.Bluetooth),
    SettingsEntry("Mobile data and internet", "Android settings", "data mobile internet sim", panel = SettingsPanel.Internet),
    SettingsEntry("Airplane mode", "Android settings", "airplane flight mode", panel = SettingsPanel.Airplane),
    SettingsEntry("Display and brightness", "Android settings", "display brightness screen dark mode", panel = SettingsPanel.Display),
    SettingsEntry("Battery", "Android settings", "battery power saver charging", panel = SettingsPanel.Battery),
    SettingsEntry("Volume and sound", "Android settings", "sound volume ringtone vibrate", panel = SettingsPanel.Sound),
    SettingsEntry("Hotspot and tethering", "Android settings", "hotspot tethering share internet", panel = SettingsPanel.Hotspot),
    SettingsEntry("SL settings", "SL Launcher Pro", "settings launcher preferences options", route = SettingsDestination),
    SettingsEntry("Customize", "SL Launcher Pro", "theme accent icon pack grid columns dock labels wallpaper profile minimal", route = CustomizationDestination),
    SettingsEntry("Widgets", "SL Launcher Pro", "widget add widget", route = WidgetPickerDestination),
    SettingsEntry("Privacy and backup", "SL Launcher Pro", "privacy permissions backup restore export import hidden apps", route = PrivacyDestination),
    SettingsEntry("Quick settings", "SL Launcher Pro", "quick settings toggles flashlight torch brightness volume", route = QuickSettingsDestination),
    SettingsEntry("Device info", "SL Launcher Pro", "device phone model storage memory ram android version", route = DeviceInfoDestination),
    SettingsEntry("Notifications", "SL Launcher Pro", "notifications alerts messages", route = NotificationCenterDestination),
    SettingsEntry("News", "SL Launcher Pro", "news headlines nigeria world technology", route = NewsDestination),
    SettingsEntry("Music", "SL Launcher Pro", "music songs player playlist", route = MusicDestination),
    SettingsEntry("Notes and tasks", "SL Launcher Pro", "notes tasks reminders todo focus pomodoro timer calendar", route = ProductivityDestination),
    SettingsEntry("About SL Launcher", "SL Launcher Pro", "about version licences privacy policy", route = AboutDestination),
)

@Singleton
class SearchRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val apps: AppRepository,
    private val articles: ArticleDao,
) {
    fun hasContactsPermission() = granted(Manifest.permission.READ_CONTACTS)

    fun hasMusicPermission() = granted(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE,
    )

    private fun granted(permission: String) = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    suspend fun search(query: String, settings: LauncherSettings): List<SearchResult> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isEmpty()) return@withContext emptyList()
        buildList {
            CommandParser.parse(q)?.let { add(SearchResult.Action(it)) }
            addAll(searchApps(q))
            addAll(searchSettings(q))
            if (settings.searchContacts && hasContactsPermission()) addAll(searchContacts(q))
            if (settings.searchMusic && hasMusicPermission()) addAll(searchMusic(q))
            addAll(articles.search(q).take(5).map { SearchResult.Article(it) })
            add(SearchResult.Web(q))
        }
    }

    private fun searchApps(q: String): List<SearchResult> =
        apps.entries.value
            .filter { !it.hidden || normalizeForSearch(it.app.label) == normalizeForSearch(q) }
            .map { it to matchScore(it.app.label, q) }
            .filter { it.second > 0 }
            .sortedWith(compareByDescending<Pair<AppEntry, Double>> { it.second }.thenByDescending { it.first.launchCount })
            .take(12)
            .map { SearchResult.App(it.first) }

    private fun searchSettings(q: String): List<SearchResult> {
        val n = normalizeForSearch(q)
        return settingsIndex
            .filter { normalizeForSearch(it.title).contains(n) || it.keywords.split(' ').any { word -> word.startsWith(n) } }
            .take(5)
            .map { SearchResult.Setting(it.title, it.subtitle, it.route, it.panel) }
    }

    private fun searchContacts(q: String): List<SearchResult> {
        val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_FILTER_URI, Uri.encode(q))
        val projection = arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.LOOKUP_KEY, ContactsContract.Contacts.DISPLAY_NAME_PRIMARY, ContactsContract.Contacts.HAS_PHONE_NUMBER)
        val results = mutableListOf<SearchResult>()
        runCatching {
            context.contentResolver.query(uri, projection, null, null, null)?.use { c ->
                while (c.moveToNext() && results.size < 6) {
                    val id = c.getLong(0)
                    val lookup = c.getString(1)
                    val name = c.getString(2) ?: continue
                    val number = if (c.getInt(3) > 0) firstNumber(id) else null
                    results += SearchResult.Contact(id, name, number, ContactsContract.Contacts.getLookupUri(id, lookup))
                }
            }
        }
        return results
    }

    private fun firstNumber(contactId: Long): String? = runCatching {
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            null,
        )?.use { if (it.moveToFirst()) it.getString(0) else null }
    }.getOrNull()

    private fun searchMusic(q: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        runCatching {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST),
                "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND (${MediaStore.Audio.Media.TITLE} LIKE ? OR ${MediaStore.Audio.Media.ARTIST} LIKE ?)",
                arrayOf("%$q%", "%$q%"),
                "${MediaStore.Audio.Media.TITLE} ASC",
            )?.use { c ->
                while (c.moveToNext() && results.size < 6) {
                    results += SearchResult.Track(c.getLong(0), c.getString(1) ?: "Unknown", c.getString(2))
                }
            }
        }
        return results
    }

    fun contactUri(id: Long): Uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id)
}
