package com.sllauncher.pro.news

/** A publisher feed SL reads headlines from. */
data class NewsFeed(val publisher: String, val category: String, val url: String)

/**
 * Default sources: publishers' own public RSS feeds. SL shows headlines,
 * a short excerpt and the publisher's name, and always links to the
 * original article. It never copies full articles.
 *
 * IMPORTANT before a commercial release: many publishers allow RSS for
 * personal use only (for example the BBC's terms). Confirm permission with
 * each publisher, or replace this list with a licensed news API through
 * [NewsProvider]. See docs/NEWS-SOURCES.md.
 */
val DefaultNewsFeeds = listOf(
    NewsFeed("Premium Times", "Nigeria", "https://www.premiumtimesng.com/feed"),
    NewsFeed("Punch", "Nigeria", "https://punchng.com/feed/"),
    NewsFeed("Channels TV", "Nigeria", "https://www.channelstv.com/feed/"),
    NewsFeed("BBC News", "World", "https://feeds.bbci.co.uk/news/world/rss.xml"),
    NewsFeed("Al Jazeera", "World", "https://www.aljazeera.com/xml/rss/all.xml"),
    NewsFeed("BBC News", "Technology", "https://feeds.bbci.co.uk/news/technology/rss.xml"),
    NewsFeed("TechCabal", "Technology", "https://techcabal.com/feed/"),
    NewsFeed("BBC News", "Business", "https://feeds.bbci.co.uk/news/business/rss.xml"),
    NewsFeed("Nairametrics", "Business", "https://nairametrics.com/feed/"),
    NewsFeed("BBC Sport", "Sports", "https://feeds.bbci.co.uk/sport/rss.xml"),
    NewsFeed("BBC News", "Entertainment", "https://feeds.bbci.co.uk/news/entertainment_and_arts/rss.xml"),
    NewsFeed("BBC News", "Education", "https://feeds.bbci.co.uk/news/education/rss.xml"),
    NewsFeed("BBC News", "Science", "https://feeds.bbci.co.uk/news/science_and_environment/rss.xml"),
    NewsFeed("BBC News", "Health", "https://feeds.bbci.co.uk/news/health/rss.xml"),
    // Lifestyle: no source configured yet. The tab shows an honest empty state.
)

/** Swappable source of headlines (RSS today; a licensed API later). */
interface NewsProvider {
    suspend fun fetch(feed: NewsFeed): List<FetchedArticle>
}

data class FetchedArticle(
    val url: String,
    val title: String,
    val summary: String,
    val imageUrl: String?,
    val publishedAt: Long?,
)
