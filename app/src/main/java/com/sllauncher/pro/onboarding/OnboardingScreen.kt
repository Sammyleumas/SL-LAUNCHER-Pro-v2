package com.sllauncher.pro.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.sllauncher.pro.common.startSafely
import com.sllauncher.pro.data.settings.LauncherSettings
import com.sllauncher.pro.data.settings.SettingsRepository
import com.sllauncher.pro.data.settings.ThemeMode
import com.sllauncher.pro.launcher.DefaultHomeChecker
import com.sllauncher.pro.launcher.HomeRoleRepository
import com.sllauncher.core.designsystem.component.SLBlock
import com.sllauncher.core.designsystem.component.SLButton
import com.sllauncher.core.designsystem.component.SLButtonStyle
import com.sllauncher.core.designsystem.component.SLSectionHeader
import com.sllauncher.core.designsystem.component.SLSegment
import com.sllauncher.core.designsystem.component.SLSegmentedControl
import com.sllauncher.core.designsystem.component.SLSwitchRow
import com.sllauncher.core.designsystem.theme.SLTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val homeRole: HomeRoleRepository,
    val checker: DefaultHomeChecker,
) : ViewModel() {
    val settings: StateFlow<LauncherSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LauncherSettings())
    val isDefaultHome = homeRole.isDefaultHome

    fun refreshRole() = homeRole.refresh()
    fun update(transform: (LauncherSettings) -> LauncherSettings) = viewModelScope.launch { settingsRepository.update(transform) }
    fun finish(onDone: () -> Unit) = viewModelScope.launch {
        settingsRepository.update { it.copy(onboardingComplete = true) }
        onDone()
    }
}

private const val STEP_COUNT = 6

