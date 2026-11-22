package com.sllauncher.pro.notifications

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.sllauncher.pro.common.SystemIntents
import com.sllauncher.pro.common.startSafely
import com.sllauncher.pro.data.db.NotificationHistoryDao
import com.sllauncher.pro.data.db.NotificationHistoryEntity
import com.sllauncher.pro.data.settings.LauncherSettings
import com.sllauncher.pro.data.settings.SettingsRepository
import com.sllauncher.pro.ui.ScreenScaffold
import com.sllauncher.core.designsystem.component.SLBlock
import com.sllauncher.core.designsystem.component.SLButton
import com.sllauncher.core.designsystem.component.SLButtonStyle
import com.sllauncher.core.designsystem.component.SLChip
import com.sllauncher.core.designsystem.component.SLEmptyState
import com.sllauncher.core.designsystem.component.SLSearchField
import com.sllauncher.core.designsystem.component.SLSectionHeader
import com.sllauncher.core.designsystem.theme.SLTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationCenterState(
    val connected: Boolean = false,
    val groups: List<Pair<String, List<SLNotification>>> = emptyList(),
    val history: List<NotificationHistoryEntity> = emptyList(),
    val settings: LauncherSettings = LauncherSettings(),
)

@HiltViewModel
class NotificationCenterViewModel @Inject constructor(
    private val repository: NotificationRepository,
    private val historyDao: NotificationHistoryDao,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val access = MutableStateFlow(repository.hasAccess())

    val state: StateFlow<NotificationCenterState> = combine(
        repository.notifications,
        combine(repository.connected, access) { c, a -> c || a },
        historyDao.observeAll(),
        settingsRepository.settings,
    ) { list, connected, history, s ->
        NotificationCenterState(
            connected = connected,
            groups = list.filter { !it.isGroupSummary && it.packageName !in s.mutedNotificationPackages }
                .groupBy { it.appLabel }
                .toList()
                .sortedByDescending { (_, items) -> items.maxOf { it.postTime } },
            history = if (s.notificationHistoryEnabled) history else emptyList(),
            settings = s,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationCenterState())

    fun refreshAccess() { access.value = repository.hasAccess() }
    fun open(n: SLNotification) = repository.open(n)
    fun dismiss(n: SLNotification) = repository.dismiss(n.key)
    fun dismissGroup(items: List<SLNotification>) = items.filter { it.isClearable }.forEach { repository.dismiss(it.key) }
    fun dismissAll() = repository.dismissAll()
    fun action(a: SLNotificationAction) = repository.runAction(a)
    fun reply(n: SLNotification, text: String) = repository.reply(n, text)

    fun mute(packageName: String) = viewModelScope.launch {
        settingsRepository.update { it.copy(mutedNotificationPackages = it.mutedNotificationPackages + packageName) }
    }

    fun unmuteAll() = viewModelScope.launch { settingsRepository.update { it.copy(mutedNotificationPackages = emptySet()) } }

    fun setHideContent(hide: Boolean) = viewModelScope.launch { settingsRepository.update { it.copy(hideNotificationContent = hide) } }
}

/** SL's notification center. Reads only what Android shares with notification access; nothing is uploaded. */
@Composable
fun NotificationCenterRoute(onBack: () -> Unit, viewModel: NotificationCenterViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var tab by rememberSaveable { mutableStateOf(0) }

    LifecycleResumeEffect(Unit) {
        viewModel.refreshAccess()
        onPauseOrDispose { }
    }

    ScreenScaffold(
        title = "Notifications",
        onBack = onBack,
        actions = {
            if (state.connected && state.groups.isNotEmpty()) SLButton("Clear all", { viewModel.dismissAll() }, style = SLButtonStyle.Ghost)
        },
    ) {
        if (!state.connected) {
            AccessCard(onOpenSettings = { context.startSafely(SystemIntents.notificationListenerSettings()) })
            return@ScreenScaffold
        }
        LazyRow(contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item { SLChip("Now", tab == 0, { tab = 0 }) }
            item { SLChip("History", tab == 1, { tab = 1 }) }
            item { SLChip(if (state.settings.hideNotificationContent) "Show content" else "Hide content", false, { viewModel.setHideContent(!state.settings.hideNotificationContent) }) }
            if (state.settings.mutedNotificationPackages.isNotEmpty()) item { SLChip("Unmute ${state.settings.mutedNotificationPackages.size} apps", false, { viewModel.unmuteAll() }) }
        }
        if (tab == 1) {
            History(state)
            return@ScreenScaffold
        }
        if (state.groups.isEmpty()) {
            SLEmptyState(title = "You're all caught up", body = "New notifications will appear here and in the Dynamic Island.", modifier = Modifier.align(Alignment.CenterHorizontally))
            return@ScreenScaffold
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.groups.forEach { (app, items) ->
                item(key = "h:$app") {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        SLSectionHeader("$app · ${items.size}", Modifier.weight(1f))
                        SLButton("Mute", { viewModel.mute(items.first().packageName) }, style = SLButtonStyle.Ghost)
                        SLButton("Clear", { viewModel.dismissGroup(items) }, style = SLButtonStyle.Ghost)
                    }
                }
                items(items, key = { it.key }) { n -> NotificationCard(n, state.settings.hideNotificationContent, viewModel) }
            }
        }
    }
}

