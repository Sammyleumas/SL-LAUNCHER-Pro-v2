package com.sllauncher.pro.home

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sllauncher.pro.common.UiCommand
import com.sllauncher.pro.common.UiCommandBus
import com.sllauncher.pro.data.apps.AppEntry
import com.sllauncher.pro.data.apps.AppKey
import com.sllauncher.pro.data.apps.AppRepository
import com.sllauncher.pro.data.db.HomeContainer
import com.sllauncher.pro.data.db.HomeItemEntity
import com.sllauncher.pro.data.db.HomeItemType
import com.sllauncher.pro.data.settings.LauncherSettings
import com.sllauncher.pro.data.settings.SettingsRepository
import com.sllauncher.pro.launcher.HomeRoleRepository
import com.sllauncher.pro.widgets.WidgetHostManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import javax.inject.Inject

/** One rendered thing on the home screen. */
sealed interface HomeTile {
    val item: HomeItemEntity
}

data class AppTile(override val item: HomeItemEntity, val entry: AppEntry) : HomeTile
data class ShortcutTile(override val item: HomeItemEntity, val label: String) : HomeTile
data class WidgetTile(override val item: HomeItemEntity) : HomeTile
data class FolderTile(override val item: HomeItemEntity, val name: String, val children: List<AppTile>) : HomeTile

