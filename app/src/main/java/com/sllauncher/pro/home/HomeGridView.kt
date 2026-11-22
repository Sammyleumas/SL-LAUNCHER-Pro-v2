package com.sllauncher.pro.home

import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.sllauncher.pro.common.AppIcon
import com.sllauncher.pro.common.ShortcutIcon
import com.sllauncher.pro.common.rememberAppRepository
import com.sllauncher.pro.common.rememberWidgetHost
import com.sllauncher.core.designsystem.component.SLBadge
import com.sllauncher.core.designsystem.theme.SLTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** Text shadow that keeps labels readable over bright wallpapers. */
val WallpaperTextShadow = Shadow(color = Color(0x99000000), offset = Offset(0f, 2f), blurRadius = 6f)

/** Callbacks shared by home pages and the dock. */
class HomeTileActions(
    val onOpen: (HomeTile) -> Unit,
    val onMenu: (HomeTile) -> Unit,
    /** Dropped [dragged] at [cell] on the current page; [onto] is the tile under the drop point, if any. */
    val onDrop: (dragged: HomeTile, cell: Int, onto: HomeTile?) -> Unit,
    val badgeFor: (HomeTile) -> Int = { 0 },
)

/**
 * One home page: tiles placed absolutely on a [GridSpec]. When [movingItemId]
 * is set (accessible "Move" mode) every cell becomes a tap target.
 */
@Composable
fun HomePageGrid(
    tiles: List<HomeTile>,
    grid: GridSpec,
    showLabels: Boolean,
    iconScale: Float,
    actions: HomeTileActions,
    movingItemId: Long?,
    onPlaceMoving: (cell: Int) -> Unit,
    onEmptyLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures(onLongPress = { onEmptyLongPress() }) },
    ) {
        val cellW = maxWidth / grid.columns
        val cellH = maxHeight / grid.rows
        val cellWpx = with(density) { cellW.toPx() }
        val cellHpx = with(density) { cellH.toPx() }

        if (movingItemId != null) {
            for (cell in 0 until grid.cellCount) {
                Box(
                    Modifier
                        .offset(cellW * (cell % grid.columns), cellH * (cell / grid.columns))
                        .size(cellW, cellH)
                        .padding(4.dp)
                        .border(1.dp, SLTheme.colors.accentIndicator.copy(alpha = 0.6f))
                        .clickable(onClickLabel = "Place here") { onPlaceMoving(cell) },
                )
            }
        }

        tiles.forEach { tile ->
            key(tile.item.id) {
                val item = tile.item
                val col = item.cell % grid.columns
                val row = item.cell / grid.columns
                DraggableTile(
                    tile = tile,
                    actions = actions,
                    enabled = movingItemId == null,
                    onDropAt = { centerX, centerY ->
                        val cell = cellAt(centerX, centerY, cellWpx, cellHpx, grid)
                        val onto = tiles.firstOrNull { it.item.id != item.id && cell in coveredCells(it.item.cell, it.item.spanX, it.item.spanY, grid.columns) }
                        actions.onDrop(tile, cell, onto)
                    },
                    originPx = Offset(col * cellWpx, row * cellHpx),
                    sizePx = Offset(item.spanX * cellWpx, item.spanY * cellHpx),
                    modifier = Modifier
                        .offset(cellW * col, cellH * row)
                        .size(cellW * item.spanX, cellH * item.spanY)
                        .graphicsLayer { alpha = if (movingItemId == item.id) 0.4f else 1f },
                ) {
                    TileContent(tile, showLabels, iconScale, cellW, cellH, actions.badgeFor(tile))
                }
            }
        }
    }
}

/**
 * Wraps a tile with tap, long-press (menu) and long-press-drag (move)
 * gestures, plus matching TalkBack actions.
 */
