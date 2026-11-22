package com.sllauncher.pro.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedUrlTest {
    @Test fun `https feeds are accepted`() {
        assertTrue(isValidFeedUrl("https://example.com/feed"))
        assertTrue(isValidFeedUrl("https://news.example.ng/rss.xml?x=1"))
    }

    @Test fun `other addresses are rejected`() {
        assertFalse(isValidFeedUrl("http://example.com/feed"))
        assertFalse(isValidFeedUrl("example.com/feed"))
        assertFalse(isValidFeedUrl("https://localhost"))
        assertFalse(isValidFeedUrl("https://exa mple.com"))
        assertFalse(isValidFeedUrl(""))
    }
}
