package com.sllauncher.pro.settings

import android.app.NotificationManager
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sllauncher.core.designsystem.component.SLBlock
import com.sllauncher.core.designsystem.component.SLButton
import com.sllauncher.core.designsystem.component.SLButtonStyle
import com.sllauncher.core.designsystem.component.SLChip
import com.sllauncher.core.designsystem.component.SLSearchField
import com.sllauncher.core.designsystem.component.SLSectionHeader
import com.sllauncher.core.designsystem.component.SLSwitchRow
import com.sllauncher.core.designsystem.theme.SLTheme
import com.sllauncher.pro.data.settings.CustomFeed
import com.sllauncher.pro.data.settings.GestureAction
import com.sllauncher.pro.gestures.SLAccessibilityService
import com.sllauncher.pro.news.DefaultNewsFeeds
import com.sllauncher.pro.ui.ScreenScaffold

/** Home-screen gestures and focus-session behaviour. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GesturesRoute(onBack: () -> Unit, viewModel: GeneralSettingsViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var serviceOn by remember { mutableStateOf(SLAccessibilityService.isRunning) }
    var dndAccess by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        serviceOn = SLAccessibilityService.isRunning
        dndAccess = context.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted
    }

    ScreenScaffold(title = "Gestures and focus", onBack = onBack, scrollable = true) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SLBlock(Modifier.fillMaxWidth(), shadow = true) {
                Text(
                    if (serviceOn) "Gesture helper is on" else "Gesture helper is off",
                    style = SLTheme.typography.title,
                    color = SLTheme.colors.textPrimary,
                )
                Text(
                    "Locking the screen and pulling down the notification shade need Android's accessibility permission. " +
                        "SL uses it only for these two actions. It doesn't read your screen or what you type.",
                    style = SLTheme.typography.bodySmall,
                    color = SLTheme.colors.textSecondary,
                )
                SLButton(
                    if (serviceOn) "Accessibility settings" else "Turn on in Accessibility",
                    { SLAccessibilityService.openSettings(context) },
                    style = if (serviceOn) SLButtonStyle.Secondary else SLButtonStyle.Primary,
                )
            }

            SLSectionHeader("Double-tap on the home screen")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GestureAction.entries.filter { it != GestureAction.SplitPanels }.forEach { a ->
                    SLChip(a.label, s.gestureDoubleTap == a, { viewModel.update { it.copy(gestureDoubleTap = a) } })
                }
            }
            SLSectionHeader("Swipe down on the home screen")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GestureAction.entries.forEach { a ->
                    SLChip(a.label, s.gestureSwipeDown == a, { viewModel.update { it.copy(gestureSwipeDown = a) } })
                }
            }
            Text("Swipe up always opens the app drawer.", style = SLTheme.typography.caption, color = SLTheme.colors.textSecondary)

            SLSectionHeader("Focus sessions")
            SLSwitchRow(
                title = "Do Not Disturb while focusing",
                subtitle = if (dndAccess) "Only priority calls and messages get through" else "Needs Do Not Disturb access. Tap to allow it.",
                checked = s.focusDoNotDisturb && dndAccess,
                onCheckedChange = { on ->
                    if (on && !dndAccess) {
                        runCatching {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    }
                    viewModel.update { it.copy(focusDoNotDisturb = on) }
                },
            )
            SLSwitchRow(
                title = "Ask before distracting apps",
                subtitle = "Social, games and media apps ask \"open anyway?\" during a focus session",
                checked = s.focusGuardDistractions,
                onCheckedChange = { on -> viewModel.update { it.copy(focusGuardDistractions = on) } },
            )
        }
    }
}

/** True for an https URL with a host, which is all SL accepts as a custom feed. */
fun isValidFeedUrl(url: String): Boolean {
    val u = url.trim()
    if (!u.startsWith("https://", ignoreCase = true)) return false
    val host = u.removePrefix("https://").removePrefix("HTTPS://").substringBefore('/').substringBefore('?')
    return host.contains('.') && host.length >= 4 && ' ' !in u
}

/** Built-in publishers, plus RSS feeds the user adds under "My feeds". */
@Composable
fun NewsSourcesRoute(onBack: () -> Unit, viewModel: GeneralSettingsViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    val urlOk = isValidFeedUrl(url)

    ScreenScaffold(title = "News sources", onBack = onBack, scrollable = true) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SLSectionHeader("My feeds")
            if (s.customFeeds.isEmpty()) {
                Text("Add any news site's RSS feed. Its headlines appear under \"My feeds\" in News.", style = SLTheme.typography.bodySmall, color = SLTheme.colors.textSecondary)
            }
            s.customFeeds.forEach { feed ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(feed.name, style = SLTheme.typography.title, color = SLTheme.colors.textPrimary)
                        Text(feed.url, style = SLTheme.typography.caption, color = SLTheme.colors.textSecondary, maxLines = 1)
                    }
                    SLButton("Remove", { viewModel.update { it.copy(customFeeds = it.customFeeds - feed) } }, style = SLButtonStyle.Ghost)
                }
            }
            SLBlock(Modifier.fillMaxWidth(), shadow = true) {
                Text("Add a feed", style = SLTheme.typography.title, color = SLTheme.colors.textPrimary)
                SLSearchField(query = name, onQueryChange = { name = it.take(40) }, placeholder = "Name, e.g. My local paper")
                SLSearchField(query = url, onQueryChange = { url = it.trim().take(300) }, placeholder = "https://example.com/feed")
                if (url.isNotBlank() && !urlOk) {
                    Text("Enter a full https:// address.", style = SLTheme.typography.caption, color = SLTheme.colors.error)
                }
                SLButton(
                    "Add feed",
                    {
                        val feed = CustomFeed(name.trim().ifBlank { url.removePrefix("https://").substringBefore('/') }, url.trim())
                        viewModel.update { st -> if (st.customFeeds.any { it.url == feed.url }) st else st.copy(customFeeds = st.customFeeds + feed) }
                        name = ""
                        url = ""
                    },
                    enabled = urlOk && s.customFeeds.size < 20,
                )
                Text("Only add feeds you're allowed to read. SL shows headlines and short excerpts and links to the original site.", style = SLTheme.typography.caption, color = SLTheme.colors.textSecondary)
            }

            SLSectionHeader("Built-in publishers")
            DefaultNewsFeeds.groupBy { it.category }.forEach { (category, feeds) ->
                Text("$category: " + feeds.joinToString { it.publisher }, style = SLTheme.typography.bodySmall, color = SLTheme.colors.textSecondary)
            }
        }
    }
}
