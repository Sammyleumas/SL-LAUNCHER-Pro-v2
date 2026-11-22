package com.sllauncher.pro.controlcenter

import org.junit.Assert.assertEquals
import org.junit.Test

class SwipeSideTest {
    @Test fun `right half opens the control center`() {
        assertEquals(HomePanel.ControlCenter, panelForSwipeStart(startX = 900f, width = 1080f))
    }

    @Test fun `left half opens notifications`() {
        assertEquals(HomePanel.Notifications, panelForSwipeStart(startX = 100f, width = 1080f))
        assertEquals(HomePanel.Notifications, panelForSwipeStart(startX = 540f, width = 1080f))
    }

    @Test fun `unknown width falls back to notifications`() {
        assertEquals(HomePanel.Notifications, panelForSwipeStart(startX = 500f, width = 0f))
    }
}
