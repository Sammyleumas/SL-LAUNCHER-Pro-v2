package com.sllauncher.pro.commands

/**
 * Turns English text ("Set a timer for ten minutes", "Open WhatsApp") into
 * a [LauncherCommand]. Rule-based and fully on-device: no audio or text
 * leaves the phone. Returns null when the text is not a command, so search
 * can treat it as an ordinary query.
 */
object CommandParser {

    private val profileNames = listOf("minimal", "work", "study", "gaming", "personal")

    fun parse(input: String): LauncherCommand? {
        val text = normalize(input)
        if (text.isEmpty()) return null

        // Music control (works on whatever is playing).
        when {
            matches(text, "(play|resume|start)( my| the| some)? music") || text == "play" -> return LauncherCommand.PlayMusic
            matches(text, "(pause|stop)( the| my)? (music|song|playback)") || text == "pause" -> return LauncherCommand.PauseMusic
            matches(text, "(next|skip)( song| track)?|skip (this )?(song|track)") -> return LauncherCommand.NextTrack
            matches(text, "(previous|last|go back a) (song|track)|previous") -> return LauncherCommand.PreviousTrack
        }

        // Timers and alarms.
        Regex("^(?:set (?:a |an )?)?timer (?:for )?(.+)$").find(text)?.let { m ->
            parseDuration(m.groupValues[1])?.let { return LauncherCommand.SetTimer(it) }
        }
        Regex("^(?:set (?:a |an )?)?(.+?) timer$").find(text)?.let { m ->
            parseDuration(m.groupValues[1])?.let { return LauncherCommand.SetTimer(it) }
        }
        Regex("^(?:set (?:an? )?)?alarm (?:for |at )?(\\d{1,2})(?::(\\d{2}))? ?(am|pm)?$").find(text)?.let { m ->
            var hour = m.groupValues[1].toInt()
            val minute = m.groupValues[2].ifEmpty { "0" }.toInt()
            when (m.groupValues[3]) {
                "pm" -> if (hour < 12) hour += 12
                "am" -> if (hour == 12) hour = 0
            }
            if (hour in 0..23 && minute in 0..59) return LauncherCommand.SetAlarm(hour, minute)
        }

        // Launcher surfaces.
        when {
            matches(text, "(open|show)( my| the)? (app drawer|apps|all apps|drawer)") -> return LauncherCommand.OpenDrawer
            matches(text, "(open|show)( me)?( my| the)? (favourite|favorite)s?( apps)?") -> return LauncherCommand.ShowFavorites
            matches(text, "(open|show|check)( me)?( my| the)? notifications?") -> return LauncherCommand.ShowNotifications
            matches(text, "(open|show)( the)? (sl|launcher|sl launcher) settings") -> return LauncherCommand.OpenLauncherSettings
            matches(text, "(read|tell me)( me)?( the)?( latest| today'?s)? (news|headlines)") -> return LauncherCommand.ReadNews
            matches(text, "(open|show)( me)?( the| my)? news") -> return LauncherCommand.OpenNews
            matches(text, "read (this|it)( to me| aloud| out loud)?") -> return LauncherCommand.ReadCurrentItem
            matches(text, "(open|show)( my| the)? music( player)?") -> return LauncherCommand.OpenMusic
            matches(text, "(change|set|pick)( my| the)? wallpaper") -> return LauncherCommand.ChangeWallpaper
            matches(text, "(open|show)( my)? (notes|tasks|to ?do|productivity)") -> return LauncherCommand.OpenProductivity
            matches(text, "(open|show)( the)? quick settings") -> return LauncherCommand.OpenQuickSettings
            matches(text, "(open|start|launch)( the)? camera|take a (photo|picture)") -> return LauncherCommand.OpenCamera
            matches(text, "turn (on|off)( the)? (flashlight|torch)|(flashlight|torch) (on|off)") ->
                return LauncherCommand.Flashlight(on = Regex("\\bon\\b").containsMatchIn(text))
        }

        // "Make my home screen minimal", "switch to work profile".
        Regex("^(?:make|set|switch|change)(?: my)?(?: home ?screen| launcher| home)?(?: to)?(?: the)? (\\w+)(?: mode| profile| theme)?$").find(text)?.let { m ->
            val name = m.groupValues[1]
            if (name in profileNames) return LauncherCommand.ApplyProfile(name.replaceFirstChar { it.uppercase() })
        }

        // System settings panels. "Turn on Wi-Fi" opens the Wi-Fi panel: Android does not let apps toggle it.
        Regex("^(?:open|show|turn on|turn off|go to|enable|disable)?(?: my| the)? ?(wi ?fi|wireless|bluetooth|internet|mobile data|data|airplane mode|flight mode|display|brightness|battery|sound|volume|hotspot|settings)(?: settings| panel)?$")
            .find(text)?.let { m ->
                val panel = when (m.groupValues[1].replace(" ", "")) {
                    "wifi", "wireless" -> SettingsPanel.Wifi
                    "bluetooth" -> SettingsPanel.Bluetooth
                    "internet", "mobiledata", "data" -> SettingsPanel.Internet
                    "airplanemode", "flightmode" -> SettingsPanel.Airplane
                    "display", "brightness" -> SettingsPanel.Display
                    "battery" -> SettingsPanel.Battery
                    "sound", "volume" -> SettingsPanel.Sound
                    "hotspot" -> SettingsPanel.Hotspot
                    else -> SettingsPanel.Device
                }
                return LauncherCommand.OpenSettings(panel)
            }

        // Notes, tasks, replies, web search.
        Regex("^(?:take a |make a |new |add a )?note(?: that)?[:,]? (.+)$").find(text)?.let { return LauncherCommand.CreateNote(original(input, it.groupValues[1])) }
        Regex("^(?:add (?:a )?task|remind me to|add to my (?:list|tasks|to ?do))[:,]? (.+)$").find(text)?.let { return LauncherCommand.AddTask(original(input, it.groupValues[1])) }
        Regex("^reply(?: to (?:the |my )?(?:last |latest )?message)?(?: with| saying| that)?[:,]? (.+)$").find(text)?.let {
            return LauncherCommand.ReplyToLatestMessage(original(input, it.groupValues[1]))
        }
        Regex("^(?:search(?: the web| google)?(?: for)?|google|look up) (.+)$").find(text)?.let { return LauncherCommand.WebSearch(original(input, it.groupValues[1])) }

        // "Open WhatsApp" - last, so "open settings" etc. match first.
        Regex("^(?:open|launch|start|run)(?: up)?(?: my| the)? (.+?)(?: app)?$").find(text)?.let { return LauncherCommand.OpenApp(it.groupValues[1]) }

        return null
    }

