package com.sllauncher.pro.news

import android.util.Log
import com.sllauncher.pro.data.db.ArticleDao
import com.sllauncher.pro.data.db.ArticleEntity
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

data class RefreshReport(val ok: Int, val failedPublishers: List<String>, val at: Long)

/** The article the user has open, for "read this" (assistant context). */
@Singleton
class CurrentArticleHolder @Inject constructor() {
    @Volatile var article: ArticleEntity? = null
}

@Singleton
class NewsRepository @Inject constructor(
    private val dao: ArticleDao,
    private val provider: NewsProvider,
) {
    private val mutex = Mutex()
    private val _lastRefresh = MutableStateFlow<RefreshReport?>(null)
    val lastRefresh: StateFlow<RefreshReport?> = _lastRefresh.asStateFlow()

    val articles: Flow<List<ArticleEntity>> = dao.observeAll()
    val saved: Flow<List<ArticleEntity>> = dao.observeSaved()
    val history: Flow<List<ArticleEntity>> = dao.observeHistory()

    /** Fetches [categories] in parallel; one failing feed never blocks the others. */
    suspend fun refresh(categories: Set<String>): RefreshReport = mutex.withLock {
        val now = System.currentTimeMillis()
        val feeds = DefaultNewsFeeds.filter { it.category in categories || categories.isEmpty() }
        val results = coroutineScope {
            feeds.map { feed ->
                async {
                    feed to runCatching { provider.fetch(feed) }.onFailure { Log.w("News", "${feed.publisher} ${feed.category} failed", it) }
                }
            }.awaitAll()
        }
        val fresh = results.flatMap { (feed, result) ->
            result.getOrDefault(emptyList()).map {
                ArticleEntity(
                    url = it.url,
                    title = it.title,
                    summary = it.summary,
                    imageUrl = it.imageUrl,
                    publisher = feed.publisher,
                    category = feed.category,
                    publishedAt = it.publishedAt ?: now,
                    fetchedAt = now,
                )
            }
        }
        dao.insertNew(fresh)
        dao.pruneBefore(now - 7L * 24 * 3600 * 1000)
        RefreshReport(
            ok = results.count { it.second.isSuccess },
            failedPublishers = results.filter { it.second.isFailure }.map { "${it.first.publisher} (${it.first.category})" },
            at = now,
        ).also { _lastRefresh.value = it }
    }

    suspend fun setSaved(url: String, saved: Boolean) = dao.setSaved(url, saved)
    suspend fun markRead(url: String) = dao.markRead(url, System.currentTimeMillis())

    /** Short spoken briefing: publisher and headline for the newest stories. */
    suspend fun headlinesForSpeech(limit: Int = 5): List<ArticleEntity> = dao.latest(limit)
}
