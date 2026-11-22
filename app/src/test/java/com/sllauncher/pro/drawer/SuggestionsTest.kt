package com.sllauncher.pro.drawer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class SuggestionsTest {
    private val zone = ZoneOffset.UTC
    private fun at(day: Int, hour: Int, minute: Int = 0) = LocalDateTime.of(2026, 11, day, hour, minute).toInstant(zone).toEpochMilli()

    @Test fun `no history gives no suggestions`() {
        assertTrue(suggestApps(emptyList(), at(20, 8), zone).isEmpty())
    }

    @Test fun `apps used at this time of day rank first`() {
        val log = (10..19).flatMap { d -> listOf("news" to at(d, 8), "games" to at(d, 21)) }
        val morning = suggestApps(log, at(20, 8, 15), zone)
        val night = suggestApps(log, at(20, 21, 10), zone)
        assertEquals("news", morning.first())
        assertEquals("games", night.first())
    }

    @Test fun `recent use beats old use at the same time`() {
        val log = listOf("old" to at(1, 9), "old" to at(2, 9), "new" to at(19, 9), "new" to at(18, 9))
        assertEquals("new", suggestApps(log, at(20, 9), zone).first())
    }

    @Test fun `distance wraps around midnight`() {
        assertEquals(20, circularMinutes(23 * 60 + 50, 10))
        assertEquals(60, circularMinutes(60, 120))
    }

    @Test fun `limit is respected`() {
        val log = (1..10).map { "app$it" to at(19, 9) }
        assertEquals(4, suggestApps(log, at(20, 9), zone, limit = 4).size)
    }
}
