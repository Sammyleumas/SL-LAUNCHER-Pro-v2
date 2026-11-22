package com.sllauncher.pro.search

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sllauncher.pro.common.AppIcon
import com.sllauncher.core.designsystem.component.SLButton
import com.sllauncher.core.designsystem.component.SLButtonStyle
import com.sllauncher.core.designsystem.component.SLChip
import com.sllauncher.core.designsystem.component.SLDialog
import com.sllauncher.core.designsystem.component.SLFloatingPanel
import com.sllauncher.core.designsystem.component.SLIconButton
import com.sllauncher.core.designsystem.component.SLIconButtonStyle
import com.sllauncher.core.designsystem.component.SLIconTile
import com.sllauncher.core.designsystem.component.SLSearchField
import com.sllauncher.core.designsystem.component.SLSectionHeader
import com.sllauncher.core.designsystem.component.SLSwitchRow
import com.sllauncher.core.designsystem.icon.SLIcons
import com.sllauncher.core.designsystem.theme.SLTheme

/** Unified search in a floating panel. Screen navigation from results goes through [com.sllauncher.pro.common.UiCommandBus]. */
@Composable
fun SearchOverlay(
    visible: Boolean,
    initialQuery: String,
    onDismiss: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(visible) {
        if (visible) {
            viewModel.reset(initialQuery)
            runCatching { focus.requestFocus() }
            keyboard?.show()
        }
    }

    val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let(viewModel::setQuery)
        }
    }
    val contactsPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.setContactsEnabled(true)
    }
    val musicPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.setMusicEnabled(true)
    }

    fun open(result: SearchResult) {
        if (viewModel.open(result)) {
            keyboard?.hide()
            onDismiss()
        }
    }

    SLFloatingPanel(visible = visible, onDismissRequest = { keyboard?.hide(); onDismiss() }, title = "Search") {
        SLSearchField(
            query = state.query,
            onQueryChange = viewModel::setQuery,
            placeholder = "Apps, contacts, settings, or a command",
            onSearch = { state.results.firstOrNull()?.let(::open) },
            modifier = Modifier.padding(horizontal = 12.dp).focusRequester(focus),
            trailing = {
                SLIconButton(SLIcons.Mic, "Voice search", onClick = {
                    try {
                        voice.launch(
                            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                .putExtra(RecognizerIntent.EXTRA_PROMPT, "Search or say a command"),
                        )
                    } catch (e: ActivityNotFoundException) {
                        viewModel.setQuery("")
                    }
                }, style = SLIconButtonStyle.Plain)
            },
        )

        if (state.query.isNotBlank()) {
            LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item { SLChip("All", state.filter == null, { viewModel.setFilter(null) }) }
                items(ResultType.entries.filter { it != ResultType.Action && it != ResultType.Web }) { type ->
                    SLChip(type.label, state.filter == type, { viewModel.setFilter(type) })
                }
            }
        }

        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(bottom = 16.dp)) {
            if (state.query.isBlank()) {
                if (state.history.isNotEmpty()) {
                    item { SLSectionHeader("Recent searches", Modifier.padding(16.dp, 12.dp)) }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(state.history, key = { it.id }) { h -> SLChip(h.query, false, { viewModel.setQuery(h.query) }) }
                            item { SLButton("Clear", { viewModel.clearHistory() }, style = SLButtonStyle.Ghost) }
                        }
                    }
                }
                if (state.topApps.isNotEmpty()) {
                    item { SLSectionHeader("Most used", Modifier.padding(16.dp, 12.dp)) }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = 8.dp)) {
                            items(state.topApps, key = { it.app.key.flatten() }) { entry ->
                                SLIconTile(label = entry.app.label, onClick = { open(SearchResult.App(entry)) }, iconSize = 44.dp) {
                                    AppIcon(entry.app.key, entry.app.label, 44.dp)
                                }
                            }
                        }
                    }
                }
                item { SLSectionHeader("Try a command", Modifier.padding(16.dp, 12.dp)) }
                items(listOf("Set a timer for 10 minutes", "Open Wi-Fi", "Show my favourite apps", "Note: call mum at 6")) { example ->
                    ResultRow(example, "Tap to try", icon = null, onClick = { viewModel.setQuery(example) })
                }
                item { SLSectionHeader("Search sources", Modifier.padding(16.dp, 12.dp)) }
                item {
                    SLSwitchRow(
                        title = "Contacts",
                        subtitle = "Android will ask for contacts access the first time",
                        checked = state.contactsEnabled,
                        onCheckedChange = { on ->
                            if (on && !state.contactsPermission) contactsPermission.launch(Manifest.permission.READ_CONTACTS) else viewModel.setContactsEnabled(on)
                        },
                    )
                }
                item {
                    SLSwitchRow(
                        title = "Music on this phone",
                        subtitle = "Android will ask for audio access the first time",
                        checked = state.musicEnabled,
                        onCheckedChange = { on ->
                            if (on) {
                                musicPermission.launch(
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE,
                                )
                            } else {
                                viewModel.setMusicEnabled(false)
                            }
                        },
                    )
                }
            } else {
                state.results.groupBy { it.type }.forEach { (type, group) ->
                    item(key = "h:${type.name}") { SLSectionHeader(type.label, Modifier.padding(16.dp, 12.dp)) }
                    items(group, key = { it.key }) { result ->
                        when (result) {
                            is SearchResult.App -> ResultRow(result.title, result.subtitle, icon = {
                                AppIcon(result.entry.app.key, result.title, 36.dp)
                            }, onClick = { open(result) })
                            is SearchResult.Contact -> ResultRow(result.title, result.subtitle, icon = null, onClick = { open(result) }) {
                                if (result.number != null) SLIconButton(SLIcons.ChevronRight, "Call ${result.title}", { viewModel.call(result) }, style = SLIconButtonStyle.Plain)
                            }
                            else -> ResultRow(result.title, result.subtitle, icon = null, highlight = result.type == ResultType.Action, onClick = { open(result) })
                        }
                    }
                }
            }
        }
    }

    state.pendingConfirmation?.let { command ->
        SLDialog(
            title = "Confirm",
            text = "${command.description}? You can change it back in Customize.",
            confirmLabel = "Apply",
            dismissLabel = "Cancel",
            onConfirm = { viewModel.execute(command, confirmed = true); onDismiss() },
            onDismissRequest = { viewModel.cancelPending() },
        )
    }
}

@Composable
private fun ResultRow(
    title: String,
    subtitle: String?,
    icon: (@Composable () -> Unit)?,
    onClick: () -> Unit,
    highlight: Boolean = false,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) {
            icon()
        } else {
            Box(
                Modifier.size(36.dp).background(if (highlight) SLTheme.colors.accent else SLTheme.colors.surfaceElevated),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (highlight) SLIcons.ChevronRight else SLIcons.Search,
                    contentDescription = null,
                    tint = if (highlight) SLTheme.colors.onAccent else SLTheme.colors.textSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = SLTheme.typography.body, color = SLTheme.colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = SLTheme.typography.caption, color = SLTheme.colors.textSecondary, maxLines = 1)
        }
        trailing()
    }
}
