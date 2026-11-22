package com.sllauncher.pro.drawer

import android.content.pm.ShortcutInfo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sllauncher.pro.common.AppIcon
import com.sllauncher.pro.common.DrawerView
import com.sllauncher.pro.common.ShortcutIcon
import com.sllauncher.pro.common.startSafely
import com.sllauncher.pro.data.apps.AppCategory
import com.sllauncher.pro.data.apps.AppEntry
import com.sllauncher.pro.data.apps.AppKey
import com.sllauncher.pro.data.settings.DrawerSort
import com.sllauncher.core.designsystem.component.SLBottomSheet
import com.sllauncher.core.designsystem.component.SLButton
import com.sllauncher.core.designsystem.component.SLChip
import com.sllauncher.core.designsystem.component.SLEmptyState
import com.sllauncher.core.designsystem.component.SLFloatingPanel
import com.sllauncher.core.designsystem.component.SLIconTile
import com.sllauncher.core.designsystem.component.SLLoadingIndicator
import com.sllauncher.core.designsystem.component.SLNavBar
import com.sllauncher.core.designsystem.component.SLNavItem
import com.sllauncher.core.designsystem.component.SLNavigationRow
import com.sllauncher.core.designsystem.component.SLSearchField
import com.sllauncher.core.designsystem.component.SLSectionHeader
import com.sllauncher.core.designsystem.component.SLSegment
import com.sllauncher.core.designsystem.component.SLSegmentedControl
import com.sllauncher.core.designsystem.component.SLSettingsRow
import com.sllauncher.core.designsystem.icon.SLIcons
import com.sllauncher.core.designsystem.theme.SLTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun categoryLabel(category: AppCategory): String = when (category) {
    AppCategory.Social -> "Social"
    AppCategory.Media -> "Media"
    AppCategory.Tools -> "Tools"
    AppCategory.Games -> "Games"
    AppCategory.Productivity -> "Productivity"
    AppCategory.Education -> "Education"
    AppCategory.Finance -> "Finance"
    AppCategory.Other -> "Other"
}

/**
 * The Grid Bold floating app drawer: search, category chips, grid/list
 * toggle, alphabet index, recents and a bottom bar, in an SL floating panel.
 */
