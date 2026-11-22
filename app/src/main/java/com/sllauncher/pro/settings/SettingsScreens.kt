package com.sllauncher.pro.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sllauncher.pro.assistant.VoiceInput
import com.sllauncher.pro.common.UiCommand
import com.sllauncher.pro.common.UiCommandBus
import com.sllauncher.pro.data.settings.LauncherSettings
import com.sllauncher.pro.data.settings.SettingsRepository
import com.sllauncher.pro.navigation.AboutDestination
import com.sllauncher.pro.navigation.AssistantDestination
import com.sllauncher.pro.navigation.CustomizationDestination
import com.sllauncher.pro.navigation.DeviceInfoDestination
import com.sllauncher.pro.navigation.LauncherSetupDestination
import com.sllauncher.pro.navigation.MusicDestination
import com.sllauncher.pro.navigation.NewsDestination
import com.sllauncher.pro.navigation.NotificationCenterDestination
import com.sllauncher.pro.navigation.PremiumDestination
import com.sllauncher.pro.navigation.PrivacyDestination
import com.sllauncher.pro.navigation.ProductivityDestination
import com.sllauncher.pro.navigation.QuickSettingsDestination
import com.sllauncher.pro.navigation.SettingsDestination
import com.sllauncher.pro.productivity.ProductivityRoute
import com.sllauncher.pro.ui.ScreenScaffold
import com.sllauncher.core.designsystem.component.SLBlock
import com.sllauncher.core.designsystem.component.SLButton
import com.sllauncher.core.designsystem.component.SLNavigationRow
import com.sllauncher.core.designsystem.component.SLSectionHeader
import com.sllauncher.core.designsystem.component.SLSwitchRow
import com.sllauncher.core.designsystem.theme.SLTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Registers every settings-area screen. */
fun NavGraphBuilder.settingsGraph(nav: NavHostController) {
    val back: () -> Unit = { nav.popBackStack() }
    val go: (Any) -> Unit = { route -> nav.navigate(route) { launchSingleTop = true } }
    composable<SettingsDestination> { SettingsHubRoute(onBack = back, onNavigate = go) }
    composable<QuickSettingsDestination> { QuickSettingsRoute(onBack = back, onNavigate = go) }
    composable<DeviceInfoDestination> { DeviceInfoRoute(onBack = back) }
    composable<ProductivityDestination> { ProductivityRoute(onBack = back) }
    composable<CustomizationDestination> { CustomizationRoute(onBack = back) }
    composable<PrivacyDestination> { PrivacyRoute(onBack = back) }
    composable<AssistantDestination> { AssistantSettingsRoute(onBack = back) }
    composable<PremiumDestination> { PremiumRoute(onBack = back) }
    composable<AboutDestination> { AboutRoute(onBack = back) }
}

@HiltViewModel
class GeneralSettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    val voice: VoiceInput,
    val entitlements: EntitlementRepository,
    private val bus: UiCommandBus,
) : ViewModel() {
    val state: StateFlow<LauncherSettings> = settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), settings.current)
    fun update(transform: (LauncherSettings) -> LauncherSettings) = viewModelScope.launch { settings.update(transform) }
    fun openAssistant() = bus.send(UiCommand.OpenAssistant)
}

@Composable
fun SettingsHubRoute(onBack: () -> Unit, onNavigate: (Any) -> Unit) {
    ScreenScaffold(title = "SL settings", onBack = onBack, scrollable = true) {
        SLSectionHeader("Launcher", Modifier.padding(16.dp, 12.dp))
        SLNavigationRow("Customize", { onNavigate(CustomizationDestination) }, subtitle = "Home, drawer, island, theme, profiles")
        SLNavigationRow("Default home app", { onNavigate(LauncherSetupDestination) })
        SLNavigationRow("Quick settings", { onNavigate(QuickSettingsDestination) })
        SLNavigationRow("Device info", { onNavigate(DeviceInfoDestination) })
        SLSectionHeader("Features", Modifier.padding(16.dp, 12.dp))
        SLNavigationRow("SL assistant", { onNavigate(AssistantDestination) })
        SLNavigationRow("Notifications", { onNavigate(NotificationCenterDestination) })
        SLNavigationRow("News", { onNavigate(NewsDestination) })
        SLNavigationRow("Music", { onNavigate(MusicDestination) })
        SLNavigationRow("Notes, tasks and focus", { onNavigate(ProductivityDestination) })
        SLSectionHeader("You", Modifier.padding(16.dp, 12.dp))
        SLNavigationRow("Privacy and backup", { onNavigate(PrivacyDestination) })
        SLNavigationRow("SL Premium", { onNavigate(PremiumDestination) })
        SLNavigationRow("About", { onNavigate(AboutDestination) })
    }
}

