package com.sllauncher.pro.news

import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** News categories shown as tabs, in order. "For You" is computed, not fetched. */
val NewsCategories = listOf(
    "For You", "Nigeria", "World", "Technology", "Business", "Sports",
    "Entertainment", "Education", "Science", "Health", "Lifestyle",
)

/**
 * Plain text from a feed's HTML description, capped at [maxLength] so SL
 * only ever shows a short excerpt, never the full article.
 */
fun excerptFromHtml(html: String, maxLength: Int = 280): String {
    val text = html
        .replace(Regex("(?is)<(script|style)[^>]*>.*?</\\1>"), " ")
        .replace(Regex("(?s)<[^>]*>"), " ")
        .replace("&nbsp;", " ").replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'")
        .replace("&#8217;", "'").replace("&#8216;", "'").replace("&#8220;", "\"").replace("&#8221;", "\"")
        .replace("&lt;", "<").replace("&gt;", ">").replace("&hellip;", "…").replace("&#8230;", "…")
        .replace(Regex("\\[…]|\\[\\.\\.\\.]"), "…")
        .replace(Regex("\\s+"), " ")
        .trim()
    if (text.length <= maxLength) return text
    val cut = text.take(maxLength)
    val lastSpace = cut.lastIndexOf(' ')
    return (if (lastSpace > maxLength / 2) cut.take(lastSpace) else cut).trimEnd('.', ',', ' ') + "…"
}

private val rssDateFormats = listOf(
    DateTimeFormatter.RFC_1123_DATE_TIME,
    DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH),
    DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss z", Locale.ENGLISH),
    DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm z", Locale.ENGLISH),
)

/** RSS (RFC 822) and Atom (ISO 8601) dates to epoch millis, or null if unreadable. */
fun parseFeedDate(raw: String?): Long? {
    val text = raw?.trim().orEmpty()
    if (text.isEmpty()) return null
    runCatching { return OffsetDateTime.parse(text).toInstant().toEpochMilli() }
    for (format in rssDateFormats) {
        runCatching { return ZonedDateTime.parse(text, format).toInstant().toEpochMilli() }
    }
    return null
}

/**
 * Category weights for "For You": categories the user reads more rank
 * higher. Every category keeps a small base weight so the feed stays varied.
 */
fun categoryWeights(readCategories: List<String>): Map<String, Double> {
    val counts = readCategories.groupingBy { it }.eachCount()
    val total = counts.values.sum().coerceAtLeast(1)
    return NewsCategories.drop(1).associateWith { 0.2 + (counts[it] ?: 0).toDouble() / total }
}

/** Score for ranking: newer and from preferred categories first. */
fun forYouScore(publishedAt: Long, category: String, weights: Map<String, Double>, now: Long): Double {
    val ageHours = ((now - publishedAt).coerceAtLeast(0)) / 3_600_000.0
    val freshness = 1.0 / (1.0 + ageHours / 6.0)
    return freshness * (weights[category] ?: 0.2)
}