data class HomeUiState(
    val now: ZonedDateTime,
    val isDefaultHome: Boolean,
    val settings: LauncherSettings = LauncherSettings(),
    val pageCount: Int = 1,
    val pages: List<List<HomeTile>> = listOf(emptyList()),
    val dock: List<HomeTile> = emptyList(),
    val ready: Boolean = false,
) {
    val grid: GridSpec get() = GridSpec(settings.homeColumns, settings.homeRows)
    fun folder(id: Long): FolderTile? = (pages.flatten() + dock).filterIsInstance<FolderTile>().firstOrNull { it.item.id == id }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val homeRoleRepository: HomeRoleRepository,
    private val clock: HomeClock,
    private val appRepository: AppRepository,
    private val layout: HomeLayoutRepository,
    private val settingsRepository: SettingsRepository,
    private val widgets: WidgetHostManager,
    private val commandBus: UiCommandBus,
) : ViewModel() {

    private val tiles = combine(layout.items, appRepository.entries, appRepository.loaded) { items, entries, loaded ->
        Triple(items, entries.associateBy { it.app.key.flatten() }, loaded)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        clock.minutes,
        homeRoleRepository.isDefaultHome,
        settingsRepository.settings,
        tiles,
    ) { now, isDefault, settings, (items, entries, loaded) ->
        buildState(now, isDefault, settings, items, entries, loaded)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        HomeUiState(clock.now(), homeRoleRepository.isDefaultHome.value),
    )

    init {
        seedDefaultDockOnFirstRun()
        cleanUpUninstalledApps()
        reflowWhenGridShrinks()
    }

    private fun buildState(
        now: ZonedDateTime,
        isDefault: Boolean,
        settings: LauncherSettings,
        items: List<HomeItemEntity>,
        entries: Map<String, AppEntry>,
        loaded: Boolean,
    ): HomeUiState {
        fun toTile(item: HomeItemEntity): HomeTile? = when (item.type) {
            HomeItemType.APP -> item.appKey?.let { entries[it] }?.let { AppTile(item, it) }
            HomeItemType.SHORTCUT -> ShortcutTile(item, item.label ?: "Shortcut")
            HomeItemType.WIDGET -> WidgetTile(item)
            HomeItemType.FOLDER -> {
                val children = items.filter { it.folderId == item.id }.sortedBy { it.cell }.mapNotNull { child ->
                    child.appKey?.let { entries[it] }?.let { AppTile(child, it) }
                }
                FolderTile(item, item.label ?: "Folder", children)
            }
            else -> null
        }
        val pageItems = items.filter { it.container == HomeContainer.PAGE }
        val pageCount = maxOf(settings.homePages, (pageItems.maxOfOrNull { it.page } ?: 0) + 1)
        return HomeUiState(
            now = now,
            isDefaultHome = isDefault,
            settings = settings,
            pageCount = pageCount,
            pages = (0 until pageCount).map { page -> pageItems.filter { it.page == page }.mapNotNull(::toTile) },
            dock = items.filter { it.container == HomeContainer.DOCK }.sortedBy { it.cell }.mapNotNull(::toTile),
            ready = loaded,
        )
    }

    fun refreshDefaultHomeStatus() = homeRoleRepository.refresh()

    fun openAssistant() = commandBus.send(UiCommand.OpenAssistant)
    fun showMessage(text: String) = commandBus.send(UiCommand.ShowMessage(text))

    // --- Launching ---

    fun launch(tile: HomeTile, bounds: Rect? = null) {
        when (tile) {
            is AppTile -> appRepository.launch(tile.entry.app.key, bounds)
            is ShortcutTile -> {
                val item = tile.item
                val ok = appRepository.startShortcut(item.shortcutPackage ?: return, item.shortcutId ?: return, item.userSerial)
                if (!ok) commandBus.send(UiCommand.ShowMessage("Shortcuts open only while SL Launcher Pro is your home app."))
            }
            else -> Unit
        }
    }

    // --- Editing ---

    fun addApp(key: AppKey, page: Int = 0) = viewModelScope.launch {
        val s = uiState.value
        when (layout.addApp(key, s.grid, s.pageCount, page)) {
            PlaceResult.AlreadyThere -> commandBus.send(UiCommand.ShowMessage("Already on your home screen."))
            else -> commandBus.send(UiCommand.ShowMessage("Added to home screen."))
        }
    }

    fun addToDock(key: AppKey) = viewModelScope.launch {
        when (layout.addToDock(key)) {
            PlaceResult.NoSpace -> commandBus.send(UiCommand.ShowMessage("The dock is full. Remove an app from it first."))
            PlaceResult.AlreadyThere -> commandBus.send(UiCommand.ShowMessage("Already in the dock."))
            PlaceResult.Placed -> Unit
        }
    }

    fun remove(tile: HomeTile) = viewModelScope.launch {
        if (tile is WidgetTile) widgets.deleteId(tile.item.widgetId)
        if (tile is FolderTile) {
            // Removing a folder keeps its apps: they return to the grid.
            tile.children.forEach { layout.removeFromFolder(it.item.id, uiState.value.grid, uiState.value.pageCount) }
        }
        layout.remove(tile.item.id)
    }

    fun move(itemId: Long, page: Int, cell: Int) = viewModelScope.launch {
        if (!layout.move(itemId, page, cell, uiState.value.grid)) {
            commandBus.send(UiCommand.ShowMessage("There isn't room there."))
        }
    }

    fun resizeWidget(itemId: Long, spanX: Int, spanY: Int) = viewModelScope.launch {
        if (!layout.resizeWidget(itemId, spanX, spanY, uiState.value.grid)) {
            commandBus.send(UiCommand.ShowMessage("Not enough free space to make it that size."))
        }
    }

    fun createFolder(firstId: Long, secondId: Long) = viewModelScope.launch { layout.createFolder(firstId, secondId, "Folder") }

    fun moveIntoFolder(itemId: Long, folderId: Long) = viewModelScope.launch { layout.moveIntoFolder(itemId, folderId) }

    fun removeFromFolder(itemId: Long) = viewModelScope.launch {
        layout.removeFromFolder(itemId, uiState.value.grid, uiState.value.pageCount)
    }

    fun renameFolder(folderId: Long, name: String) = viewModelScope.launch {
        layout.renameFolder(folderId, name.trim().ifEmpty { "Folder" })
    }

    fun addPage() = viewModelScope.launch {
        settingsRepository.update { it.copy(homePages = (uiState.value.pageCount + 1).coerceAtMost(9)) }
    }

    /** Removes the last page when it is empty. */
    fun removeLastPage() = viewModelScope.launch {
        val s = uiState.value
        if (s.pageCount > 1 && s.pages.last().isEmpty()) {
            settingsRepository.update { it.copy(homePages = s.pageCount - 1) }
        } else {
            commandBus.send(UiCommand.ShowMessage("Only an empty last page can be removed."))
        }
    }

    fun openAppInfo(key: AppKey) = appRepository.openAppInfo(key)

    fun uninstallIntent(key: AppKey): Intent = appRepository.uninstallIntent(key.packageName)

    fun shortcutsFor(key: AppKey) = appRepository.shortcutsFor(key)

    fun startShortcut(packageName: String, id: String, userSerial: Long) = appRepository.startShortcut(packageName, id, userSerial)

    fun pinShortcutToHome(packageName: String, id: String, userSerial: Long, label: String) = viewModelScope.launch {
        appRepository.pinShortcut(packageName, id, userSerial)
        val s = uiState.value
        layout.addShortcut(packageName, id, userSerial, label, s.grid, s.pageCount)
        commandBus.send(UiCommand.ShowMessage("Shortcut added to home screen."))
    }

    fun pinnedShortcut(tile: ShortcutTile) =
        appRepository.pinnedShortcut(tile.item.shortcutPackage.orEmpty(), tile.item.shortcutId.orEmpty(), tile.item.userSerial)

    // --- Background housekeeping ---

    /** On first run the dock gets the phone, messages, browser and camera apps the user already uses. */
    private fun seedDefaultDockOnFirstRun() = viewModelScope.launch {
        appRepository.loaded.first { it }
        if (!layout.isEmpty()) return@launch
        val apps = appRepository.apps.value
        val pm = context.packageManager
        val intents = listOf(
            Intent(Intent.ACTION_DIAL),
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MESSAGING),
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER),
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
        )
        val keys = intents.mapNotNull { intent ->
            @Suppress("DEPRECATION")
            val pkg = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
            apps.firstOrNull { it.key.packageName == pkg && !it.isWorkProfile }?.key
        }.distinct()
        layout.seedDock(keys)
    }

    /** Removes home items whose app was uninstalled. */
    private fun cleanUpUninstalledApps() = viewModelScope.launch {
        combine(appRepository.apps, appRepository.loaded, layout.items) { apps, loaded, items -> Triple(apps, loaded, items) }
            .collect { (apps, loaded, items) ->
                if (!loaded || apps.isEmpty()) return@collect
                val installed = apps.map { it.key.flatten() }.toSet()
                // Apps in a paused work profile are hidden by Android, not uninstalled: keep them.
                items.filter { it.type == HomeItemType.APP && it.appKey != null && it.appKey !in installed }
                    .mapNotNull { it.appKey }
                    .filter { key -> AppKey.parse(key)?.let { appRepository.isProfileAvailable(it.userSerial) } == true }
                    .distinct()
                    .forEach { layout.removeApp(it) }
            }
    }

    private fun reflowWhenGridShrinks() = viewModelScope.launch {
        settingsRepository.settings.map { GridSpec(it.homeColumns, it.homeRows) to it.homePages }
            .distinctUntilChanged()
            .collect { (grid, pages) ->
                val needed = layout.reflow(grid, pages)
                if (needed > pages) settingsRepository.update { it.copy(homePages = needed) }
            }
    }
}
