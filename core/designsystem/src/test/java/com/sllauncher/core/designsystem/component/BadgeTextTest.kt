package com.sllauncher.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Test

class BadgeTextTest {

    @Test
    fun `zero and negative counts show nothing`() {
        assertEquals("", badgeText(0))
        assertEquals("", badgeText(-2))
    }

    @Test
    fun `counts up to 99 are shown exactly`() {
        assertEquals("7", badgeText(7))
        assertEquals("99", badgeText(99))
    }

    @Test
    fun `counts above 99 are capped`() {
        assertEquals("99+", badgeText(100))
        assertEquals("99+", badgeText(4_000))
    }
}
