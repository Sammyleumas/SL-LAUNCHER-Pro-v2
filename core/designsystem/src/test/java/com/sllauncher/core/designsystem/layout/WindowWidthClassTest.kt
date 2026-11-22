package com.sllauncher.core.designsystem.layout

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class WindowWidthClassTest {

    @Test
    fun `breakpoints match Android window size classes`() {
        assertEquals(WindowWidthClass.Compact, windowWidthClassOf(360.dp))
        assertEquals(WindowWidthClass.Compact, windowWidthClassOf(599.9.dp))
        assertEquals(WindowWidthClass.Medium, windowWidthClassOf(600.dp))
        assertEquals(WindowWidthClass.Medium, windowWidthClassOf(839.dp))
        assertEquals(WindowWidthClass.Expanded, windowWidthClassOf(840.dp))
    }
}
