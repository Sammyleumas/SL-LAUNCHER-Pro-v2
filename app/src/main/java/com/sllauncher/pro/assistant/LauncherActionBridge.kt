package com.sllauncher.pro.assistant

import com.sllauncher.pro.commands.ActionResult
import com.sllauncher.pro.commands.CommandParser
import com.sllauncher.pro.commands.LauncherActionExecutor
import com.sllauncher.pro.commands.LauncherCommand
import com.sllauncher.pro.commands.SettingsPanel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Contract between the SL assistant app and the launcher (see
 * docs/ASSISTANT-BRIDGE.md). Actions are names with string parameters;
 * every parameter is validated before anything runs.
 */
interface LauncherActionBridge {
    suspend fun execute(action: String, parameters: Map<String, String>): ActionResult
}

/** Thrown for unknown actions or invalid parameters; reported back as a failure, never executed. */
class InvalidActionException(message: String) : IllegalArgumentException(message)

/** Maps a bridge action to a [LauncherCommand]. Pure, so it is unit-tested. */
fun commandForAction(action: String, p: Map<String, String>): LauncherCommand {
    fun req(key: String): String = p[key]?.trim()?.takeIf { it.isNotEmpty() } ?: throw InvalidActionException("Missing parameter \"$key\" for $action")
    fun text(key: String, max: Int = 500): String = req(key).also { if (it.length > max) throw InvalidActionException("\"$key\" is longer than $max characters") }
    fun int(key: String, range: IntRange): Int = req(key).toIntOrNull()?.takeIf { it in range } ?: throw InvalidActionException("\"$key\" must be a number in $range")
    fun bool(key: String): Boolean = when (req(key).lowercase()) {
        "true", "on", "1" -> true
        "false", "off", "0" -> false
        else -> throw InvalidActionException("\"$key\" must be true or false")
    }

    return when (action) {
        "open_app" -> LauncherCommand.OpenApp(text("name", 80))
        "open_drawer" -> LauncherCommand.OpenDrawer
        "show_favorites" -> LauncherCommand.ShowFavorites
        "show_notifications" -> LauncherCommand.ShowNotifications
        "open_settings" -> LauncherCommand.OpenSettings(
            SettingsPanel.entries.firstOrNull { it.name.equals(req("panel"), ignoreCase = true) }
                ?: throw InvalidActionException("Unknown panel. Use one of ${SettingsPanel.entries.joinToString { it.name.lowercase() }}"),
        )
        "set_timer" -> LauncherCommand.SetTimer(int("seconds", 1..86_400), p["label"]?.take(60))
        "set_alarm" -> LauncherCommand.SetAlarm(int("hour", 0..23), int("minute", 0..59))
        "play_music" -> LauncherCommand.PlayMusic
        "pause_music" -> LauncherCommand.PauseMusic
        "next_track" -> LauncherCommand.NextTrack
        "previous_track" -> LauncherCommand.PreviousTrack
        "read_news" -> LauncherCommand.ReadNews
        "read_current" -> LauncherCommand.ReadCurrentItem
        "change_wallpaper" -> LauncherCommand.ChangeWallpaper
        "apply_profile" -> LauncherCommand.ApplyProfile(text("name", 24))
        "web_search" -> LauncherCommand.WebSearch(text("query", 200))
        "create_note" -> LauncherCommand.CreateNote(text("text", 2_000))
        "add_task" -> LauncherCommand.AddTask(text("text", 200))
        "flashlight" -> LauncherCommand.Flashlight(bool("on"))
        "reply" -> LauncherCommand.ReplyToLatestMessage(text("text", 500))
        "open_camera" -> LauncherCommand.OpenCamera
        "text_command" -> CommandParser.parse(text("text", 300)) ?: throw InvalidActionException("I couldn't understand that request.")
        else -> throw InvalidActionException("Unknown action \"$action\"")
    }
}

@Singleton
class DefaultLauncherActionBridge @Inject constructor(
    private val executor: LauncherActionExecutor,
) : LauncherActionBridge {

    /**
     * Runs a validated action. Commands that need confirmation (profile
     * changes, message replies) only run when parameters include
     * "confirmed" = "true", meaning the SL app already asked the user.
     */
    override suspend fun execute(action: String, parameters: Map<String, String>): ActionResult {
        val command = try {
            commandForAction(action, parameters)
        } catch (e: InvalidActionException) {
            return ActionResult.Failure(e.message ?: "Invalid action")
        }
        return executor.execute(command, confirmed = parameters["confirmed"] == "true")
    }
}
