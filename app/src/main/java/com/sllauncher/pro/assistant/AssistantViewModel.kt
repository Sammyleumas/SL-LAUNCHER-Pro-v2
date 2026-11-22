package com.sllauncher.pro.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sllauncher.pro.commands.ActionResult
import com.sllauncher.pro.commands.CommandParser
import com.sllauncher.pro.commands.LauncherActionExecutor
import com.sllauncher.pro.commands.LauncherCommand
import com.sllauncher.pro.data.apps.AppRepository
import com.sllauncher.pro.data.settings.SettingsRepository
import com.sllauncher.pro.drawer.normalizeForSearch
import com.sllauncher.core.designsystem.component.SLAssistantState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class Sender { User, SL }

data class ChatMessage(
    val id: Long,
    val from: Sender,
    val text: String,
    /** Set when SL is asking the user to confirm a command. */
    val confirm: LauncherCommand? = null,
    /** Set when a permission or setting is needed. */
    val settingsHint: String? = null,
)

data class AssistantUiState(
    val state: SLAssistantState = SLAssistantState.Idle,
    val partial: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val voiceAvailable: Boolean = true,
    val onDeviceVoice: Boolean = false,
)

private const val HELP = "I can open apps, control music, set timers and alarms, open settings panels, read the news, take notes, add tasks, " +
    "reply to messages and switch your home screen profile. Try \"set a timer for 10 minutes\" or \"open WhatsApp\"."

@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val executor: LauncherActionExecutor,
    private val speaker: Speaker,
    private val voice: VoiceInput,
    private val settings: SettingsRepository,
    private val apps: AppRepository,
) : ViewModel() {

    private var nextId = 0L
    private val _ui = MutableStateFlow(AssistantUiState(voiceAvailable = voice.isAvailable, onDeviceVoice = voice.isOnDevice))
    val ui: StateFlow<AssistantUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            speaker.speaking.collect { talking ->
                _ui.update { if (talking) it.copy(state = SLAssistantState.Speaking) else if (it.state == SLAssistantState.Speaking) it.copy(state = SLAssistantState.Idle) else it }
            }
        }
        if (_ui.value.messages.isEmpty()) add(Sender.SL, "Hi, I'm SL. Tap the button and speak, or type below.")
    }

    /** Main button: start listening, or interrupt whatever SL is doing. */
    fun onButton(hasMicPermission: Boolean) {
        when (_ui.value.state) {
            SLAssistantState.Listening -> stopListening()
            SLAssistantState.Speaking -> speaker.stop()
            SLAssistantState.Processing -> Unit
            SLAssistantState.Idle -> if (hasMicPermission) startListening() else add(Sender.SL, "I need microphone access to hear you. You can also type.")
        }
    }

    fun startListening() {
        if (!voice.isAvailable) {
            add(Sender.SL, "Voice input isn't available on this phone. You can type instead.")
            return
        }
        speaker.stop()
        _ui.update { it.copy(state = SLAssistantState.Listening, partial = "") }
        voice.start { event ->
            when (event) {
                is VoiceEvent.Partial -> _ui.update { it.copy(partial = event.text) }
                is VoiceEvent.Final -> { _ui.update { it.copy(partial = "") }; submit(event.text) }
                is VoiceEvent.Error -> { _ui.update { it.copy(state = SLAssistantState.Idle, partial = "") }; reply(event.message) }
                else -> Unit
            }
        }
    }

    fun stopListening() {
        voice.stop()
        _ui.update { it.copy(state = SLAssistantState.Idle, partial = "") }
    }

    /** Handles typed or spoken text. */
    fun submit(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        add(Sender.User, trimmed)
        _ui.update { it.copy(state = SLAssistantState.Processing) }
        viewModelScope.launch {
            val command = CommandParser.parse(trimmed) ?: appNamed(trimmed)?.let { LauncherCommand.OpenApp(it) }
            if (command == null) {
                reply(
                    if (normalizeForSearch(trimmed).let { it == "help" || it.startsWith("what can you") }) HELP
                    else "Sorry, I can't do that yet. $HELP",
                )
                return@launch
            }
            handle(executor.execute(command))
        }
    }

    fun confirm(command: LauncherCommand) = viewModelScope.launch {
        clearConfirmations()
        _ui.update { it.copy(state = SLAssistantState.Processing) }
        handle(executor.execute(command, confirmed = true))
    }

    fun decline() {
        clearConfirmations()
        reply("OK, I won't change anything.")
    }

    fun interrupt() {
        speaker.stop()
        voice.stop()
        _ui.update { it.copy(state = SLAssistantState.Idle, partial = "") }
    }

    private fun handle(result: ActionResult) {
        when (result) {
            is ActionResult.NeedsConfirmation -> {
                add(Sender.SL, result.message, confirm = result.command)
                _ui.update { it.copy(state = SLAssistantState.Idle) }
                speak(result.message)
            }
            is ActionResult.NeedsPermission -> {
                add(Sender.SL, result.message, settingsHint = result.settingsHint)
                _ui.update { it.copy(state = SLAssistantState.Idle) }
                speak(result.message)
            }
            else -> reply(result.message)
        }
    }

    /** "WhatsApp" on its own opens WhatsApp when it exactly matches an app name. */
    private fun appNamed(text: String): String? {
        val n = normalizeForSearch(text)
        return apps.entries.value.firstOrNull { !it.hidden && normalizeForSearch(it.app.label) == n }?.app?.label
    }

    private fun reply(text: String) {
        add(Sender.SL, text)
        _ui.update { it.copy(state = SLAssistantState.Idle) }
        speak(text)
    }

    private fun speak(text: String) {
        if (settings.current.assistantSpeakReplies) speaker.speak(text)
    }

    private fun clearConfirmations() = _ui.update { s -> s.copy(messages = s.messages.map { it.copy(confirm = null) }) }

    private fun add(from: Sender, text: String, confirm: LauncherCommand? = null, settingsHint: String? = null) {
        _ui.update { it.copy(messages = (it.messages + ChatMessage(nextId++, from, text, confirm, settingsHint)).takeLast(40)) }
    }

    override fun onCleared() {
        voice.stop()
        speaker.stop()
    }
}