@Composable
private fun AccessCard(onOpenSettings: () -> Unit) {
    SLBlock(Modifier.padding(16.dp).fillMaxWidth(), shadow = true) {
        Text("Turn on notification access", style = SLTheme.typography.title, color = SLTheme.colors.textPrimary)
        Text(
            "SL needs Android's notification access to show your notifications here, power the Dynamic Island and music controls, and send quick replies. " +
                "Notifications are read on your phone only and never uploaded. You can turn it off at any time.",
            style = SLTheme.typography.bodySmall,
            color = SLTheme.colors.textSecondary,
        )
        Text("In the next screen, find SL Launcher Pro and switch it on.", style = SLTheme.typography.bodySmall, color = SLTheme.colors.textSecondary)
        SLButton("Open notification access", onOpenSettings, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun NotificationCard(n: SLNotification, hideContent: Boolean, viewModel: NotificationCenterViewModel) {
    var replying by remember(n.key) { mutableStateOf(false) }
    var draft by remember(n.key) { mutableStateOf("") }
    var status by remember(n.key) { mutableStateOf<String?>(null) }
    SLBlock(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(n.title.ifEmpty { n.appLabel }, style = SLTheme.typography.title, color = SLTheme.colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text(DateUtils.getRelativeTimeSpanString(n.postTime).toString(), style = SLTheme.typography.caption, color = SLTheme.colors.textSecondary)
        }
        Text(
            if (hideContent) "Content hidden" else n.text,
            style = SLTheme.typography.bodySmall,
            color = SLTheme.colors.textSecondary,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
        if (n.isProgress && n.progressMax > 0) {
            com.sllauncher.core.designsystem.component.SLProgressBar(n.progress.toFloat() / n.progressMax)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SLButton("Open", { viewModel.open(n) }, style = SLButtonStyle.Secondary)
            if (n.replyAction != null) SLButton("Reply", { replying = !replying }, style = SLButtonStyle.Secondary)
            if (n.isClearable) SLButton("Dismiss", { viewModel.dismiss(n) }, style = SLButtonStyle.Ghost)
        }
        val otherActions = n.actions.filter { it.remoteInput == null }
        if (otherActions.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                otherActions.take(3).forEach { a -> SLButton(a.title, { viewModel.action(a) }, style = SLButtonStyle.Ghost) }
            }
        }
        if (replying) {
            SLSearchField(query = draft, onQueryChange = { draft = it }, placeholder = "Reply", onSearch = {
                status = when (viewModel.reply(n, draft)) {
                    ReplyResult.Sent -> { draft = ""; replying = false; "Reply sent." }
                    ReplyResult.NotSupported -> "This app doesn't accept quick replies."
                    ReplyResult.Failed -> "Couldn't send. Open the app to reply."
                }
            })
        }
        status?.let { Text(it, style = SLTheme.typography.caption, color = SLTheme.colors.accentIndicator) }
    }
}

@Composable
private fun History(state: NotificationCenterState) {
    if (!state.settings.notificationHistoryEnabled) {
        SLEmptyState(
            title = "History is off",
            body = "Turn on notification history in Privacy to keep the last 7 days of notifications on this phone.",
        )
        return
    }
    if (state.history.isEmpty()) {
        SLEmptyState(title = "No history yet", body = "Notifications received from now on will be kept for 7 days on this phone.")
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(state.history, key = { it.id }) { h ->
            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text("${h.appLabel} · ${DateUtils.getRelativeTimeSpanString(h.postedAt)}", style = SLTheme.typography.caption, color = SLTheme.colors.textSecondary)
                Text(h.title, style = SLTheme.typography.body, color = SLTheme.colors.textPrimary)
                if (!state.settings.hideNotificationContent) Text(h.text, style = SLTheme.typography.bodySmall, color = SLTheme.colors.textSecondary, maxLines = 2)
            }
        }
    }
}
