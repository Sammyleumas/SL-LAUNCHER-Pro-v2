package com.sllauncher.pro.home

import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import com.sllauncher.pro.data.settings.GestureAction
import com.sllauncher.pro.controlcenter.ControlCenterPanel
import com.sllauncher.pro.controlcenter.HomePanel
import com.sllauncher.pro.controlcenter.NotificationsPanel
import com.sllauncher.pro.controlcenter.panelForSwipeStart
import com.sllauncher.pro.gestures.SLAccessibilityService
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sllauncher.pro.R
import com.sllauncher.pro.common.DrawerView
import com.sllauncher.pro.common.SystemIntents
import com.sllauncher.pro.common.UiCommand
import com.sllauncher.pro.common.startSafely
import com.sllauncher.pro.drawer.AppDrawerOverlay
import com.sllauncher.pro.navigation.CustomizationDestination
import com.sllauncher.pro.navigation.NotificationCenterDestination
import com.sllauncher.pro.navigation.SettingsDestination
import com.sllauncher.pro.navigation.WidgetPickerDestination
import com.sllauncher.pro.search.SearchOverlay
import com.sllauncher.pro.widgets.WidgetHostManager
import com.sllauncher.core.designsystem.component.SLAssistantButton
import com.sllauncher.core.designsystem.component.SLAssistantState
import com.sllauncher.core.designsystem.component.SLBlock
import com.sllauncher.core.designsystem.component.SLButton
import com.sllauncher.core.designsystem.icon.SLIcons
import com.sllauncher.core.designsystem.theme.SLTheme
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.shape.CircleShape
import com.sllauncher.core.designsystem.component.softRaised
import com.sllauncher.core.designsystem.component.softStyle
import androidx.compose.ui.draw.clip

private enum class Overlay { None, Drawer, Search, Folder, ControlCenter, Notifications }

/**
 * The launcher's home screen.
 *
 * @param island Dynamic Island slot shown above the pages.
 * @param overview optional block under the clock (daily overview).
 * @param badgeFor unread count for an app package, from notifications.
 */
