package com.sllauncher.pro.home

/** Size of one home page grid. */
data class GridSpec(val columns: Int, val rows: Int) {
    val cellCount: Int get() = columns * rows
}

/** Anything placed on a grid: position and size in cells. */
interface GridPlacement {
    val placementId: Long
    val cell: Int
    val spanX: Int
    val spanY: Int
}

/** Cells covered by an item at [cell] spanning [spanX] x [spanY]. */
fun coveredCells(cell: Int, spanX: Int, spanY: Int, columns: Int): List<Int> {
    val row = cell / columns
    val col = cell % columns
    return buildList {
        for (dy in 0 until spanY) for (dx in 0 until spanX) add((row + dy) * columns + col + dx)
    }
}

/** True when an item of this span starting at [cell] stays inside the grid. */
fun fitsInGrid(cell: Int, spanX: Int, spanY: Int, grid: GridSpec): Boolean {
    if (cell < 0) return false
    val row = cell / grid.columns
    val col = cell % grid.columns
    return col + spanX <= grid.columns && row + spanY <= grid.rows
}

fun occupiedCells(items: List<GridPlacement>, grid: GridSpec, excludeId: Long? = null): Set<Int> =
    items.filter { it.placementId != excludeId }
        .flatMap { coveredCells(it.cell, it.spanX, it.spanY, grid.columns) }
        .toSet()

/** True when the item can be placed at [cell] without overlapping anything else. */
fun canPlace(items: List<GridPlacement>, grid: GridSpec, cell: Int, spanX: Int, spanY: Int, excludeId: Long? = null): Boolean {
    if (!fitsInGrid(cell, spanX, spanY, grid)) return false
    val taken = occupiedCells(items, grid, excludeId)
    return coveredCells(cell, spanX, spanY, grid.columns).none { it in taken }
}

/** First free top-left cell for an item of this span, scanning row by row, or null when the page is full. */
fun findFreeCell(items: List<GridPlacement>, grid: GridSpec, spanX: Int = 1, spanY: Int = 1, excludeId: Long? = null): Int? =
    (0 until grid.cellCount).firstOrNull { canPlace(items, grid, it, spanX, spanY, excludeId) }

/** Grid cell under a point, given the cell size. Clamped to the grid. */
fun cellAt(x: Float, y: Float, cellWidth: Float, cellHeight: Float, grid: GridSpec): Int {
    val col = (x / cellWidth).toInt().coerceIn(0, grid.columns - 1)
    val row = (y / cellHeight).toInt().coerceIn(0, grid.rows - 1)
    return row * grid.columns + col
}

/** Cell span a widget needs for its minimum size, capped to the grid. */
fun widgetSpan(minWidthDp: Int, minHeightDp: Int, cellWidthDp: Float, cellHeightDp: Float, grid: GridSpec): Pair<Int, Int> {
    val x = kotlin.math.ceil(minWidthDp / cellWidthDp).toInt().coerceIn(1, grid.columns)
    val y = kotlin.math.ceil(minHeightDp / cellHeightDp).toInt().coerceIn(1, grid.rows)
    return x to y
}
