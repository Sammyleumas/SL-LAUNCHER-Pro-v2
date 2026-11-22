package com.sllauncher.pro.commands

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CommandParserTest {

    private fun parse(text: String) = CommandParser.parse(text)

    @Test
    fun `every example command from the product brief parses`() {
        assertEquals(LauncherCommand.OpenApp("whatsapp"), parse("Open WhatsApp."))
        assertEquals(LauncherCommand.OpenSettings(SettingsPanel.Device), parse("Open my settings."))
        assertEquals(LauncherCommand.ShowFavorites, parse("Show my favorite apps."))
        assertEquals(LauncherCommand.PlayMusic, parse("Play my music."))
        assertEquals(LauncherCommand.PauseMusic, parse("Pause the music."))
        assertEquals(LauncherCommand.ReadNews, parse("Read the latest news."))
        assertEquals(LauncherCommand.ChangeWallpaper, parse("Change my wallpaper."))
        assertEquals(LauncherCommand.ApplyProfile("Minimal"), parse("Make my home screen minimal."))
        assertEquals(LauncherCommand.OpenDrawer, parse("Open my app drawer."))
        assertEquals(LauncherCommand.ShowNotifications, parse("Show my notifications."))
        assertEquals(LauncherCommand.SetTimer(600), parse("Set a timer for ten minutes."))
        assertEquals(LauncherCommand.OpenSettings(SettingsPanel.Wifi), parse("Turn on Wi-Fi settings."))
        assertEquals(LauncherCommand.NextTrack, parse("next song"))
    }

    @Test
    fun `assistant wake prefixes and politeness are ignored`() {
        assertEquals(LauncherCommand.OpenApp("spotify"), parse("Hey SL, please open Spotify"))
        assertEquals(LauncherCommand.OpenDrawer, parse("SL open the app drawer"))
        assertEquals(LauncherCommand.OpenCamera, parse("can you open the camera"))
    }

    @Test
    fun `timers accept digits, words and combinations`() {
        assertEquals(LauncherCommand.SetTimer(90), parse("timer for 90 seconds"))
        assertEquals(LauncherCommand.SetTimer(5400), parse("set a timer for 1 hour 30 minutes"))
        assertEquals(LauncherCommand.SetTimer(1500), parse("set a twenty five minute timer"))
        assertEquals(LauncherCommand.SetTimer(1800), parse("timer for half an hour"))
        assertEquals(LauncherCommand.SetTimer(300), parse("timer 5 min"))
    }

    @Test
    fun `alarms convert to 24 hour time`() {
        assertEquals(LauncherCommand.SetAlarm(6, 30), parse("set an alarm for 6:30 am"))
        assertEquals(LauncherCommand.SetAlarm(19, 0), parse("alarm at 7 pm"))
        assertEquals(LauncherCommand.SetAlarm(0, 15), parse("alarm 12:15 am"))
    }

    @Test
    fun `settings panels open instead of toggling protected settings`() {
        assertEquals(LauncherCommand.OpenSettings(SettingsPanel.Bluetooth), parse("turn off bluetooth"))
        assertEquals(LauncherCommand.OpenSettings(SettingsPanel.Display), parse("brightness"))
        assertEquals(LauncherCommand.OpenSettings(SettingsPanel.Hotspot), parse("open hotspot settings"))
        assertEquals(LauncherCommand.OpenLauncherSettings, parse("open SL settings"))
    }

    @Test
    fun `free text keeps the user's casing`() {
        assertEquals(LauncherCommand.CreateNote("Call Mum at 6"), parse("Note: Call Mum at 6"))
        assertEquals(LauncherCommand.AddTask("Buy Garri"), parse("remind me to Buy Garri"))
        assertEquals(LauncherCommand.ReplyToLatestMessage("I'm coming"), parse("Reply I'm coming"))
        assertEquals(LauncherCommand.WebSearch("Lagos traffic"), parse("search for Lagos traffic"))
    }

    @Test
    fun `flashlight and profiles`() {
        assertEquals(LauncherCommand.Flashlight(true), parse("turn on the flashlight"))
        assertEquals(LauncherCommand.Flashlight(false), parse("torch off"))
        assertEquals(LauncherCommand.ApplyProfile("Work"), parse("switch to work profile"))
    }

    @Test
    fun `ordinary search text is not a command`() {
        assertNull(parse("whatsapp"))
        assertNull(parse("pizza near me"))
        assertNull(parse(""))
        assertNull(parse("stop"))
    }

    @Test
    fun `read this refers to what is on screen`() {
        assertEquals(LauncherCommand.ReadCurrentItem, parse("read this"))
        assertEquals(LauncherCommand.ReadCurrentItem, parse("Read it to me"))
    }

    @Test
    fun `open play store is an app, not music`() {
        assertEquals(LauncherCommand.OpenApp("play store"), parse("open play store"))
    }

    @Test
    fun `durations format for speech`() {
        assertEquals("10 minutes", formatDuration(600))
        assertEquals("1 hour 30 minutes", formatDuration(5400))
        assertEquals("1 minute 5 seconds", formatDuration(65))
    }

    @Test
    fun `number words`() {
        assertEquals(25, CommandParser.wordsToNumber("twenty-five"))
        assertEquals(10, CommandParser.wordsToNumber("ten"))
        assertNull(CommandParser.wordsToNumber("banana"))
    }
}