@Composable
fun HomeRoute(
    onOpenLauncherSetup: () -> Unit,
    onNavigate: (Any) -> Unit,
    widgetHost: WidgetHostManager,
    island: @Composable () -> Unit = {},
    overview: @Composable () -> Unit = {},
    badgeFor: (String) -> Int = { 0 },
    viewModel: HomeViewModel = hiltViewModel(),
    commands: kotlinx.coroutines.flow.Flow<UiCommand>? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var overlay by rememberSaveable { mutableStateOf(Overlay.None) }
    var drawerView by rememberSaveable { mutableStateOf(DrawerView.All) }
    var searchSeed by rememberSaveable { mutableStateOf("") }
    var showWallpapers by remember { mutableStateOf(false) }
    var openFolderId by rememberSaveable { mutableLongStateOf(-1L) }
    var menuTile by remember { mutableStateOf<HomeTile?>(null) }
    var menuInDock by remember { mutableStateOf(false) }
    var homeMenu by remember { mutableStateOf(false) }
    var movingItemId by rememberSaveable { mutableStateOf<Long?>(null) }
    var renameFolder by remember { mutableStateOf<FolderTile?>(null) }

    LifecycleResumeEffect(Unit) {
        viewModel.refreshDefaultHomeStatus()
        onPauseOrDispose { }
    }

    fun runGesture(action: GestureAction) {
        when (action) {
            GestureAction.None -> Unit
            GestureAction.SplitPanels, GestureAction.ControlCenter -> overlay = Overlay.ControlCenter
            GestureAction.NotificationsPanel -> overlay = Overlay.Notifications
            GestureAction.LockScreen -> if (!SLAccessibilityService.lockScreen()) {
                viewModel.showMessage(
                    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.P) {
                        "Double-tap to lock needs Android 9 or newer."
                    } else {
                        "Turn on \"SL Launcher Pro gestures\" in Accessibility to lock with a double-tap."
                    },
                )
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) SLAccessibilityService.openSettings(context)
            }
            GestureAction.Notifications -> if (!SLAccessibilityService.openNotificationShade()) onNavigate(NotificationCenterDestination)
            GestureAction.NotificationCenter -> onNavigate(NotificationCenterDestination)
            GestureAction.Search -> { searchSeed = ""; overlay = Overlay.Search }
            GestureAction.Assistant -> viewModel.openAssistant()
            GestureAction.Drawer -> { drawerView = DrawerView.All; overlay = Overlay.Drawer }
        }
    }

    // Commands from the assistant, search or notifications.
    LaunchedEffect(commands) {
        commands?.collect { command ->
            when (command) {
                is UiCommand.OpenDrawer -> { drawerView = command.view; overlay = Overlay.Drawer }
                is UiCommand.OpenSearch -> { searchSeed = command.query; overlay = Overlay.Search }
                UiCommand.CloseOverlays -> { overlay = Overlay.None; movingItemId = null }
                else -> Unit
            }
        }
    }

    // Widget long-press opens the same item menu as icons.
    LaunchedEffect(widgetHost, state.pages) {
        widgetHost.onWidgetLongPress = { widgetId ->
            state.pages.flatten().firstOrNull { it is WidgetTile && it.item.widgetId == widgetId }?.let {
                menuTile = it; menuInDock = false
            }
        }
    }

    // Back closes overlays and move mode; on the bare home screen it does nothing
    // when SL is the default home app (Android would otherwise restart the launcher).
    BackHandler(enabled = movingItemId != null) { movingItemId = null }
    BackHandler(enabled = state.isDefaultHome && overlay == Overlay.None && movingItemId == null) { }

    val tileActions = HomeTileActions(
        onOpen = { tile ->
            when (tile) {
                is FolderTile -> { openFolderId = tile.item.id; overlay = Overlay.Folder }
                else -> viewModel.launch(tile)
            }
        },
        onMenu = { tile ->
            menuTile = tile
            menuInDock = tile.item.container == com.sllauncher.pro.data.db.HomeContainer.DOCK
        },
        onDrop = { dragged, cell, onto ->
            when {
                onto is AppTile && dragged is AppTile -> viewModel.createFolder(onto.item.id, dragged.item.id)
                onto is FolderTile && dragged is AppTile -> viewModel.moveIntoFolder(dragged.item.id, onto.item.id)
                else -> viewModel.move(dragged.item.id, dragged.item.page, cell)
            }
        },
        badgeFor = { tile -> if (tile is AppTile) badgeFor(tile.entry.app.key.packageName) else 0 },
    )

    HomeScreen(
        state = state,
        tileActions = tileActions,
        island = island,
        overview = overview,
        movingItemId = movingItemId,
        onPlaceMoving = { page, cell ->
            movingItemId?.let { viewModel.move(it, page, cell) }
            movingItemId = null
        },
        onEmptyLongPress = { homeMenu = true },
        onEmptyDoubleTap = { runGesture(state.settings.gestureDoubleTap) },
        onSwipeUp = { drawerView = DrawerView.All; overlay = Overlay.Drawer },
        onSwipeDown = { startX, width ->
            val action = state.settings.gestureSwipeDown
            if (action == GestureAction.SplitPanels) {
                overlay = when (panelForSwipeStart(startX, width)) {
                    HomePanel.ControlCenter -> Overlay.ControlCenter
                    HomePanel.Notifications -> Overlay.Notifications
                }
            } else {
                runGesture(action)
            }
        },
        onOpenSearch = { searchSeed = ""; overlay = Overlay.Search },
        onOpenAssistant = viewModel::openAssistant,
        onOpenLauncherSetup = onOpenLauncherSetup,
    )

    AppDrawerOverlay(
        visible = overlay == Overlay.Drawer,
        initialView = drawerView,
        onDismiss = { overlay = Overlay.None },
        onAddToHome = { key -> viewModel.addApp(key) },
        onAddToDock = { key -> viewModel.addToDock(key) },
    )
    SearchOverlay(
        visible = overlay == Overlay.Search,
        initialQuery = searchSeed,
        onDismiss = { overlay = Overlay.None },
    )
    NotificationsPanel(
        visible = overlay == Overlay.Notifications,
        onDismiss = { overlay = Overlay.None },
        onOpenAll = { onNavigate(NotificationCenterDestination) },
    )
    ControlCenterPanel(
        visible = overlay == Overlay.ControlCenter,
        onDismiss = { overlay = Overlay.None },
        onOpenSlSettings = { onNavigate(com.sllauncher.pro.navigation.SettingsDestination) },
    )
    FolderPanel(
        folder = if (overlay == Overlay.Folder) state.folder(openFolderId) else null,
        onDismiss = { overlay = Overlay.None },
        onOpen = { viewModel.launch(it) },
        onRemoveFromFolder = { viewModel.removeFromFolder(it.item.id) },
    )

    menuTile?.let { tile ->
        ItemMenuSheet(
            tile = tile,
            inDock = menuInDock,
            grid = state.grid,
            actions = ItemMenuActions(
                shortcuts = { t -> if (t is AppTile) viewModel.shortcutsFor(t.entry.app.key) else emptyList() },
                onShortcut = { info -> viewModel.startShortcut(info.`package`, info.id, (tile as? AppTile)?.entry?.app?.key?.userSerial ?: 0) },
                onPinShortcut = { info ->
                    viewModel.pinShortcutToHome(info.`package`, info.id, (tile as? AppTile)?.entry?.app?.key?.userSerial ?: 0, info.shortLabel?.toString() ?: info.id)
                },
                onAppInfo = { viewModel.openAppInfo(it.entry.app.key) },
                onUninstall = { context.startSafely(viewModel.uninstallIntent(it.entry.app.key)) },
                onAddToDock = { viewModel.addToDock(it.entry.app.key) },
                onRemove = { viewModel.remove(it) },
                onMove = { movingItemId = it.item.id },
                onResize = { widget, x, y -> viewModel.resizeWidget(widget.item.id, x, y) },
                onRename = { renameFolder = it },
            ),
            onDismiss = { menuTile = null },
        )
    }
    renameFolder?.let { folder ->
        RenameFolderSheet(folder, onRename = { viewModel.renameFolder(folder.item.id, it) }, onDismiss = { renameFolder = null })
    }
    if (showWallpapers) {
        com.sllauncher.pro.settings.WallpaperGallerySheet(onDismiss = { showWallpapers = false })
    }
    if (homeMenu) {
        HomeMenuSheet(
            onWallpaper = { context.startSafely(SystemIntents.wallpaperPicker()) },
            onSlWallpapers = { showWallpapers = true },
            onWidgets = { onNavigate(WidgetPickerDestination) },
            onAddPage = { viewModel.addPage() },
            onRemovePage = { viewModel.removeLastPage() },
            onCustomize = { onNavigate(CustomizationDestination) },
            onSettings = { onNavigate(SettingsDestination) },
            onDismiss = { homeMenu = false },
        )
    }
}

