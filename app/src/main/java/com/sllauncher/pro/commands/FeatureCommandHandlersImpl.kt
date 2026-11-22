package com.sllauncher.pro.commands

import com.sllauncher.pro.common.UiCommand
import com.sllauncher.pro.common.UiCommandBus
import com.sllauncher.pro.data.db.NoteDao
import com.sllauncher.pro.data.db.NoteEntity
import com.sllauncher.pro.data.db.TaskDao
import com.sllauncher.pro.data.db.TaskEntity
import com.sllauncher.pro.device.TorchController
import com.sllauncher.pro.media.MediaSessionRepository
import com.sllauncher.pro.music.MusicController
import com.sllauncher.pro.navigation.MusicDestination
import com.sllauncher.pro.navigation.NewsDestination
import com.sllauncher.pro.navigation.ProductivityDestination
import com.sllauncher.pro.navigation.QuickSettingsDestination
import com.sllauncher.pro.news.CurrentArticleHolder
import com.sllauncher.pro.news.NewsRepository
import com.sllauncher.pro.notifications.NotificationRepository
import com.sllauncher.pro.notifications.ReplyResult
import com.sllauncher.pro.settings.ProfileManager
import javax.inject.Inject
import javax.inject.Singleton

/** Routes feature commands (music, news, notes, profiles, torch, replies) to their feature areas. */
@Singleton
class FeatureCommandHandlersImpl @Inject constructor(
    private val bus: UiCommandBus,
    private val sessions: MediaSessionRepository,
    private val music: MusicController,
    private val news: NewsRepository,
    private val currentArticle: CurrentArticleHolder,
    private val notes: NoteDao,
    private val tasks: TaskDao,
    private val profiles: ProfileManager,
    private val torch: TorchController,
    private val notifications: NotificationRepository,
) : FeatureCommandHandlers {

    override suspend fun handle(command: LauncherCommand): ActionResult = when (command) {
        LauncherCommand.PlayMusic -> when {
            sessions.hasSession && sessions.play() -> ActionResult.Success("Playing.")
            music.state.value.hasMedia -> { music.resume(); ActionResult.Success("Playing.") }
            else -> { navigate(MusicDestination); ActionResult.Success("Pick something to play.") }
        }
        LauncherCommand.PauseMusic -> when {
            sessions.nowPlaying.value?.isPlaying == true -> { sessions.pause(); ActionResult.Success("Paused.") }
            music.state.value.isPlaying -> { music.pause(); ActionResult.Success("Paused.") }
            else -> ActionResult.Failure("Nothing is playing.")
        }
        LauncherCommand.NextTrack -> if (sessions.next()) ActionResult.Success("Next track.") else ActionResult.Failure("Nothing is playing.")
        LauncherCommand.PreviousTrack -> if (sessions.previous()) ActionResult.Success("Previous track.") else ActionResult.Failure("Nothing is playing.")
        LauncherCommand.ReadNews -> {
            val headlines = news.headlinesForSpeech(5)
            if (headlines.isEmpty()) {
                ActionResult.Failure("I don't have any headlines yet. Open News and tap Refresh.")
            } else {
                ActionResult.Success("Here are the latest headlines. " + headlines.joinToString(" ") { "From ${it.publisher}: ${it.title}." })
            }
        }
        LauncherCommand.ReadCurrentItem -> currentArticle.article?.let {
            ActionResult.Success("${it.title}. From ${it.publisher}. ${it.summary}")
        } ?: ActionResult.Failure("Open a news story first, then ask me to read it.")
        LauncherCommand.OpenNews -> navigate(NewsDestination)
        LauncherCommand.OpenMusic -> navigate(MusicDestination)
        LauncherCommand.OpenProductivity -> navigate(ProductivityDestination)
        LauncherCommand.OpenQuickSettings -> navigate(QuickSettingsDestination)
        is LauncherCommand.CreateNote -> {
            notes.upsert(NoteEntity(title = command.text.take(40), body = command.text, updatedAt = System.currentTimeMillis()))
            ActionResult.Success("Note saved.")
        }
        is LauncherCommand.AddTask -> {
            tasks.upsert(TaskEntity(title = command.text, createdAt = System.currentTimeMillis()))
            ActionResult.Success("Added to your tasks.")
        }
        is LauncherCommand.ApplyProfile -> if (profiles.apply(command.profile)) {
            ActionResult.Success("Done. Your home screen now uses the ${command.profile} profile. You can undo this in Customize.")
        } else {
            ActionResult.Failure("I don't know a profile called ${command.profile}.")
        }
        is LauncherCommand.Flashlight -> when {
            !torch.available -> ActionResult.Failure("This phone has no flashlight.")
            torch.set(command.on) -> ActionResult.Success(if (command.on) "Flashlight on." else "Flashlight off.")
            else -> ActionResult.Failure("The flashlight is in use by another app.")
        }
        is LauncherCommand.ReplyToLatestMessage -> {
            val target = notifications.latestReplyable()
            when {
                !notifications.hasAccess() -> ActionResult.NeedsPermission("I need notification access to reply to messages.", "Settings › Notifications › Notification access")
                target == null -> ActionResult.Failure("There's no message I can reply to. Some apps don't allow replies from outside the app.")
                else -> when (notifications.reply(target, command.text)) {
                    ReplyResult.Sent -> ActionResult.Success("Sent to ${target.conversationTitle ?: target.title}.")
                    ReplyResult.NotSupported -> ActionResult.Failure("${target.appLabel} doesn't allow quick replies.")
                    ReplyResult.Failed -> ActionResult.Failure("Couldn't send. Open ${target.appLabel} to reply.")
                }
            }
        }
        else -> ActionResult.Failure("${command.description} isn't supported.")
    }

    private fun navigate(route: Any): ActionResult {
        bus.send(UiCommand.CloseOverlays)
        bus.send(UiCommand.Navigate(route))
        return ActionResult.Success("Opening.")
    }
}