@Composable
fun AppDrawerOverlay(
    visible: Boolean,
    initialView: DrawerView,
    onDismiss: () -> Unit,
    onAddToHome: (AppKey) -> Unit,
    onAddToDock: (AppKey) -> Unit,
    viewModel: DrawerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var menuEntry by remember { mutableStateOf<AppEntry?>(null) }
    var categoryEntry by remember { mutableStateOf<AppEntry?>(null) }
    var newCategoryFor by remember { mutableStateOf<AppEntry?>(null) }
    var showNewCategory by remember { mutableStateOf(false) }

    LaunchedEffect(visible, initialView) {
        if (visible) {
            viewModel.reset()
            viewModel.setFilter(
                when (initialView) {
                    DrawerView.All -> DrawerFilter.All
                    DrawerView.Favorites -> DrawerFilter.Favorites
                    DrawerView.Recent -> DrawerFilter.Recent
                },
            )
        }
    }

    fun open(entry: AppEntry) {
        viewModel.launch(entry)
        onDismiss()
    }

    SLFloatingPanel(
        visible = visible,
        onDismissRequest = onDismiss,
        title = "App drawer, ${state.installedCount} apps",
        surfaceAlpha = if (state.settings.lowPerformanceMode) 1f else state.settings.drawerPanelAlpha,
        widthFraction = state.settings.drawerWidthFraction,
        heightFraction = state.settings.drawerHeightFraction,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SLSearchField(
                query = state.query,
                onQueryChange = viewModel::setQuery,
                placeholder = "Search ${state.installedCount} apps",
                onSearch = { state.apps.firstOrNull()?.let(::open) },
                modifier = Modifier.weight(1f),
            )
            SLSegmentedControl(
                options = listOf(SLSegment("Grid", SLIcons.Grid, showLabel = false), SLSegment("List", SLIcons.List, showLabel = false)),
                selectedIndex = if (state.settings.drawerListMode) 1 else 0,
                onSelect = { viewModel.setListMode(it == 1) },
            )
        }
        CategoryChips(state, viewModel, onNewCategory = { newCategoryFor = null; showNewCategory = true })

        Text(
            text = when {
                state.query.isNotBlank() -> "${state.apps.size} RESULTS"
                else -> "${state.apps.size} APPS"
            },
            style = SLTheme.typography.label,
            color = SLTheme.colors.textSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.loading -> SLLoadingIndicator(Modifier.align(Alignment.Center))
                state.apps.isEmpty() -> SLEmptyState(
                    title = if (state.query.isNotBlank()) "No apps match \"${state.query}\"" else emptyTitle(state.filter),
                    body = emptyBody(state.filter),
                    modifier = Modifier.align(Alignment.Center),
                )
                else -> AppGridWithIndex(
                    state = state,
                    onOpen = ::open,
                    onMenu = { menuEntry = it },
                )
            }
        }

        SLNavBar(
            items = listOf(
                SLNavItem("Apps", SLIcons.Grid),
                SLNavItem("Favourites", SLIcons.Check),
                SLNavItem("Recent", SLIcons.List),
                SLNavItem("Sort", SLIcons.ChevronRight),
            ),
            selectedIndex = when (state.filter) {
                DrawerFilter.Favorites -> 1
                DrawerFilter.Recent -> 2
                else -> 0
            },
            onSelect = { index ->
                when (index) {
                    0 -> viewModel.setFilter(DrawerFilter.All)
                    1 -> viewModel.setFilter(DrawerFilter.Favorites)
                    2 -> viewModel.setFilter(DrawerFilter.Recent)
                    3 -> viewModel.setSort(DrawerSort.entries[(state.settings.drawerSort.ordinal + 1) % DrawerSort.entries.size])
                }
            },
        )
    }

    menuEntry?.let { entry ->
        AppMenuSheet(
            entry = entry,
            shortcuts = { viewModel.shortcuts(entry.app.key) },
            onShortcut = { viewModel.startShortcut(it, entry.app.key); onDismiss() },
            onAddToHome = { onAddToHome(entry.app.key) },
            onAddToDock = { onAddToDock(entry.app.key) },
            onFavorite = { viewModel.toggleFavorite(entry) },
            onHide = { viewModel.setHidden(entry, !entry.hidden) },
            onCategory = { categoryEntry = entry },
            onAppInfo = { viewModel.openAppInfo(entry.app.key) },
            onUninstall = { context.startSafely(viewModel.uninstallIntent(entry.app.key)) },
            onDismiss = { menuEntry = null },
        )
    }
    categoryEntry?.let { entry ->
        CategorySheet(
            entry = entry,
            customs = state.customCategories.map { it.id to it.name },
            onPick = { viewModel.setCategory(entry, it) },
            onNew = { newCategoryFor = entry; showNewCategory = true },
            onDismiss = { categoryEntry = null },
        )
    }
    if (showNewCategory) {
        NewCategorySheet(
            onCreate = { name -> viewModel.createCategory(name, newCategoryFor) },
            onDismiss = { showNewCategory = false },
        )
    }
}

private fun emptyTitle(filter: DrawerFilter) = when (filter) {
    DrawerFilter.Favorites -> "No favourites yet"
    DrawerFilter.Recent -> "Nothing opened yet"
    DrawerFilter.Hidden -> "No hidden apps"
    is DrawerFilter.Custom -> "This category is empty"
    else -> "No apps here"
}

private fun emptyBody(filter: DrawerFilter) = when (filter) {
    DrawerFilter.Favorites -> "Long-press any app and choose Add to favourites."
    DrawerFilter.Recent -> "Apps you open from SL will appear here."
    DrawerFilter.Hidden -> "Hidden apps are removed from the drawer only. They stay installed and visible in Android Settings."
    is DrawerFilter.Custom -> "Long-press an app and choose Move to category."
    else -> "Try another category."
}

