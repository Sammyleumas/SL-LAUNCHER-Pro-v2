package com.sllauncher.pro.common

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Drawer views another feature can ask for. */
enum class DrawerView { All, Favorites, Recent }

/** Requests from features (assistant, search, notifications) to the launcher UI. */
sealed interface UiCommand {
    data class OpenDrawer(val view: DrawerView = DrawerView.All) : UiCommand
    data class OpenSearch(val query: String = "") : UiCommand
    data object CloseOverlays : UiCommand
    data object OpenAssistant : UiCommand
    /** A type-safe navigation route object from `navigation/Destinations.kt`. */
    data class Navigate(val route: Any) : UiCommand
    data class ShowMessage(val text: String) : UiCommand
    /** A distracting app was opened during a focus session; ask before opening it. */
    data class ConfirmFocusLaunch(val key: com.sllauncher.pro.data.apps.AppKey, val label: String) : UiCommand
}

@Singleton
class UiCommandBus @Inject constructor() {
    private val _commands = MutableSharedFlow<UiCommand>(extraBufferCapacity = 16)
    val commands: SharedFlow<UiCommand> = _commands.asSharedFlow()

    fun send(command: UiCommand) {
        _commands.tryEmit(command)
    }
}
