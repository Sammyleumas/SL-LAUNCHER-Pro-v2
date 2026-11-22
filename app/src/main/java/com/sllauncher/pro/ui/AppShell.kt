package com.sllauncher.pro.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.composable
import com.sllauncher.pro.assistant.AssistantOverlay
import com.sllauncher.pro.common.UiCommand
import com.sllauncher.pro.common.UiCommandBus
import com.sllauncher.pro.island.DynamicIsland
import com.sllauncher.pro.music.MusicRoute
import com.sllauncher.pro.navigation.FeatureGraph
import com.sllauncher.pro.navigation.HomeExtras
import com.sllauncher.pro.navigation.MusicDestination
import com.sllauncher.pro.navigation.NewsDestination
import com.sllauncher.pro.navigation.NotificationCenterDestination
import com.sllauncher.pro.navigation.SettingsDestination
import com.sllauncher.pro.news.NewsRoute
import com.sllauncher.pro.notifications.NotificationCenterRoute
import com.sllauncher.pro.notifications.NotificationRepository
import com.sllauncher.pro.settings.settingsGraph
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Connects feature areas to the launcher shell: their screens
 * ([featureGraph]), their pieces of the home screen ([homeExtras]) and
 * app-wide overlays ([Overlays], e.g. the assistant panel).
 */
@Singleton
class AppShell @Inject constructor(
    private val notifications: NotificationRepository,
    private val bus: UiCommandBus,
) {
    val featureGraph: FeatureGraph = { nav ->
        val back: () -> Unit = { nav.popBackStack() }
        composable<NotificationCenterDestination> { NotificationCenterRoute(onBack = back) }
        composable<NewsDestination> { NewsRoute(onBack = back) }
        composable<MusicDestination> { MusicRoute(onBack = back) }
        settingsGraph(nav)
    }

    @Composable
    fun homeExtras(): HomeExtras {
        val active by notifications.notifications.collectAsStateWithLifecycle()
        val counts = remember(active) {
            active.filter { !it.isGroupSummary && it.isClearable }.groupingBy { it.packageName }.eachCount()
        }
        return remember(counts) {
            HomeExtras(
                island = { DynamicIsland(onOpenSettings = { bus.send(UiCommand.Navigate(SettingsDestination)) }) },
                overview = { DailyOverview() },
                badgeFor = { pkg -> counts[pkg] ?: 0 },
            )
        }
    }

    @Composable
    fun Overlays() {
        var assistantOpen by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            bus.commands.collect { command ->
                when (command) {
                    UiCommand.OpenAssistant -> assistantOpen = true
                    is UiCommand.Navigate, UiCommand.CloseOverlays -> assistantOpen = false
                    else -> Unit
                }
            }
        }
        AssistantOverlay(visible = assistantOpen, onDismiss = { assistantOpen = false })
    }
}