@Composable
private fun HomeScreen(
    state: HomeUiState,
    tileActions: HomeTileActions,
    island: @Composable () -> Unit,
    overview: @Composable () -> Unit,
    movingItemId: Long?,
    onPlaceMoving: (page: Int, cell: Int) -> Unit,
    onEmptyLongPress: () -> Unit,
    onEmptyDoubleTap: () -> Unit,
    onSwipeUp: () -> Unit,
    onSwipeDown: (startX: Float, width: Float) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenAssistant: () -> Unit,
    onOpenLauncherSetup: () -> Unit,
) {
    val settings = state.settings
    val pagerState = rememberPagerState { state.pageCount }
    var dragTotal by remember { mutableStateOf(0f) }
    var dragStartX by remember { mutableStateOf(0f) }
    val currentSwipeUp by androidx.compose.runtime.rememberUpdatedState(onSwipeUp)
    val currentSwipeDown by androidx.compose.runtime.rememberUpdatedState(onSwipeDown)

    // The window theme draws the system wallpaper behind this screen.
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { offset -> dragTotal = 0f; dragStartX = offset.x },
                    onDragEnd = {
                        when {
                            dragTotal < -120f -> currentSwipeUp()
                            dragTotal > 160f -> currentSwipeDown(dragStartX, size.width.toFloat())
                        }
                    },
                ) { _, amount -> dragTotal += amount }
            },
    ) {
        island()
        if (movingItemId != null) {
            MoveBanner()
        }
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                if (page == 0) {
                    if (settings.homeShowClock) ClockBlock(now = state.now, modifier = Modifier.padding(top = 8.dp, start = 8.dp))
                    if (settings.homeShowOverview) overview()
                    if (!state.isDefaultHome && state.ready) {
                        NotDefaultBanner(onOpenLauncherSetup, Modifier.padding(8.dp).fillMaxWidth())
                    }
                }
                HomePageGrid(
                    tiles = state.pages.getOrElse(page) { emptyList() },
                    grid = state.grid,
                    showLabels = settings.homeShowLabels,
                    iconScale = settings.homeIconScale,
                    actions = tileActions,
                    movingItemId = movingItemId,
                    onPlaceMoving = { cell -> onPlaceMoving(page, cell) },
                    onEmptyLongPress = onEmptyLongPress,
                    onEmptyDoubleTap = onEmptyDoubleTap,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        PageIndicator(state.pageCount, pagerState.currentPage, Modifier.align(Alignment.CenterHorizontally).padding(vertical = 6.dp))
        if (settings.homeShowSearchBar) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HomeSearchBar(onClick = onOpenSearch, modifier = Modifier.weight(1f))
                SLAssistantButton(state = SLAssistantState.Idle, onClick = onOpenAssistant, modifier = Modifier.size(48.dp))
            }
        }
        if (!settings.homeShowSearchBar) {
            SLAssistantButton(state = SLAssistantState.Idle, onClick = onOpenAssistant, modifier = Modifier.align(Alignment.End).padding(end = 16.dp).size(48.dp))
        }
        if (settings.dockEnabled) {
            Dock(state.dock, settings.homeIconScale, tileActions, Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
        }
    }
}

