package com.sllauncher.pro.commands

import android.content.Context
import com.sllauncher.pro.common.DrawerView
import com.sllauncher.pro.common.SystemIntents
import com.sllauncher.pro.common.UiCommand
import com.sllauncher.pro.common.UiCommandBus
import com.sllauncher.pro.common.startSafely
import com.sllauncher.pro.data.apps.AppRepository
import com.sllauncher.pro.drawer.matchScore
import com.sllauncher.pro.navigation.NotificationCenterDestination
import com.sllauncher.pro.navigation.SettingsDestination
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runs [LauncherCommand]s using only Android-approved APIs: launcher APIs
 * for apps, public intents for system screens and the clock app, and
 * SL's own features. Every path returns an [ActionResult] the caller can
 * show or speak; nothing fails silently.
 */
@Singleton
class LauncherActionExecutor @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val apps: AppRepository,
    private val bus: UiCommandBus,
    private val features: FeatureCommandHandlers,
) {
    /**
     * @param confirmed true when the user already approved a command that
     *   [LauncherCommand.needsConfirmation].
     */
    suspend fun execute(command: LauncherCommand, confirmed: Boolean = false): ActionResult {
        if (command.needsConfirmation && !confirmed) {
            return ActionResult.NeedsConfirmation("${command.description}?", command)
        }
        return when (command) {
            is LauncherCommand.OpenApp -> openApp(command.appName)
            LauncherCommand.OpenDrawer -> { bus.send(UiCommand.OpenDrawer()); ActionResult.Success("Here are your apps.") }
            LauncherCommand.ShowFavorites -> { bus.send(UiCommand.OpenDrawer(DrawerView.Favorites)); ActionResult.Success("Here are your favourites.") }
            LauncherCommand.ShowNotifications -> navigate(NotificationCenterDestination, "Here are your notifications.")
            LauncherCommand.OpenLauncherSettings -> navigate(SettingsDestination, "Opening SL settings.")
            is LauncherCommand.OpenSettings -> openSettings(command.panel)
            is LauncherCommand.SetTimer -> intent(SystemIntents.setTimer(command.seconds, command.label), "Timer set for ${formatDuration(command.seconds)}.", "No clock app can set timers on this phone.")
            is LauncherCommand.SetAlarm -> intent(SystemIntents.setAlarm(command.hour, command.minute, null), "Alarm set for %02d:%02d.".format(command.hour, command.minute), "No clock app can set alarms on this phone.")
            LauncherCommand.ChangeWallpaper -> intent(SystemIntents.wallpaperPicker(), "Pick a wallpaper.", "Wallpaper picker not available.")
            is LauncherCommand.WebSearch -> intent(SystemIntents.webSearch(command.query), "Searching for ${command.query}.", "No browser or search app found.")
            LauncherCommand.OpenCamera -> intent(SystemIntents.camera(), "Opening the camera.", "No camera app found.")
            else -> features.handle(command)
        }
    }

    private fun openApp(name: String): ActionResult {
        val best = apps.entries.value
            .filter { !it.hidden }
            .map { it to matchScore(it.app.label, name) }
            .filter { it.second >= 2.0 }
            .maxByOrNull { it.second * 1000 + it.first.launchCount }
            ?.first
            ?: return ActionResult.Failure("I couldn't find an app called $name.")
        return if (apps.launch(best.app.key)) {
            ActionResult.Success("Opening ${best.app.label}.")
        } else {
            ActionResult.Failure("${best.app.label} couldn't be opened.")
        }
    }

    private fun openSettings(panel: SettingsPanel): ActionResult {
        val intent = when (panel) {
            SettingsPanel.Wifi -> SystemIntents.wifi()
            SettingsPanel.Bluetooth -> SystemIntents.bluetooth()
            SettingsPanel.Internet -> SystemIntents.internet()
            SettingsPanel.Airplane -> SystemIntents.airplane()
            SettingsPanel.Display -> SystemIntents.display()
            SettingsPanel.Battery -> SystemIntents.battery()
            SettingsPanel.Sound -> SystemIntents.volumePanel()
            SettingsPanel.Hotspot -> SystemIntents.hotspot()
            SettingsPanel.Device -> SystemIntents.deviceSettings()
        }
        return intent(intent, "Opening ${panel.spoken} settings.", "That settings screen isn't available on this phone.")
    }

    private fun intent(intent: android.content.Intent, ok: String, failed: String): ActionResult =
        if (context.startSafely(intent)) ActionResult.Success(ok) else ActionResult.Failure(failed)

    private fun navigate(route: Any, message: String): ActionResult {
        bus.send(UiCommand.CloseOverlays)
        bus.send(UiCommand.Navigate(route))
        return ActionResult.Success(message)
    }
}

/**
 * Commands that belong to feature areas (music, news, notes, profiles,
 * torch, message replies). Kept separate so the core executor does not
 * depend on every feature.
 */
interface FeatureCommandHandlers {
    suspend fun handle(command: LauncherCommand): ActionResult
}
