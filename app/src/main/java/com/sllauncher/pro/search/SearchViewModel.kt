package com.sllauncher.pro.search

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sllauncher.pro.commands.ActionResult
import com.sllauncher.pro.commands.LauncherActionExecutor
import com.sllauncher.pro.commands.LauncherCommand
import com.sllauncher.pro.common.SystemIntents
import com.sllauncher.pro.common.UiCommand
import com.sllauncher.pro.common.UiCommandBus
import com.sllauncher.pro.common.startSafely
import com.sllauncher.pro.data.apps.AppEntry
import com.sllauncher.pro.data.apps.AppRepository
import com.sllauncher.pro.data.db.SearchHistoryDao
import com.sllauncher.pro.data.db.SearchHistoryEntity
import com.sllauncher.pro.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val filter: ResultType? = null,
    val results: List<SearchResult> = emptyList(),
    val history: List<SearchHistoryEntity> = emptyList(),
    val topApps: List<AppEntry> = emptyList(),
    val contactsEnabled: Boolean = false,
    val contactsPermission: Boolean = false,
    val musicEnabled: Boolean = false,
    val historyEnabled: Boolean = true,
    /** A command waiting for the user's confirmation. */
    val pendingConfirmation: LauncherCommand? = null,
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: SearchRepository,
    private val history: SearchHistoryDao,
    private val settingsRepository: SettingsRepository,
    private val apps: AppRepository,
    private val executor: LauncherActionExecutor,
    private val bus: UiCommandBus,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow<ResultType?>(null)
    private val pending = MutableStateFlow<LauncherCommand?>(null)

    private val results = combine(query.debounce(120), settingsRepository.settings) { q, s -> q to s }
        .mapLatest { (q, s) -> repository.search(q, s) }

    val uiState: StateFlow<SearchUiState> = combine(
        combine(query, filter, pending) { q, f, p -> Triple(q, f, p) },
        results,
        history.observeRecent(),
        settingsRepository.settings,
        apps.entries,
    ) { (q, f, p), r, h, s, entries ->
        SearchUiState(
            query = q,
            filter = f,
            results = if (q.isBlank()) emptyList() else r.filter { f == null || it.type == f || it.type == ResultType.Action },
            history = if (s.searchHistoryEnabled) h else emptyList(),
            topApps = entries.filter { !it.hidden && it.launchCount > 0 }.sortedByDescending { it.launchCount }.take(8),
            contactsEnabled = s.searchContacts,
            contactsPermission = repository.hasContactsPermission(),
            musicEnabled = s.searchMusic,
            historyEnabled = s.searchHistoryEnabled,
            pendingConfirmation = p,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun setQuery(value: String) { query.value = value }
    fun setFilter(value: ResultType?) { filter.value = value }
    fun reset(seed: String) { query.value = seed; filter.value = null; pending.value = null }

    /** Opens [result]. Returns true when search should close. */
    fun open(result: SearchResult): Boolean {
        recordHistory()
        when (result) {
            is SearchResult.App -> apps.launch(result.entry.app.key)
            is SearchResult.Action -> { execute(result.command, confirmed = false); return !result.command.needsConfirmation }
            is SearchResult.Contact -> context.startSafely(Intent(Intent.ACTION_VIEW, result.lookupUri))
            is SearchResult.Setting -> {
                if (result.route != null) bus.send(UiCommand.Navigate(result.route))
                result.panel?.let { viewModelScope.launch { executor.execute(LauncherCommand.OpenSettings(it)) } }
            }
            is SearchResult.Track -> bus.send(UiCommand.Navigate(com.sllauncher.pro.navigation.MusicDestination))
            is SearchResult.Article -> context.startSafely(SystemIntents.openUrl(result.article.url))
            is SearchResult.Web -> context.startSafely(SystemIntents.webSearch(result.query))
        }
        return true
    }

    fun call(contact: SearchResult.Contact) {
        contact.number?.let { context.startSafely(SystemIntents.dial(it)) }
    }

    fun execute(command: LauncherCommand, confirmed: Boolean) = viewModelScope.launch {
        when (val result = executor.execute(command, confirmed)) {
            is ActionResult.NeedsConfirmation -> pending.value = result.command
            is ActionResult.Success -> { pending.value = null; bus.send(UiCommand.ShowMessage(result.message)) }
            else -> { pending.value = null; bus.send(UiCommand.ShowMessage(result.message)) }
        }
    }

    fun cancelPending() { pending.value = null }

    fun setContactsEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.update { it.copy(searchContacts = enabled) } }
    fun setMusicEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.update { it.copy(searchMusic = enabled) } }

    fun removeHistory(item: SearchHistoryEntity) = viewModelScope.launch { history.delete(item.query) }
    fun clearHistory() = viewModelScope.launch { history.clear() }

    private fun recordHistory() {
        val q = query.value.trim()
        if (q.length < 2 || !settingsRepository.current.searchHistoryEnabled) return
        viewModelScope.launch { history.record(q, System.currentTimeMillis()) }
    }
}
