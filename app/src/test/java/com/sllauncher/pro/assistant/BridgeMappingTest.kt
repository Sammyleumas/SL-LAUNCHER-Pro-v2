package com.sllauncher.pro.assistant

import com.sllauncher.pro.commands.LauncherCommand
import com.sllauncher.pro.commands.SettingsPanel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BridgeMappingTest {

    @Test
    fun `structured actions map to commands`() {
        assertEquals(LauncherCommand.OpenApp("WhatsApp"), commandForAction("open_app", mapOf("name" to "WhatsApp")))
        assertEquals(LauncherCommand.SetTimer(600, null), commandForAction("set_timer", mapOf("seconds" to "600")))
        assertEquals(LauncherCommand.OpenSettings(SettingsPanel.Wifi), commandForAction("open_settings", mapOf("panel" to "wifi")))
        assertEquals(LauncherCommand.Flashlight(true), commandForAction("flashlight", mapOf("on" to "true")))
        assertEquals(LauncherCommand.PlayMusic, commandForAction("play_music", emptyMap()))
    }

    @Test
    fun `free text goes through the same parser as the launcher`() {
        assertEquals(LauncherCommand.SetTimer(300), commandForAction("text_command", mapOf("text" to "set a timer for 5 minutes")))
    }

    @Test
    fun `invalid parameters are rejected, never executed`() {
        assertThrows(InvalidActionException::class.java) { commandForAction("open_app", emptyMap()) }
        assertThrows(InvalidActionException::class.java) { commandForAction("set_timer", mapOf("seconds" to "-5")) }
        assertThrows(InvalidActionException::class.java) { commandForAction("set_timer", mapOf("seconds" to "ten")) }
        assertThrows(InvalidActionException::class.java) { commandForAction("set_alarm", mapOf("hour" to "25", "minute" to "0")) }
        assertThrows(InvalidActionException::class.java) { commandForAction("open_settings", mapOf("panel" to "kernel")) }
        assertThrows(InvalidActionException::class.java) { commandForAction("flashlight", mapOf("on" to "maybe")) }
        assertThrows(InvalidActionException::class.java) { commandForAction("delete_everything", emptyMap()) }
        assertThrows(InvalidActionException::class.java) { commandForAction("create_note", mapOf("text" to "x".repeat(2_001))) }
    }
}