@Composable
fun DraggableTile(
    tile: HomeTile,
    actions: HomeTileActions,
    enabled: Boolean,
    onDropAt: (centerX: Float, centerY: Float) -> Unit,
    originPx: Offset,
    sizePx: Offset,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var drag by remember { mutableStateOf(Offset.Zero) }
    var dragging by remember { mutableStateOf(false) }
    val currentTile by rememberUpdatedState(tile)
    val currentDropAt by rememberUpdatedState(onDropAt)
    val currentOrigin by rememberUpdatedState(originPx)
    val currentSize by rememberUpdatedState(sizePx)
    val label = tileLabel(tile)

    Box(
        modifier
            .zIndex(if (dragging) 10f else 0f)
            .offset { IntOffset(drag.x.roundToInt(), drag.y.roundToInt()) }
            .then(
                if (enabled && tile !is WidgetTile) {
                    Modifier
                        .pointerInput(tile.item.id) {
                            detectTapGestures(
                                onTap = { actions.onOpen(currentTile) },
                                onLongPress = { actions.onMenu(currentTile) },
                            )
                        }
                        .pointerInput(tile.item.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { dragging = true },
                                onDragEnd = {
                                    if (drag.getDistance() > 24f) {
                                        currentDropAt(currentOrigin.x + currentSize.x / 2 + drag.x, currentOrigin.y + currentSize.y / 2 + drag.y)
                                    }
                                    drag = Offset.Zero
                                    dragging = false
                                },
                                onDragCancel = { drag = Offset.Zero; dragging = false },
                                onDrag = { change, amount -> change.consume(); drag += amount },
                            )
                        }
                        .semantics(mergeDescendants = true) {
                            contentDescription = label
                            role = Role.Button
                            onClick(label = "Open") { actions.onOpen(currentTile); true }
                            onLongClick(label = "Options") { actions.onMenu(currentTile); true }
                        }
                } else {
                    Modifier
                },
            )
            .graphicsLayer { if (dragging) { scaleX = 1.08f; scaleY = 1.08f } },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

fun tileLabel(tile: HomeTile): String = when (tile) {
    is AppTile -> tile.entry.app.label
    is ShortcutTile -> tile.label
    is FolderTile -> "${tile.name}, folder, ${tile.children.size} apps"
    is WidgetTile -> "Widget"
}

@Composable
fun TileContent(tile: HomeTile, showLabels: Boolean, iconScale: Float, cellW: Dp, cellH: Dp, badge: Int) {
    val iconSize = (minOf(cellW, cellH) * 0.56f * iconScale).coerceIn(32.dp, 72.dp)
    when (tile) {
        is WidgetTile -> WidgetView(tile, cellW * tile.item.spanX, cellH * tile.item.spanY)
        is AppTile -> IconWithLabel(tile.entry.app.label, showLabels, badge) {
            AppIcon(tile.entry.app.key, tile.entry.app.label, iconSize)
        }
        is ShortcutTile -> IconWithLabel(tile.label, showLabels, 0) {
            val repository = rememberAppRepository()
            val info by produceState<android.content.pm.ShortcutInfo?>(null, tile.item.id) {
                value = withContext(Dispatchers.IO) {
                    repository.pinnedShortcut(tile.item.shortcutPackage.orEmpty(), tile.item.shortcutId.orEmpty(), tile.item.userSerial)
                }
            }
            ShortcutIcon(info, tile.label, iconSize)
        }
        is FolderTile -> IconWithLabel(tile.name, showLabels, 0) { FolderIcon(tile, iconSize) }
    }
}

@Composable
fun IconWithLabel(label: String, showLabel: Boolean, badge: Int, icon: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box {
            icon()
            SLBadge(count = badge, modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-6).dp))
        }
        if (showLabel) {
            Text(
                text = label,
                style = SLTheme.typography.caption.copy(shadow = WallpaperTextShadow),
                color = Color(0xFFF5F1E8),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 2.dp),
            )
        }
    }
}

/** Folder icon: a dark block showing up to four of its apps. */
@Composable
fun FolderIcon(folder: FolderTile, size: Dp) {
    val mini = (size - 10.dp) / 2
    Box(
        Modifier
            .size(size)
            .background(SLTheme.colors.surface.copy(alpha = 0.9f))
            .border(SLTheme.shapes.borderWidth, SLTheme.colors.borderStrong)
            .padding(3.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            folder.children.take(4).chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    row.forEach { AppIcon(it.entry.app.key, it.entry.app.label, mini) }
                }
            }
        }
    }
}

/** Hosts another app's widget view inside Compose. */
@Composable
fun WidgetView(tile: WidgetTile, width: Dp, height: Dp) {
    val host = rememberWidgetHost()
    val widgetId = tile.item.widgetId
    AndroidView(
        modifier = Modifier.fillMaxSize().padding(4.dp),
        factory = { context -> host.createView(widgetId) ?: FrameLayout(context) },
        update = { view ->
            if (view is android.appwidget.AppWidgetHostView) host.updateSize(view, width.value - 8f, height.value - 8f)
        },
    )
}

/** Page dots: the current page is a yellow bar. */
@Composable
fun PageIndicator(count: Int, current: Int, modifier: Modifier = Modifier) {
    if (count <= 1) return
    Row(
        modifier.semantics { contentDescription = "Page ${current + 1} of $count" },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            Box(
                Modifier
                    .size(width = if (index == current) 16.dp else 6.dp, height = 6.dp)
                    .background(if (index == current) SLTheme.colors.accentIndicator else Color(0x99F5F1E8)),
            )
        }
    }
}

/** Dock: up to five apps in one row on a translucent block. */
@Composable
fun Dock(
    tiles: List<HomeTile>,
    iconScale: Float,
    actions: HomeTileActions,
    modifier: Modifier = Modifier,
) {
    if (tiles.isEmpty()) return
    Row(
        modifier
            .fillMaxWidth()
            .background(SLTheme.colors.surface.copy(alpha = 0.72f))
            .border(SLTheme.shapes.borderWidth, SLTheme.colors.border)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        tiles.forEach { tile ->
            key(tile.item.id) {
                DraggableTile(
                    tile = tile,
                    actions = actions,
                    enabled = true,
                    onDropAt = { _, _ -> },
                    originPx = Offset.Zero,
                    sizePx = Offset.Zero,
                ) {
                    val size = (48.dp * iconScale).coerceIn(36.dp, 64.dp)
                    when (tile) {
                        is AppTile -> AppIcon(tile.entry.app.key, tile.entry.app.label, size)
                        is FolderTile -> FolderIcon(tile, size)
                        else -> Unit
                    }
                }
            }
        }
    }
}
