package com.sllauncher.core.designsystem.component

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FloatingPanelSizeTest {

    private fun assertDp(expected: Dp, actual: Dp) = assertEquals(expected.value, actual.value, 0.01f)

    @Test
    fun `typical phone uses 80 percent width and 70 percent height`() {
        val size = floatingPanelSize(maxWidth = 360.dp, maxHeight = 800.dp)
        assertDp(288.dp, size.width)
        assertDp(560.dp, size.height)
    }

    @Test
    fun `narrow phone grows to minimum width so a 4-column grid fits`() {
        val size = floatingPanelSize(maxWidth = 320.dp, maxHeight = 560.dp)
        assertDp(280.dp, size.width)
        assertDp(392.dp, size.height)
    }

    @Test
    fun `tablet width is capped so the panel stays a panel`() {
        val size = floatingPanelSize(maxWidth = 1280.dp, maxHeight = 800.dp)
        assertDp(560.dp, size.width)
        assertDp(560.dp, size.height)
    }

    @Test
    fun `short landscape window uses most of the height without crossing the gutter`() {
        val size = floatingPanelSize(maxWidth = 800.dp, maxHeight = 360.dp)
        assertDp(560.dp, size.width)
        assertDp(328.dp, size.height) // 360 - 2 x 16dp gutter
    }

    @Test
    fun `panel never exceeds the window minus the edge gutter`() {
        val widths = listOf(200, 280, 320, 360, 411, 600, 840, 1280)
        val heights = listOf(200, 360, 480, 640, 800, 1200)
        for (w in widths) for (h in heights) {
            val size = floatingPanelSize(w.dp, h.dp)
            assertTrue("width at ${w}x$h", size.width <= w.dp - FloatingPanelEdgeGutter * 2 + 0.01.dp)
            assertTrue("height at ${w}x$h", size.height <= h.dp - FloatingPanelEdgeGutter * 2 + 0.01.dp)
            assertTrue("positive at ${w}x$h", size.width > 0.dp && size.height > 0.dp)
        }
    }
}
