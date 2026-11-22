package com.sllauncher.pro

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.lifecycleScope
import com.sllauncher.pro.common.UiCommand
import com.sllauncher.pro.data.apps.AppIconLoader
import com.sllauncher.pro.data.apps.IconPackManager
import com.sllauncher.pro.productivity.ReminderReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.sllauncher.pro.common.UiCommandBus
import com.sllauncher.pro.data.settings.SettingsRepository
import com.sllauncher.pro.navigation.SLNavHost
import com.sllauncher.pro.navigation.returnToHome
import com.sllauncher.pro.ui.AppShell
import com.sllauncher.pro.ui.colorsFor
import com.sllauncher.pro.ui.typographyFor
import com.sllauncher.pro.widgets.WidgetHostManager
import com.sllauncher.core.designsystem.theme.SLTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject
import androidx.compose.foundation.shape.RoundedCornerShape
import com.sllauncher.core.designsystem.component.softRaised
import com.sllauncher.core.designsystem.component.softStyle

/**
 * The launcher's single home activity. Registered for the HOME category,
 * so Android shows it when the user presses Home once SL Launcher is the
 * default home app.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var widgetHost: WidgetHostManager
    @Inject lateinit var commandBus: UiCommandBus
    @Inject lateinit var shell: AppShell
    @Inject lateinit var iconPacks: IconPackManager
    @Inject lateinit var iconLoader: AppIconLoader

    /** Fires when the user presses Home while the launcher is already running. */
    private val homeButtonPresses = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        ReminderReceiver.ensureChannel(this)

        // Load the chosen icon pack at start-up and whenever it changes.
        lifecycleScope.launch {
            settingsRepository.settings.map { it.iconPackPackage }.distinctUntilChanged().collect { pkg ->
                val ok = withContext(Dispatchers.IO) { iconPacks.select(pkg) }
                if (ok) iconLoader.clear()
            }
        }

        setContent {
            val settings by settingsRepository.settingsOrNull.collectAsStateWithLifecycle()
            val current = settings ?: return@setContent // First read takes a few ms; avoid flashing defaults.
            SLTheme(
                colors = colorsFor(current, androidx.compose.foundation.isSystemInDarkTheme()),
                typography = typographyFor(current),
                reduceMotion = if (current.lowPerformanceMode) true else null,
                softShadows = !current.lowPerformanceMode,
            ) {
                val navController = rememberNavController()
                val snackbar = remember { SnackbarHostState() }
                val startOnboarding = remember { !current.onboardingComplete }

                LaunchedEffect(navController) {
                    homeButtonPresses.collect {
                        commandBus.send(UiCommand.CloseOverlays)
                        navController.returnToHome()
                    }
                }
                LaunchedEffect(navController) {
                    commandBus.commands.collect { command ->
                        when (command) {
                            is UiCommand.Navigate -> navController.navigate(command.route) { launchSingleTop = true }
                            is UiCommand.ShowMessage -> {
                                snackbar.currentSnackbarData?.dismiss()
                                snackbar.showSnackbar(command.text)
                            }
                            else -> Unit
                        }
                    }
                }

                androidx.compose.runtime.CompositionLocalProvider(
                    com.sllauncher.pro.common.LocalIconStyle provides com.sllauncher.pro.common.IconStyle(current.iconShape, current.iconSoftPlate),
                ) {
                Box(Modifier.fillMaxSize()) {
                    SLNavHost(
                        navController = navController,
                        startOnboarding = startOnboarding,
                        widgetHost = widgetHost,
                        commands = commandBus.commands,
                        homeExtras = shell.homeExtras(),
                        featureGraph = shell.featureGraph,
                    )
                    SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp)) { data ->
                        SLSnackbar(data.visuals.message)
                    }
                    shell.Overlays()
                }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        widgetHost.startListening()
    }

    override fun onStop() {
        super.onStop()
        widgetHost.stopListening()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            homeButtonPresses.tryEmit(Unit)
        }
    }
}

@Composable
private fun SLSnackbar(message: String) {
    Text(
        message,
        style = SLTheme.typography.body,
        color = SLTheme.colors.textPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .softRaised(RoundedCornerShape(SLTheme.shapes.cornerMedium), softStyle())
            .border(SLTheme.shapes.borderWidth, SLTheme.colors.borderStrong, RoundedCornerShape(SLTheme.shapes.cornerMedium))
            .padding(16.dp),
    )
}
