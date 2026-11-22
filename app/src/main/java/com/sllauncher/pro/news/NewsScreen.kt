package com.sllauncher.pro.news

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.sllauncher.pro.assistant.Speaker
import com.sllauncher.pro.common.SystemIntents
import com.sllauncher.pro.common.startSafely
import com.sllauncher.pro.data.db.ArticleEntity
import com.sllauncher.pro.data.settings.SettingsRepository
import com.sllauncher.pro.ui.ScreenScaffold
import com.sllauncher.core.designsystem.component.SLBlock
import com.sllauncher.core.designsystem.component.SLBottomSheet
import com.sllauncher.core.designsystem.component.SLButton
import com.sllauncher.core.designsystem.component.SLButtonStyle
import com.sllauncher.core.designsystem.component.SLChip
import com.sllauncher.core.designsystem.component.SLEmptyState
import com.sllauncher.core.designsystem.component.SLErrorState
import com.sllauncher.core.designsystem.component.SLLoadingIndicator
import com.sllauncher.core.designsystem.component.SLSearchField
import com.sllauncher.core.designsystem.component.SLSectionHeader
import com.sllauncher.core.designsystem.theme.SLTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class NewsTab { Feed, Saved, History }

data class NewsUiState(
    val category: String = "For You",
    val tab: NewsTab = NewsTab.Feed,
    val query: String = "",
    val articles: List<ArticleEntity> = emptyList(),
    val refreshing: Boolean = false,
    val report: RefreshReport? = null,
    val hasAnyArticles: Boolean = false,
    val online: Boolean = true,
)