@Composable
private fun CategoryChips(state: DrawerUiState, viewModel: DrawerViewModel, onNewCategory: () -> Unit) {
    data class Chip(val label: String, val filter: DrawerFilter?)
    val chips = buildList {
        add(Chip("All", DrawerFilter.All))
        state.builtInCategories.forEach { add(Chip(categoryLabel(it), DrawerFilter.Category(it))) }
        state.customCategories.forEach { add(Chip(it.name, DrawerFilter.Custom(it.id, it.name))) }
        if (state.hasHidden) add(Chip("Hidden", DrawerFilter.Hidden))
        add(Chip("+ New", null))
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(top = 4.dp),
    ) {
        items(chips, key = { it.label + it.filter.toString() }) { chip ->
            SLChip(
                label = chip.label,
                selected = chip.filter != null && chip.filter == state.filter,
                onClick = { if (chip.filter == null) onNewCategory() else viewModel.setFilter(chip.filter) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppGridWithIndex(state: DrawerUiState, onOpen: (AppEntry) -> Unit, onMenu: (AppEntry) -> Unit) {
    val settings = state.settings
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val showIndex = settings.drawerShowAlphabetIndex && state.query.isBlank() && settings.drawerSort == DrawerSort.Name && state.apps.size > 20
    val headerCount = if (state.recents.isNotEmpty()) 2 else 0

    Row(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()) {
            // Fit the user's column choice to the panel: tiles never get narrower than 64dp.
            val maxColumns = (maxWidth / 64.dp).toInt().coerceAtLeast(3)
            val columns = if (settings.drawerListMode) 1 else settings.drawerColumns.coerceIn(3, minOf(7, maxColumns))
            val iconSize = (48.dp * settings.drawerIconScale).coerceIn(36.dp, 64.dp)
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                state = gridState,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (state.recents.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "recent-header") {
                        SLSectionHeader(if (state.recentsAreSuggestions) "Suggested for now" else "Recent", Modifier.padding(8.dp))
                    }
                    item(span = { GridItemSpan(maxLineSpan) }, key = "recent-row") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(state.recents, key = { "r:" + it.app.key.flatten() }) { entry ->
                                SLIconTile(label = entry.app.label, onClick = { onOpen(entry) }, onLongClick = { onMenu(entry) }, iconSize = 40.dp) {
                                    AppIcon(entry.app.key, entry.app.label, 40.dp)
                                }
                            }
                        }
                    }
                }
                items(state.apps, key = { it.app.key.flatten() }) { entry ->
                    if (settings.drawerListMode) {
                        AppListRow(entry, onOpen, onMenu)
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            SLIconTile(
                                label = entry.app.label,
                                onClick = { onOpen(entry) },
                                onLongClick = { onMenu(entry) },
                                onLongClickLabel = "App options",
                                iconSize = iconSize,
                                showLabel = settings.drawerShowLabels,
                            ) { AppIcon(entry.app.key, entry.app.label, iconSize) }
                        }
                    }
                }
            }
        }
        if (showIndex) {
            val letters = remember(state.apps) { state.apps.map { indexLetter(it.app.label) }.distinct() }
            Column(
                Modifier.width(24.dp).fillMaxHeight().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                letters.forEach { letter ->
                    Text(
                        letter.toString(),
                        style = SLTheme.typography.caption.copy(fontSize = 10.sp),
                        color = SLTheme.colors.textSecondary,
                        modifier = Modifier
                            .clickable(onClickLabel = "Jump to $letter") {
                                val index = state.apps.indexOfFirst { indexLetter(it.app.label) == letter }
                                if (index >= 0) scope.launch { gridState.scrollToItem(index + headerCount) }
                            }
                            .padding(horizontal = 4.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppListRow(entry: AppEntry, onOpen: (AppEntry) -> Unit, onMenu: (AppEntry) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .combinedClickable(onClick = { onOpen(entry) }, onLongClick = { onMenu(entry) }, onLongClickLabel = "App options", role = Role.Button)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppIcon(entry.app.key, entry.app.label, 36.dp)
        Text(entry.app.label, style = SLTheme.typography.body, color = SLTheme.colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (entry.favorite) Text("★", color = SLTheme.colors.accentIndicator, modifier = Modifier.semantics { contentDescription = "Favourite" })
        if (entry.app.isWorkProfile) Text("WORK", style = SLTheme.typography.caption, color = SLTheme.colors.textSecondary)
    }
}

/** Long-press menu for an app in the drawer or search results. */
@Composable
fun AppMenuSheet(
    entry: AppEntry,
    shortcuts: () -> List<ShortcutInfo>,
    onShortcut: (ShortcutInfo) -> Unit,
    onAddToHome: () -> Unit,
    onAddToDock: () -> Unit,
    onFavorite: () -> Unit,
    onHide: () -> Unit,
    onCategory: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
    onDismiss: () -> Unit,
) {
    fun run(action: () -> Unit) { action(); onDismiss() }
    val list by produceState(emptyList<ShortcutInfo>(), entry.app.key) { value = withContext(Dispatchers.IO) { shortcuts() } }
    SLBottomSheet(onDismissRequest = onDismiss, title = entry.app.label) {
        list.forEach { info ->
            val label = info.shortLabel?.toString() ?: info.id
            SLSettingsRow(title = label, onClick = { run { onShortcut(info) } }, leading = { ShortcutIcon(info, label, 28.dp) })
        }
        SLNavigationRow(title = "Add to home screen", onClick = { run(onAddToHome) })
        SLNavigationRow(title = "Add to dock", onClick = { run(onAddToDock) })
        SLNavigationRow(title = if (entry.favorite) "Remove from favourites" else "Add to favourites", onClick = { run(onFavorite) })
        SLNavigationRow(title = "Move to category", value = categoryLabel(entry.category), onClick = { run(onCategory) })
        SLNavigationRow(
            title = if (entry.hidden) "Show in drawer" else "Hide from drawer",
            subtitle = if (entry.hidden) null else "Launcher-level only. The app stays installed and visible in Android Settings.",
            onClick = { run(onHide) },
        )
        SLNavigationRow(title = "App info", onClick = { run(onAppInfo) })
        SLNavigationRow(title = "Uninstall", subtitle = "Android will ask you to confirm", onClick = { run(onUninstall) })
    }
}

@Composable
private fun CategorySheet(
    entry: AppEntry,
    customs: List<Pair<Long, String>>,
    onPick: (String?) -> Unit,
    onNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    fun pick(value: String?) { onPick(value); onDismiss() }
    SLBottomSheet(onDismissRequest = onDismiss, title = "Category for ${entry.app.label}") {
        Row(
            Modifier
                .fillMaxWidth()
                .selectable(selected = false, role = Role.RadioButton) { pick(null) }
                .padding(16.dp),
        ) { Text("Automatic (${categoryLabel(entry.app.autoCategory)})", style = SLTheme.typography.body, color = SLTheme.colors.textPrimary) }
        AppCategory.entries.filter { it != AppCategory.Other }.forEach { category ->
            SLSettingsRow(title = categoryLabel(category), onClick = { pick("builtin:${category.name}") })
        }
        customs.forEach { (id, name) -> SLSettingsRow(title = name, subtitle = "Custom", onClick = { pick("custom:$id") }) }
        SLNavigationRow(title = "New category…", onClick = { onNew(); onDismiss() })
    }
}

@Composable
private fun NewCategorySheet(onCreate: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    SLBottomSheet(onDismissRequest = onDismiss, title = "New category") {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SLSearchField(query = name, onQueryChange = { name = it.take(24) }, placeholder = "Category name, e.g. Work")
            SLButton(
                text = "Create",
                enabled = name.isNotBlank(),
                onClick = { onCreate(name); onDismiss() },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
