package com.sllauncher.pro.commands

/** Android settings screens SL can open (it never changes them itself). */
enum class SettingsPanel(val spoken: String) {
    Wifi("Wi-Fi"), Bluetooth("Bluetooth"), Internet("internet"), Airplane("airplane mode"),
    Display("display"), Battery("battery"), Sound("sound"), Hotspot("hotspot"), Device("settings"),
}

/**
 * A validated launcher action. Search, the SL assistant and the external
 * assistant bridge all produce these; [LauncherActionExecutor] runs them
 * after permission checks (and confirmation where [needsConfirmation]).
 */
sealed interface LauncherCommand {
    /** Shown to the user before running, and spoken by the assistant. */
    val description: String
    /** Significant changes (theme/profile) are previewed and confirmed first. */
    val needsConfirmation: Boolean get() = false

    data class OpenApp(val appName: String) : LauncherCommand { override val description = "Open $appName" }
    data object OpenDrawer : LauncherCommand { override val description = "Open the app drawer" }
    data object ShowFavorites : LauncherCommand { override val description = "Show favourite apps" }
    data object ShowNotifications : LauncherCommand { override val description = "Show notifications" }
    data object OpenLauncherSettings : LauncherCommand { override val description = "Open SL settings" }
    data class OpenSettings(val panel: SettingsPanel) : LauncherCommand { override val description = "Open ${panel.spoken} settings" }
    data class SetTimer(val seconds: Int, val label: String? = null) : LauncherCommand {
        override val description = "Set a timer for ${formatDuration(seconds)}"
    }
    data class SetAlarm(val hour: Int, val minute: Int) : LauncherCommand {
        override val description = "Set an alarm for %02d:%02d".format(hour, minute)
    }
    data object PlayMusic : LauncherCommand { override val description = "Play music" }
    data object PauseMusic : LauncherCommand { override val description = "Pause music" }
    data object NextTrack : LauncherCommand { override val description = "Next track" }
    data object PreviousTrack : LauncherCommand { override val description = "Previous track" }
    data object ReadNews : LauncherCommand { override val description = "Read the latest headlines" }
    data object OpenNews : LauncherCommand { override val description = "Open news" }
    data object OpenMusic : LauncherCommand { override val description = "Open music" }
    data object ChangeWallpaper : LauncherCommand { override val description = "Change wallpaper" }
    data class ApplyProfile(val profile: String) : LauncherCommand {
        override val description = "Switch home screen to the $profile profile"
        override val needsConfirmation = true
    }
    data class WebSearch(val query: String) : LauncherCommand { override val description = "Search the web for \"$query\"" }
    data class CreateNote(val text: String) : LauncherCommand { override val description = "Save note: $text" }
    data class AddTask(val text: String) : LauncherCommand { override val description = "Add task: $text" }
    data class Flashlight(val on: Boolean) : LauncherCommand { override val description = if (on) "Turn on the flashlight" else "Turn off the flashlight" }
    data class ReplyToLatestMessage(val text: String) : LauncherCommand {
        override val description = "Prepare reply: \"$text\""
        override val needsConfirmation = true
    }
    data object OpenCamera : LauncherCommand { override val description = "Open the camera" }
    /** "Read this": reads whatever the user has open (e.g. a news article). */
    data object ReadCurrentItem : LauncherCommand { override val description = "Read this aloud" }
    data object OpenProductivity : LauncherCommand { override val description = "Open notes and tasks" }
    data object OpenQuickSettings : LauncherCommand { override val description = "Open quick settings" }
}

fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return buildList {
        if (h > 0) add("$h hour${if (h == 1) "" else "s"}")
        if (m > 0) add("$m minute${if (m == 1) "" else "s"}")
        if (s > 0 || (h == 0 && m == 0)) add("$s second${if (s == 1) "" else "s"}")
    }.joinToString(" ")
}

/** What happened when a command ran; the assistant speaks [message]. */
sealed interface ActionResult {
    val message: String

    data class Success(override val message: String) : ActionResult
    /** Needs the user to confirm first (e.g. a profile change). Run again with confirmed = true. */
    data class NeedsConfirmation(override val message: String, val command: LauncherCommand) : ActionResult
    /** A permission or special access is missing; [settingsHint] says where to grant it. */
    data class NeedsPermission(override val message: String, val settingsHint: String) : ActionResult
    data class Failure(override val message: String) : ActionResult
}