@HiltViewModel
class NewsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: NewsRepository,
    private val settings: SettingsRepository,
    private val current: CurrentArticleHolder,
    val speaker: Speaker,
) : ViewModel() {
    private val category = MutableStateFlow("For You")
    private val tab = MutableStateFlow(NewsTab.Feed)
    private val query = MutableStateFlow("")
    private val refreshing = MutableStateFlow(false)

    val uiState: StateFlow<NewsUiState> = combine(
        combine(category, tab, query) { c, t, q -> Triple(c, t, q) },
        combine(repository.articles, repository.saved, repository.history) { a, s, h -> Triple(a, s, h) },
        refreshing,
        repository.lastRefresh,
    ) { (c, t, q), (all, saved, history), busy, report ->
        val source = when (t) {
            NewsTab.Feed -> when (c) {
                "For You" -> {
                    val weights = categoryWeights(history.map { it.category })
                    val now = System.currentTimeMillis()
                    all.sortedByDescending { forYouScore(it.publishedAt, it.category, weights, now) }
                }
                else -> all.filter { it.category == c }
            }
            NewsTab.Saved -> saved
            NewsTab.History -> history
        }
        NewsUiState(
            category = c,
            tab = t,
            query = q,
            articles = if (q.isBlank()) source else source.filter { it.title.contains(q, true) || it.summary.contains(q, true) || it.publisher.contains(q, true) },
            refreshing = busy,
            report = report,
            hasAnyArticles = all.isNotEmpty(),
            online = isOnline(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NewsUiState())

    init {
        viewModelScope.launch {
            if (repository.lastRefresh.value == null) refresh()
            NewsRefreshWorker.schedule(context, settings.current.newsAutoRefresh)
        }
    }

    fun setCategory(value: String) { category.value = value; tab.value = NewsTab.Feed }
    fun setTab(value: NewsTab) { tab.value = value }
    fun setQuery(value: String) { query.value = value }

    fun refresh() = viewModelScope.launch {
        refreshing.value = true
        try { repository.refresh(settings.current.newsCategories + NewsCategories.drop(1).toSet()) } finally { refreshing.value = false }
    }

    fun opened(article: ArticleEntity) {
        current.article = article
        viewModelScope.launch { repository.markRead(article.url) }
    }

    fun closed() { current.article = null; speaker.stop() }

    fun toggleSaved(article: ArticleEntity) = viewModelScope.launch { repository.setSaved(article.url, !article.saved) }

    fun listen(article: ArticleEntity) = speaker.speak("${article.title}. From ${article.publisher}. ${article.summary}")

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(android.net.ConnectivityManager::class.java)
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

/** News center: headlines and short excerpts from publishers, always linking to the original article. */
@Composable
fun NewsRoute(onBack: () -> Unit, viewModel: NewsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var open by remember { mutableStateOf<ArticleEntity?>(null) }

    ScreenScaffold(
        title = "News",
        onBack = onBack,
        actions = { SLButton(if (state.refreshing) "Updating" else "Refresh", { viewModel.refresh() }, style = SLButtonStyle.Ghost, enabled = !state.refreshing) },
    ) {
        SLSearchField(query = state.query, onQueryChange = viewModel::setQuery, placeholder = "Search headlines", modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(NewsCategories) { c -> SLChip(c, state.tab == NewsTab.Feed && state.category == c, { viewModel.setCategory(c) }) }
            item { SLChip("Saved", state.tab == NewsTab.Saved, { viewModel.setTab(NewsTab.Saved) }) }
            item { SLChip("History", state.tab == NewsTab.History, { viewModel.setTab(NewsTab.History) }) }
        }
        state.report?.takeIf { it.failedPublishers.isNotEmpty() && it.ok > 0 }?.let { report ->
            Text(
                "Couldn't reach: ${report.failedPublishers.joinToString()}",
                style = SLTheme.typography.caption,
                color = SLTheme.colors.textSecondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        when {
            state.articles.isEmpty() && state.refreshing -> SLLoadingIndicator(Modifier.padding(24.dp).align(Alignment.CenterHorizontally))
            state.articles.isEmpty() && !state.online && !state.hasAnyArticles -> SLErrorState(
                title = "You're offline",
                body = "Connect to Wi-Fi or mobile data to load headlines. Saved articles stay available offline.",
                onRetry = { viewModel.refresh() },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            state.articles.isEmpty() && state.report?.ok == 0 -> SLErrorState(
                title = "Couldn't load news",
                body = "None of the news sources answered. Try again in a moment.",
                onRetry = { viewModel.refresh() },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            state.articles.isEmpty() -> SLEmptyState(
                title = when {
                    state.tab == NewsTab.Saved -> "No saved stories"
                    state.tab == NewsTab.History -> "Nothing read yet"
                    state.category == "Lifestyle" -> "No lifestyle source yet"
                    else -> "No stories here yet"
                },
                body = when {
                    state.tab == NewsTab.Saved -> "Open a story and tap Save to keep it here."
                    state.category == "Lifestyle" -> "A licensed lifestyle source hasn't been configured. Other categories are available."
                    else -> "Pull new headlines with Refresh."
                },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val featured = state.articles.firstOrNull { it.imageUrl != null }.takeIf { state.tab == NewsTab.Feed && state.query.isBlank() }
                if (featured != null) {
                    item(key = "featured") { FeaturedCard(featured) { open = featured; viewModel.opened(featured) } }
                }
                items(state.articles.filter { it != featured }, key = { it.url }) { a ->
                    ArticleRow(a) { open = a; viewModel.opened(a) }
                }
                item { Text("Headlines and excerpts belong to their publishers. Tap a story to read it on the publisher's site.", style = SLTheme.typography.caption, color = SLTheme.colors.textSecondary, modifier = Modifier.padding(8.dp)) }
            }
        }
    }

    open?.let { article ->
        val related = state.articles.filter { it.category == article.category && it.url != article.url }.take(3)
        SLBottomSheet(onDismissRequest = { open = null; viewModel.closed() }, title = article.publisher) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(article.title, style = SLTheme.typography.headline, color = SLTheme.colors.textPrimary)
                Text("${article.publisher} · ${article.category} · ${DateUtils.getRelativeTimeSpanString(article.publishedAt)}", style = SLTheme.typography.caption, color = SLTheme.colors.textSecondary)
                if (article.summary.isNotEmpty()) Text(article.summary, style = SLTheme.typography.body, color = SLTheme.colors.textPrimary)
                SLButton("Read full story on ${article.publisher}", { context.startSafely(SystemIntents.openUrl(article.url)) }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SLButton(if (article.saved) "Saved" else "Save", { viewModel.toggleSaved(article); open = article.copy(saved = !article.saved) }, style = SLButtonStyle.Secondary)
                    SLButton("Listen", { viewModel.listen(article) }, style = SLButtonStyle.Secondary)
                    SLButton("Share", { context.startSafely(SystemIntents.share("${article.title}\n${article.url}", article.title)) }, style = SLButtonStyle.Secondary)
                }
                Text("AI summaries need an AI service, which isn't set up yet.", style = SLTheme.typography.caption, color = SLTheme.colors.textSecondary)
                if (related.isNotEmpty()) {
                    SLSectionHeader("Related")
                    related.forEach { r -> ArticleRow(r) { open = r; viewModel.opened(r) } }
                }
            }
        }
    }
}

@Composable
private fun FeaturedCard(article: ArticleEntity, onClick: () -> Unit) {
    SLBlock(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick), shadow = true) {
        AsyncImage(
            model = article.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(SLTheme.colors.surfaceElevated),
        )
        Text("FEATURED · ${article.category.uppercase()}", style = SLTheme.typography.label, color = SLTheme.colors.accentIndicator)
        Text(article.title, style = SLTheme.typography.title, color = SLTheme.colors.textPrimary)
        Text("${article.publisher} · ${DateUtils.getRelativeTimeSpanString(article.publishedAt)}", style = SLTheme.typography.caption, color = SLTheme.colors.textSecondary)
    }
}

@Composable
private fun ArticleRow(article: ArticleEntity, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp).clickable(role = Role.Button, onClick = onClick).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(article.title, style = SLTheme.typography.body, color = SLTheme.colors.textPrimary, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text("${article.publisher} · ${DateUtils.getRelativeTimeSpanString(article.publishedAt)}", style = SLTheme.typography.caption, color = SLTheme.colors.textSecondary)
        }
        if (article.imageUrl != null) {
            AsyncImage(model = article.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(72.dp).background(SLTheme.colors.surfaceElevated))
        }
    }
}
