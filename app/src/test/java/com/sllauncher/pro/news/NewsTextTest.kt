package com.sllauncher.pro.news

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NewsTextTest {

    @Test
    fun `html is stripped and entities decoded`() {
        assertEquals("Tinubu meets governors & ministers", excerptFromHtml("<p>Tinubu meets <b>governors</b> &amp; ministers</p>"))
        assertEquals("Hello world", excerptFromHtml("<script>bad()</script>Hello <img src=x>world"))
    }

    @Test
    fun `excerpts are capped so full articles are never shown`() {
        val long = "word ".repeat(200)
        val excerpt = excerptFromHtml(long, maxLength = 280)
        assertTrue(excerpt.length <= 281)
        assertTrue(excerpt.endsWith("…"))
    }

    @Test
    fun `rss and atom dates parse`() {
        assertEquals(1_763_628_000_000L, parseFeedDate("Thu, 20 Nov 2025 08:40:00 GMT"))
        assertEquals(1_763_628_000_000L, parseFeedDate("2025-11-20T08:40:00Z"))
        assertEquals(1_763_628_000_000L, parseFeedDate("Thu, 20 Nov 2025 09:40:00 +0100"))
        assertNull(parseFeedDate("yesterday"))
        assertNull(parseFeedDate(null))
    }

    @Test
    fun `for you favours categories the user reads`() {
        val weights = categoryWeights(listOf("Technology", "Technology", "Sports"))
        val now = 10_000_000_000L
        val tech = forYouScore(now, "Technology", weights, now)
        val health = forYouScore(now, "Health", weights, now)
        assertTrue(tech > health)
    }

    @Test
    fun `newer stories outrank older ones in the same category`() {
        val w = categoryWeights(emptyList())
        val now = 10_000_000_000L
        assertTrue(forYouScore(now, "World", w, now) > forYouScore(now - 24 * 3_600_000L, "World", w, now))
    }
}
