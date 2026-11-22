package com.sllauncher.pro.news

import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads RSS 2.0 and Atom feeds over HTTPS with short timeouts and a size
 * cap, so a slow or huge feed can never stall the launcher or eat data.
 */
@Singleton
class RssNewsProvider @Inject constructor() : NewsProvider {

    override suspend fun fetch(feed: NewsFeed): List<FetchedArticle> = withContext(Dispatchers.IO) {
        val connection = (URL(feed.url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "SLLauncher/1.0 (Android news reader)")
            setRequestProperty("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml")
        }
        try {
            if (connection.responseCode !in 200..299) throw IOException("HTTP ${connection.responseCode} from ${feed.url}")
            connection.inputStream.use { stream -> parse(LimitedInputStream(stream, 3L * 1024 * 1024)) }
        } finally {
            connection.disconnect()
        }
    }

    internal fun parse(stream: InputStream): List<FetchedArticle> {
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(stream, null)
        }
        val items = mutableListOf<FetchedArticle>()
        var inItem = false
        var title = ""; var link = ""; var summary = ""; var image: String? = null; var date: String? = null
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT && items.size < 40) {
            val name = parser.name?.lowercase()
            when (event) {
                XmlPullParser.START_TAG -> when {
                    name == "item" || name == "entry" -> {
                        inItem = true; title = ""; link = ""; summary = ""; image = null; date = null
                    }
                    !inItem -> Unit
                    name == "title" -> title = parser.nextText()
                    name == "link" -> {
                        val href = parser.getAttributeValue(null, "href")
                        val rel = parser.getAttributeValue(null, "rel")
                        if (href != null) { if (rel == null || rel == "alternate") link = href } else link = parser.nextText()
                    }
                    name == "description" || name == "summary" || (name == "content" && summary.isEmpty()) -> summary = parser.nextText()
                    name == "pubdate" || name == "published" || name == "updated" || name == "dc:date" -> if (date == null) date = parser.nextText()
                    name == "media:thumbnail" || name == "media:content" -> if (image == null) image = parser.getAttributeValue(null, "url")
                    name == "enclosure" -> if (image == null && parser.getAttributeValue(null, "type")?.startsWith("image") == true) image = parser.getAttributeValue(null, "url")
                }
                XmlPullParser.END_TAG -> if (name == "item" || name == "entry") {
                    inItem = false
                    val cleanTitle = excerptFromHtml(title, 200)
                    if (cleanTitle.isNotEmpty() && link.startsWith("http")) {
                        items += FetchedArticle(
                            url = link.trim(),
                            title = cleanTitle,
                            summary = excerptFromHtml(summary),
                            imageUrl = image?.takeIf { it.startsWith("https://") },
                            publishedAt = parseFeedDate(date),
                        )
                    }
                }
            }
            event = parser.next()
        }
        return items
    }
}

/** Stops reading after [limit] bytes. */
private class LimitedInputStream(private val source: InputStream, private var remaining: Long) : InputStream() {
    override fun read(): Int {
        if (remaining <= 0) return -1
        val b = source.read()
        if (b >= 0) remaining--
        return b
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (remaining <= 0) return -1
        val n = source.read(b, off, minOf(len.toLong(), remaining).toInt())
        if (n > 0) remaining -= n
        return n
    }

    override fun close() = source.close()
}