/** First-run setup. Nothing here requests a permission: each is asked for only when its feature is first used. */
@Composable
fun OnboardingRoute(onDone: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isDefault by viewModel.isDefaultHome.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { viewModel.refreshRole() }

    LifecycleResumeEffect(Unit) {
        viewModel.refreshRole()
        onPauseOrDispose { }
    }

    Box(Modifier.fillMaxSize().background(SLTheme.colors.background).safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 560.dp).fillMaxSize().padding(24.dp)) {
            Progress(step)
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (step) {
                    0 -> Step(
                        "Welcome to SL Launcher",
                        "A home screen built around you: a floating app drawer, the SL assistant, a live Dynamic Island, news, music and your own layout. This takes about a minute.",
                    )
                    1 -> {
                        Step("What you get", "Everything works on your phone. Nothing is uploaded unless you turn on a feature that needs the internet, such as news.")
                        listOf(
                            "Floating app drawer" to "Swipe up on the home screen. Categories, favourites, search.",
                            "SL assistant" to "Tap the yellow button and speak or type: \"set a timer for 10 minutes\".",
                            "Dynamic Island" to "Music, charging, messages and calls at the top of your home screen.",
                            "Your layout" to "Pages, widgets, folders, icon packs, themes and saved profiles.",
                        ).forEach { (t, b) ->
                            SLBlock(Modifier.fillMaxWidth()) {
                                Text(t, style = SLTheme.typography.title, color = SLTheme.colors.textPrimary)
                                Text(b, style = SLTheme.typography.bodySmall, color = SLTheme.colors.textSecondary)
                            }
                        }
                    }
                    2 -> {
                        Step(
                            "Make SL your home screen",
                            "Android lets you choose which app opens when you press Home. You can switch back any time in Android Settings › Apps › Default apps.",
                        )
                        if (isDefault) {
                            Text("SL Launcher is your default home app.", style = SLTheme.typography.title, color = SLTheme.colors.accentIndicator)
                        } else {
                            SLButton("Set as default home app", {
                                val intent = viewModel.checker.createRoleRequestIntent()
                                if (intent != null) roleLauncher.launch(intent) else context.startSafely(viewModel.checker.createHomeSettingsIntent())
                            }, modifier = Modifier.fillMaxWidth())
                            SLButton("Open Android home settings", { context.startSafely(viewModel.checker.createHomeSettingsIntent()) }, style = SLButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())
                            Text("You can also do this later from the home screen.", style = SLTheme.typography.caption, color = SLTheme.colors.textSecondary)
                        }
                    }
                    3 -> {
                        Step("Permissions, only when needed", "SL asks for access at the moment you turn on a feature that needs it, and explains why. You can say no and the rest of the launcher keeps working.")
                        listOf(
                            "Notification access" to "Dynamic Island, notification center, quick replies, music controls. You turn it on in Android settings.",
                            "Microphone" to "Only while you hold a conversation with SL. SL never listens in the background.",
                            "Contacts" to "Only if you turn on contact search.",
                            "Music and audio" to "Only to play songs stored on your phone.",
                            "Notifications" to "Only for reminders and timers you create.",
                            "Calendar" to "Only to show today's events in your daily overview.",
                        ).forEach { (t, b) ->
                            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text(t, style = SLTheme.typography.body, color = SLTheme.colors.textPrimary)
                                Text(b, style = SLTheme.typography.bodySmall, color = SLTheme.colors.textSecondary)
                            }
                        }
                    }
                    4 -> {
                        Step("Your layout", "Pick a starting point. Everything can be changed later in Customize.")
                        SLSectionHeader("Home grid columns")
                        SLSegmentedControl(
                            options = listOf(SLSegment("4"), SLSegment("5"), SLSegment("6")),
                            selectedIndex = (settings.homeColumns - 4).coerceIn(0, 2),
                            onSelect = { i -> viewModel.update { it.copy(homeColumns = 4 + i, drawerColumns = 4 + i, homeRows = if (i == 0) 5 else 6) } },
                        )
                        SLSectionHeader("Theme")
                        SLSegmentedControl(
                            options = listOf(SLSegment("Dark"), SLSegment("Light")),
                            selectedIndex = if (settings.themeMode == ThemeMode.Dark) 0 else 1,
                            onSelect = { i -> viewModel.update { it.copy(themeMode = if (i == 0) ThemeMode.Dark else ThemeMode.Light) } },
                        )
                        SLSwitchRow(title = "Show app names", checked = settings.homeShowLabels, onCheckedChange = { on -> viewModel.update { it.copy(homeShowLabels = on, drawerShowLabels = on) } })
                        SLSwitchRow(
                            title = "Low-performance mode",
                            subtitle = "Turns off animations and transparency. Good for older phones.",
                            checked = settings.lowPerformanceMode,
                            onCheckedChange = { on -> viewModel.update { it.copy(lowPerformanceMode = on) } },
                        )
                    }
                    else -> {
                        Step("Meet SL", "SL is your assistant inside the launcher. Tap the yellow SL button on the home screen, then speak or type. The first time you speak, Android asks for microphone access.")
                        SLSwitchRow(
                            title = "SL speaks replies",
                            subtitle = "Uses your phone's text-to-speech voice",
                            checked = settings.assistantSpeakReplies,
                            onCheckedChange = { on -> viewModel.update { it.copy(assistantSpeakReplies = on) } },
                        )
                        Text("Try: \"open WhatsApp\", \"play my music\", \"read the latest news\", \"make my home screen minimal\".", style = SLTheme.typography.bodySmall, color = SLTheme.colors.textSecondary)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (step > 0) SLButton("Back", { step-- }, style = SLButtonStyle.Ghost)
                Box(Modifier.weight(1f))
                if (step < STEP_COUNT - 1) {
                    SLButton(if (step == 2 && !isDefault) "Later" else "Next", { step++ })
                } else {
                    SLButton("Start", { viewModel.finish(onDone) })
                }
            }
        }
    }
}

@Composable
private fun Progress(step: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.semantics { heading() }) {
        repeat(STEP_COUNT) { i ->
            Box(Modifier.weight(1f).height(6.dp).background(if (i <= step) SLTheme.colors.accentIndicator else SLTheme.colors.surfaceElevated))
        }
    }
    Text("STEP ${step + 1} OF $STEP_COUNT", style = SLTheme.typography.label, color = SLTheme.colors.textSecondary, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun Step(title: String, body: String) {
    Text(title, style = SLTheme.typography.headline, color = SLTheme.colors.textPrimary, modifier = Modifier.semantics { heading() })
    Text(body, style = SLTheme.typography.body, color = SLTheme.colors.textSecondary)
    Box(Modifier.size(width = 48.dp, height = 6.dp).background(SLTheme.colors.accentIndicator))
}
