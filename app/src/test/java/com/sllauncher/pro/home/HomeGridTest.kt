package com.sllauncher.pro.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeGridTest {

    private val grid = GridSpec(columns = 4, rows = 5)

    private data class P(override val placementId: Long, override val cell: Int, override val spanX: Int = 1, override val spanY: Int = 1) : GridPlacement

    @Test
    fun `covered cells of a 2x2 widget`() {
        assertEquals(listOf(5, 6, 9, 10), coveredCells(5, 2, 2, 4))
    }

    @Test
    fun `items must stay inside the grid`() {
        assertTrue(fitsInGrid(2, 2, 1, grid))
        assertFalse(fitsInGrid(3, 2, 1, grid)) // would wrap past the right edge
        assertFalse(fitsInGrid(16, 1, 2, grid)) // would fall off the bottom
        assertFalse(fitsInGrid(-1, 1, 1, grid))
    }

    @Test
    fun `free cell skips occupied space and respects spans`() {
        val items = listOf(P(1, 0), P(2, 1), P(3, 4, 2, 2))
        assertEquals(2, findFreeCell(items, grid))
        // A 2x2 needs two free columns on two rows: first fit is cell 2 (cols 2-3, rows 0-1).
        assertEquals(2, findFreeCell(items, grid, 2, 2))
    }

    @Test
    fun `full page has no free cell`() {
        val items = (0 until grid.cellCount).map { P(it.toLong(), it) }
        assertNull(findFreeCell(items, grid))
    }

    @Test
    fun `an item can be placed over its own old position`() {
        val items = listOf(P(1, 0, 2, 2))
        assertTrue(canPlace(items, grid, 1, 2, 2, excludeId = 1))
        assertFalse(canPlace(items, grid, 1, 2, 2, excludeId = null))
    }

    @Test
    fun `touch position maps to a clamped cell`() {
        assertEquals(5, cellAt(150f, 150f, 100f, 100f, grid))
        assertEquals(19, cellAt(9999f, 9999f, 100f, 100f, grid))
        assertEquals(0, cellAt(-5f, -5f, 100f, 100f, grid))
    }

    @Test
    fun `widget span from minimum size`() {
        assertEquals(3 to 2, widgetSpan(250, 110, 90f, 100f, grid))
        assertEquals(4 to 5, widgetSpan(2000, 2000, 90f, 100f, grid)) // capped to the grid
    }
}