@Composable
private fun MoveBanner() {
    Text(
        "Tap a highlighted space to move the item. Press Back to cancel.",
        style = SLTheme.typography.bodySmall,
        color = SLTheme.colors.onAccent,
        modifier = Modifier.fillMaxWidth().background(SLTheme.colors.accent).padding(12.dp),
    )
}

/** Tapping opens unified search; the bar is a button, not a text field, so the keyboard does not pop up on home. */
@Composable
private fun HomeSearchBar(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .softRaised(CircleShape, softStyle(SLTheme.colors.surface.copy(alpha = 0.94f)))
            .border(SLTheme.shapes.borderWidth, SLTheme.colors.borderStrong, CircleShape)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = "Search", onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(SLIcons.Search, contentDescription = null, tint = SLTheme.colors.textSecondary, modifier = Modifier.size(20.dp))
        Text("Search apps, contacts, settings", style = SLTheme.typography.body, color = SLTheme.colors.textSecondary)
    }
}

@Composable
fun ClockBlock(now: ZonedDateTime, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // Observable: recomposes when the user changes the system language.
    val locale = LocalLocale.current.platformLocale
    val is24Hour = DateFormat.is24HourFormat(context)
    val timeFormatter = remember(locale, is24Hour) {
        DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm", locale)
    }
    val dateFormatter = remember(locale) {
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM"), locale)
    }
    Column(modifier) {
        Text(
            text = now.format(timeFormatter),
            style = SLTheme.typography.display.copy(shadow = WallpaperTextShadow),
            color = Color(0xFFF5F1E8),
            modifier = Modifier.semantics { heading() },
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(width = 16.dp, height = 4.dp).background(SLTheme.colors.accentIndicator))
            Spacer(Modifier.size(SLTheme.spacing.sm))
            Text(
                text = now.format(dateFormatter).uppercase(locale),
                style = SLTheme.typography.label.copy(shadow = WallpaperTextShadow),
                color = Color(0xFFF5F1E8),
            )
        }
    }
}

@Composable
private fun NotDefaultBanner(onSetUp: () -> Unit, modifier: Modifier = Modifier) {
    SLBlock(modifier = modifier, shadow = true) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SLTheme.spacing.md)) {
            Text(
                text = stringResource(R.string.home_not_default_title),
                style = SLTheme.typography.body,
                color = SLTheme.colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            SLButton(text = stringResource(R.string.home_not_default_action), onClick = onSetUp)
        }
    }
}
