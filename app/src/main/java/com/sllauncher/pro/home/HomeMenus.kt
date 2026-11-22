package com.sllauncher.pro.home

import android.content.pm.ShortcutInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.sllauncher.pro.common.AppIcon
import com.sllauncher.pro.common.ShortcutIcon
import com.sllauncher.core.designsystem.component.SLBottomSheet
import com.sllauncher.core.designsystem.component.SLButton
import com.sllauncher.core.designsystem.component.SLButtonStyle
import com.sllauncher.core.designsystem.component.SLFloatingPanel
import com.sllauncher.core.designsystem.component.SLIconTile
import com.sllauncher.core.designsystem.component.SLNavigationRow
import com.sllauncher.core.designsystem.component.SLSectionHeader
import com.sllauncher.core.designsystem.component.SLSettingsRow
import com.sllauncher.core.designsystem.theme.SLTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Everything the item menu can do; the screen wires these to the ViewModel. */
class ItemMenuActions(
    val shortcuts: (HomeTile) -> List<ShortcutInfo>,
    val onShortcut: (ShortcutInfo) -> Unit,
    val onPinShortcut: (ShortcutInfo) -> Unit,
    val onAppInfo: (AppTile) -> Unit,
    val onUninstall: (AppTile) -> Unit,
    val onAddToDock: (AppTile) -> Unit,
    val onRemove: (HomeTile) -> Unit,
    val onMove: (HomeTile) -> Unit,
    val onResize: (WidgetTile, Int, Int) -> Unit,
    val onRename: (FolderTile) -> Unit,
)

/** Long-press menu for a home tile. Only actions Android allows for that item are shown. */
@Composable
fun ItemMenuSheet(tile: HomeTile, inDock: Boolean, grid: GridSpec, actions: ItemMenuActions, onDismiss: () -> Unit) {
    fun run(action: () -> Unit) { action(); onDismiss() }
    SLBottomSheet(onDismissRequest = onDismiss, title = tileLabel(tile).substringBefore(",")) {
        if (tile is AppTile) {
            val shortcuts by produceState(emptyList<ShortcutInfo>(), tile.item.id) {
                value = withContext(Dispatchers.IO) { actions.shortcuts(tile) }
            }
            if (shortcuts.isNotEmpty()) {
                SLSectionHeader("Shortcuts", Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                shortcuts.forEach { info ->
                    val label = info.shortLabel?.toString() ?: info.id
                    SLSettingsRow(
                        title = label,
                        onClick = { run { actions.onShortcut(info) } },
                        leading = { ShortcutIcon(info, label, 28.dp) },
                        trailing = {
                            SLButton(text = "Pin", onClick = { run { actions.onPinShortcut(info) } }, style = SLButtonStyle.Ghost)
                        },
                    )
                }
            }
            SLNavigationRow(title = "App info", onClick = { run { actions.onAppInfo(tile) } })
            if (!inDock) SLNavigationRow(title = "Add to dock", onClick = { run { actions.onAddToDock(tile) } })
            SLNavigationRow(title = "Uninstall", subtitle = "Android will ask you to confirm", onClick = { run { actions.onUninstall(tile) } })
        }
        if (tile is WidgetTile) {
            SLSectionHeader("Size", Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            val sizes = listOf(1 to 1, 2 to 1, 2 to 2, 3 to 2, grid.columns to 2, grid.columns to 3).filter { (x, y) -> x <= grid.columns && y <= grid.rows }.distinct()
            sizes.forEach { (x, y) ->
                SLSettingsRow(
                    title = "$x × $y",
                    subtitle = if (x == tile.item.spanX && y == tile.item.spanY) "Current size" else null,
                    onClick = { run { actions.onResize(tile, x, y) } },
                )
            }
        }
        if (tile is FolderTile) {
            SLNavigationRow(title = "Rename folder", onClick = { run { actions.onRename(tile) } })
        }
        if (!inDock) SLNavigationRow(title = "Move", subtitle = "Then tap where it should go", onClick = { run { actions.onMove(tile) } })
        SLNavigationRow(
            title = when {
                inDock -> "Remove from dock"
                tile is FolderTile -> "Remove folder (keeps the apps)"
                else -> "Remove from home screen"
            },
            onClick = { run { actions.onRemove(tile) } },
        )
    }
}

/** Long-press on empty home space. */
@Composable
fun HomeMenuSheet(
    onWallpaper: () -> Unit,
    onSlWallpapers: () -> Unit,
    onWidgets: () -> Unit,
    onAddPage: () -> Unit,
    onRemovePage: () -> Unit,
    onCustomize: () -> Unit,
    onSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    fun run(action: () -> Unit) { action(); onDismiss() }
    SLBottomSheet(onDismissRequest = onDismiss, title = "Home screen") {
        SLNavigationRow(title = "SL wallpapers", subtitle = "Soft wallpapers made for SL Pro", onClick = { run(onSlWallpapers) })
        SLNavigationRow(title = "Other wallpapers", subtitle = "Your photos and Android's wallpaper picker", onClick = { run(onWallpaper) })
        SLNavigationRow(title = "Widgets", onClick = { run(onWidgets) })
        SLNavigationRow(title = "Add page", onClick = { run(onAddPage) })
        SLNavigationRow(title = "Remove last page", subtitle = "Only when it is empty", onClick = { run(onRemovePage) })
        SLNavigationRow(title = "Customize", onClick = { run(onCustomize) })
        SLNavigationRow(title = "SL settings", onClick = { run(onSettings) })
    }
}

/** An open folder in a floating panel. Long-press an app to take it out. */
@Composable
fun FolderPanel(
    folder: FolderTile?,
    onDismiss: () -> Unit,
    onOpen: (AppTile) -> Unit,
    onRemoveFromFolder: (AppTile) -> Unit,
) {
    SLFloatingPanel(visible = folder != null, onDismissRequest = onDismiss, title = folder?.name ?: "Folder") {
        if (folder == null) return@SLFloatingPanel
        Text(
            folder.name,
            style = SLTheme.typography.headline,
            color = SLTheme.colors.textPrimary,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Text(
            "Long-press an app to move it out of the folder.",
            style = SLTheme.typography.caption,
            color = SLTheme.colors.textSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(76.dp),
            contentPadding = PaddingValues(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(folder.children, key = { it.item.id }) { child ->
                SLIconTile(
                    label = child.entry.app.label,
                    onClick = { onOpen(child) },
                    onLongClick = { onRemoveFromFolder(child) },
                    onLongClickLabel = "Move out of folder",
                ) { AppIcon(child.entry.app.key, child.entry.app.label, 52.dp) }
            }
        }
    }
}

/** Simple rename sheet for folders. */
@Composable
fun RenameFolderSheet(folder: FolderTile, onRename: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember(folder.item.id) { mutableStateOf(folder.name) }
    SLBottomSheet(onDismissRequest = onDismiss, title = "Rename folder") {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            com.sllauncher.core.designsystem.component.SLSearchField(
                query = name,
                onQueryChange = { name = it.take(30) },
                placeholder = "Folder name",
                onSearch = { onRename(name); onDismiss() },
            )
            SLButton(text = "Save", onClick = { onRename(name); onDismiss() }, modifier = Modifier.fillMaxWidth())
        }
    }
}