@Composable
fun AssistantSettingsRoute(onBack: () -> Unit, viewModel: GeneralSettingsViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    ScreenScaffold(title = "SL assistant", onBack = onBack, scrollable = true) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SLButton("Talk to SL", { viewModel.openAssistant() })
            SLSwitchRow("Speak replies", s.assistantSpeakReplies, { on -> viewModel.update { it.copy(assistantSpeakReplies = on) } }, subtitle = "Uses your phone's text-to-speech voice")
            SLBlock(Modifier.fillMaxWidth()) {
                Text("Voice", style = SLTheme.typography.title, color = SLTheme.colors.textPrimary)
                Text(
                    when {
                        !viewModel.voice.isAvailable -> "This phone has no speech recognition service. You can still type to SL."
                        viewModel.voice.isOnDevice -> "Speech is recognised on this phone. Audio is not sent anywhere."
                        else -> "Speech is recognised by your phone's speech service, which may use the internet."
                    },
                    style = SLTheme.typography.bodySmall,
                    color = SLTheme.colors.textSecondary,
                )
                Text("SL listens only after you tap its button, never in the background. There is no wake word.", style = SLTheme.typography.bodySmall, color = SLTheme.colors.textSecondary)
            }
            SLSectionHeader("What SL can do")
            listOf(
                "Apps" to "\"Open WhatsApp\", \"show my favourite apps\", \"open my app drawer\"",
                "Music" to "\"Play my music\", \"pause\", \"next song\" (works with any player that has a media session)",
                "Time" to "\"Set a timer for ten minutes\", \"alarm at 6:30 am\"",
                "Phone" to "\"Turn on Wi-Fi\" opens the Wi-Fi panel, \"flashlight on\", \"open the camera\"",
                "News" to "\"Read the latest news\", or \"read this\" while a story is open",
                "Notes" to "\"Note: buy bread\", \"remind me to call mum\"",
                "Messages" to "\"Reply I'm coming\" (apps that allow quick replies)",
                "Look" to "\"Make my home screen minimal\", \"change my wallpaper\"",
            ).forEach { (t, b) ->
                Column {
                    Text(t, style = SLTheme.typography.body, color = SLTheme.colors.textPrimary)
                    Text(b, style = SLTheme.typography.caption, color = SLTheme.colors.textSecondary)
                }
            }
            SLSectionHeader("SL app connection")
            Text(
                "Your separate SL assistant app can control the launcher through a protected connection. Only apps signed with the same key as SL Launcher can use it. Details for developers are in docs/ASSISTANT-BRIDGE.md.",
                style = SLTheme.typography.bodySmall,
                color = SLTheme.colors.textSecondary,
            )
            Text("AI answers and summaries need an AI service, which isn't connected yet.", style = SLTheme.typography.caption, color = SLTheme.colors.textSecondary)
        }
    }
}

@Composable
fun PremiumRoute(onBack: () -> Unit, viewModel: GeneralSettingsViewModel = hiltViewModel()) {
    val tier by viewModel.entitlements.tier.collectAsStateWithLifecycle()
    ScreenScaffold(title = "SL Premium", onBack = onBack, scrollable = true) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Your plan: ${tier.name}", style = SLTheme.typography.headline, color = SLTheme.colors.textPrimary)
            SLBlock(Modifier.fillMaxWidth()) {
                Text("Free, always", style = SLTheme.typography.title, color = SLTheme.colors.textPrimary)
                Text("Home screen, floating drawer, search, SL commands, Dynamic Island, notifications, news, music, notes and tasks, privacy controls and backup. No ads on your home screen, ever.", style = SLTheme.typography.bodySmall, color = SLTheme.colors.textSecondary)
            }
            SLBlock(Modifier.fillMaxWidth()) {
                Text("Premium", style = SLTheme.typography.title, color = SLTheme.colors.textPrimary)
                PremiumFeature.entries.filter { it.tier == Tier.Premium }.forEach { Text("• ${it.label}", style = SLTheme.typography.bodySmall, color = SLTheme.colors.textSecondary) }
            }
            SLBlock(Modifier.fillMaxWidth()) {
                Text("AI", style = SLTheme.typography.title, color = SLTheme.colors.textPrimary)
                PremiumFeature.entries.filter { it.tier == Tier.AI }.forEach { Text("• ${it.label}", style = SLTheme.typography.bodySmall, color = SLTheme.colors.textSecondary) }
            }
            Text(
                "Purchases aren't available yet. They will be sold through Google Play once SL is published, and unlocked only after Google Play confirms the purchase.",
                style = SLTheme.typography.body,
                color = SLTheme.colors.textSecondary,
            )
        }
    }
}

@Composable
fun AboutRoute(onBack: () -> Unit) {
    val context = LocalContext.current
    val version = remember(context) { versionName(context) }
    val licence by produceState("") {
        value = withContext(Dispatchers.IO) {
            runCatching { context.assets.open("licenses/space-grotesk-OFL.txt").bufferedReader().readText() }.getOrDefault("")
        }
    }
    ScreenScaffold(title = "About", onBack = onBack, scrollable = true) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("SL Launcher", style = SLTheme.typography.headline, color = SLTheme.colors.textPrimary)
            Text("Version $version", style = SLTheme.typography.body, color = SLTheme.colors.textSecondary)
            SLSectionHeader("Privacy")
            Text("SL has no account, no ads and no analytics. Your data stays on your phone. The full privacy policy is published with the app's store listing.", style = SLTheme.typography.bodySmall, color = SLTheme.colors.textSecondary)
            SLSectionHeader("Open-source licences")
            Text("Built with Android Jetpack (AndroidX, Compose, Room, DataStore, WorkManager, Media3), Kotlin, Dagger Hilt and Coil, under the Apache License 2.0.", style = SLTheme.typography.bodySmall, color = SLTheme.colors.textSecondary)
            Text("Space Grotesk font", style = SLTheme.typography.title, color = SLTheme.colors.textPrimary)
            Text(licence, style = SLTheme.typography.caption, color = SLTheme.colors.textSecondary)
        }
    }
}

private fun versionName(context: android.content.Context): String = runCatching {
    @Suppress("DEPRECATION")
    context.packageManager.getPackageInfo(context.packageName, 0).versionName
}.getOrNull() ?: "unknown"