    /** "ten minutes", "1 hour 30 minutes", "90 seconds", "5 min", "half an hour" -> seconds. */
    fun parseDuration(raw: String): Int? {
        val text = normalize(raw).replace("half an hour", "30 minutes").replace("an hour", "1 hour").replace(" and ", " ")
        val unit = Regex("(\\d+|$numberWordPattern) ?(hours?|hrs?|h|minutes?|mins?|m|seconds?|secs?|s)\\b")
        var total = 0
        var found = false
        for (m in unit.findAll(text)) {
            val n = m.groupValues[1].toIntOrNull() ?: wordsToNumber(m.groupValues[1]) ?: continue
            total += when (m.groupValues[2].first()) {
                'h' -> n * 3600
                'm' -> n * 60
                else -> n
            }
            found = true
        }
        return if (found && total in 1..(24 * 3600)) total else null
    }

    private val units = mapOf(
        "a" to 1, "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5, "six" to 6, "seven" to 7,
        "eight" to 8, "nine" to 9, "ten" to 10, "eleven" to 11, "twelve" to 12, "thirteen" to 13, "fourteen" to 14,
        "fifteen" to 15, "sixteen" to 16, "seventeen" to 17, "eighteen" to 18, "nineteen" to 19,
    )
    private val tens = mapOf("twenty" to 20, "thirty" to 30, "forty" to 40, "fifty" to 50, "sixty" to 60)

    /** Only real number words, longest first, so "ten minutes" is not read as the number "ten minute". */
    private val numberWordPattern: String by lazy {
        val ones = units.keys.filter { units.getValue(it) < 10 }.sortedByDescending { it.length }.joinToString("|")
        val tensAlt = tens.keys.joinToString("|")
        val singles = units.keys.sortedByDescending { it.length }.joinToString("|")
        "(?:(?:$tensAlt)(?:[ -](?:$ones))?|$singles)"
    }

    /** "twenty five" / "twenty-five" / "ten" -> number. */
    fun wordsToNumber(words: String): Int? {
        val parts = words.trim().split(Regex("[ -]+"))
        return when (parts.size) {
            1 -> units[parts[0]] ?: tens[parts[0]]
            2 -> { val t = tens[parts[0]]; val u = units[parts[1]]; if (t != null && u != null && u < 10) t + u else null }
            else -> null
        }
    }

    private fun normalize(text: String): String =
        text.trim().lowercase()
            .replace(Regex("[.!?]+$"), "")
            .replace("wi-fi", "wifi")
            .replace(Regex("^(hey |ok |okay )?sl[, ]+"), "")
            .replace(Regex("^(please |can you |could you |would you |i want to |i'd like to )"), "")
            .replace(Regex(" please$"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun matches(text: String, pattern: String) = Regex("^(?:$pattern)$").matches(text)

    /** Recovers the user's original casing for free text such as note bodies. */
    private fun original(input: String, lowered: String): String {
        val index = input.lowercase().indexOf(lowered)
        return if (index >= 0) input.substring(index, index + lowered.length).trim() else lowered
    }
}
