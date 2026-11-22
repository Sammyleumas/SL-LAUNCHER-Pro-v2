package com.sllauncher.pro.drawer

import android.content.Intent
import android.content.pm.ShortcutInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sllauncher.pro.data.apps.AppCategory
import com.sllauncher.pro.data.apps.AppEntry
import com.sllauncher.pro.data.apps.AppKey
import com.sllauncher.pro.data.apps.AppRepository
import com.sllauncher.pro.data.db.CategoryDao
import com.sllauncher.pro.data.db.CustomCategoryEntity
import com.sllauncher.pro.data.settings.DrawerSort
import com.sllauncher.pro.data.settings.LauncherSettings
import com.sllauncher.pro.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DrawerUiState(
    val apps: List<AppEntry> = emptyList(),
    val recents: List<AppEntry> = emptyList(),
    /** True when [recents] are time-of-day suggestions rather than recently opened apps. */
    val recentsAreSuggestions: Boolean = false,
    val filter: DrawerFilter = DrawerFilter.All,
    val query: String = "",
    val builtInCategories: List<AppCategory> = emptyList(),
    val customCategories: List<CustomCategoryEntity> = emptyList(),
    val hasHidden: Boolean = false,
    val installedCount: Int = 0,
    val settings: LauncherSettings = LauncherSettings(),
    val loading: Boolean = true,
)

@HiltViewModel
class DrawerViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val categoryDao: CategoryDao,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val filter = MutableStateFlow<DrawerFilter>(DrawerFilter.All)
    private val query = MutableStateFlow("")

    val uiState: StateFlow<DrawerUiState> = combine(
        combine(appRepository.entries, appRepository.launchLog) { e, log -> e to log },
        combine(filter, query) { f, q -> f to q },
        categoryDao.observeAll(),
        settingsRepository.settings,
        appRepository.loaded,
    ) { (entries, log), (f, q), customs, settings, loaded ->
        val hidden = settings.drawerHiddenCategories
        val showTop = q.isBlank() && f == DrawerFilter.All
        val suggested = if (showTop && settings.drawerShowSuggestions) {
            val byKey = entries.filter { !it.hidden }.associateBy { it.app.key.flatten() }
            suggestApps(log.map { it.appKey to it.at }, System.currentTimeMillis(), limit = 8).mapNotNull { byKey[it] }
        } else {
            emptyList()
        }
        DrawerUiState(
            apps = filterApps(entries, f, q, settings.drawerSort),
            recents = when {
                suggested.size >= 3 -> suggested
                showTop && settings.drawerShowRecents -> filterApps(entries, DrawerFilter.Recent, "", settings.drawerSort, recentLimit = 8)
                else -> emptyList()
            },
            recentsAreSuggestions = suggested.size >= 3,
            filter = f,
            query = q,
            builtInCategories = nonEmptyCategories(entries).filter { it.name !in hidden },
            customCategories = customs,
            hasHidden = entries.any { it.hidden },
            installedCount = entries.size,
            settings = settings,
            loading = !loaded,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DrawerUiState())

    fun setFilter(value: DrawerFilter) { filter.value = value }
    fun setQuery(value: String) { query.value = value }
    fun reset() { query.value = ""; filter.value = DrawerFilter.All }

    fun launch(entry: AppEntry) = appRepository.launch(entry.app.key)

    fun toggleFavorite(entry: AppEntry) = viewModelScope.launch {
        appRepository.updateMeta(entry.app.key) { it.copy(favorite = !entry.favorite) }
    }

    fun setHidden(entry: AppEntry, hidden: Boolean) = viewModelScope.launch {
        appRepository.updateMeta(entry.app.key) { it.copy(hidden = hidden, favorite = if (hidden) false else it.favorite) }
    }

    /** null = automatic detection. */
    fun setCategory(entry: AppEntry, override: String?) = viewModelScope.launch {
        appRepository.updateMeta(entry.app.key) { it.copy(categoryOverride = override) }
    }

    fun createCategory(name: String, thenAssign: AppEntry?) = viewModelScope.launch {
        val trimmed = name.trim().take(24)
        if (trimmed.isEmpty()) return@launch
        val id = categoryDao.insert(CustomCategoryEntity(name = trimmed, position = Int.MAX_VALUE / 2))
        if (thenAssign != null) appRepository.updateMeta(thenAssign.app.key) { it.copy(categoryOverride = "custom:$id") }
    }

    fun deleteCategory(category: CustomCategoryEntity) = viewModelScope.launch {
        categoryDao.delete(category)
        if ((filter.value as? DrawerFilter.Custom)?.id == category.id) filter.value = DrawerFilter.All
    }

    fun setListMode(list: Boolean) = viewModelScope.launch { settingsRepository.update { it.copy(drawerListMode = list) } }

    fun setSort(sort: DrawerSort) = viewModelScope.launch { settingsRepository.update { it.copy(drawerSort = sort) } }

    fun shortcuts(key: AppKey): List<ShortcutInfo> = appRepository.shortcutsFor(key)

    fun startShortcut(info: ShortcutInfo, key: AppKey) = appRepository.startShortcut(info.`package`, info.id, key.userSerial)

    fun openAppInfo(key: AppKey) = appRepository.openAppInfo(key)

    fun uninstallIntent(key: AppKey): Intent = appRepository.uninstallIntent(key.packageName)
}
