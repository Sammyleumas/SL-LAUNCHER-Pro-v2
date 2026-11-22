package com.sllauncher.pro.drawer

import com.sllauncher.pro.data.apps.AppCategory
import com.sllauncher.pro.data.apps.AppEntry
import com.sllauncher.pro.data.settings.DrawerSort
import java.text.Normalizer

/** What the drawer is showing. */
sealed interface DrawerFilter {
    data object All : DrawerFilter
    data object Favorites : DrawerFilter
    data object Recent : DrawerFilter
    data object Hidden : DrawerFilter
    data class Category(val category: AppCategory) : DrawerFilter
    data class Custom(val id: Long, val name: String) : DrawerFilter
}

/** Lower-case, accent-free text so "Créer" matches "creer". */
fun normalizeForSearch(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase()

/**
 * How well [label] matches [query]: 3 = starts with it, 2 = a word starts
 * with it, 1 = word initials match ("wa" matches "Whats App"), 0.5 =
 * contains it anywhere, 0 = no match.
 */
fun matchScore(label: String, query: String): Double {
    val q = normalizeForSearch(query.trim())
    if (q.isEmpty()) return 1.0
    val l = normalizeForSearch(label)
    if (l.startsWith(q)) return 3.0
    val words = l.split(Regex("[\\s\\-_.]+")).filter { it.isNotEmpty() }
    if (words.any { it.startsWith(q) }) return 2.0
    if (q.length >= 2 && words.joinToString("") { it.take(1) }.startsWith(q)) return 1.0
    if (l.contains(q)) return 0.5
    return 0.0
}

/**
 * Apps to show for [filter] and [query], in display order. Hidden apps
 * appear only in [DrawerFilter.Hidden] (or when searched by exact name,
 * so a hidden app is never truly lost).
 */
fun filterApps(
    entries: List<AppEntry>,
    filter: DrawerFilter,
    query: String,
    sort: DrawerSort,
    recentLimit: Int = 12,
): List<AppEntry> {
    val searching = query.isNotBlank()
    val visible = entries.filter { entry ->
        when (filter) {
            DrawerFilter.Hidden -> entry.hidden
            else -> !entry.hidden || (searching && normalizeForSearch(entry.app.label) == normalizeForSearch(query.trim()))
        }
    }
    val inFilter = when (filter) {
        DrawerFilter.All, DrawerFilter.Hidden -> visible
        DrawerFilter.Favorites -> visible.filter { it.favorite }
        DrawerFilter.Recent -> visible.filter { it.lastLaunchedAt > 0 }.sortedByDescending { it.lastLaunchedAt }.take(recentLimit)
        is DrawerFilter.Category -> visible.filter { it.customCategoryId == null && it.category == filter.category }
        is DrawerFilter.Custom -> visible.filter { it.customCategoryId == filter.id }
    }
    if (searching) {
        return inFilter
            .map { it to matchScore(it.app.label, query) }
            .filter { it.second > 0 }
            .sortedWith(compareByDescending<Pair<AppEntry, Double>> { it.second }.thenByDescending { it.first.launchCount })
            .map { it.first }
    }
    if (filter == DrawerFilter.Recent) return inFilter
    return when (sort) {
        DrawerSort.Name -> inFilter // Repository already sorts by label with the device's collator.
        DrawerSort.MostUsed -> inFilter.sortedByDescending { it.launchCount }
        DrawerSort.RecentlyInstalled -> inFilter.sortedByDescending { it.app.firstInstallTime }
    }
}

/** First letter used by the alphabet index; digits and symbols group under "#". */
fun indexLetter(label: String): Char {
    val c = normalizeForSearch(label).firstOrNull()?.uppercaseChar() ?: '#'
    return if (c in 'A'..'Z') c else '#'
}

/** Built-in categories that actually contain apps, in a stable order. */
fun nonEmptyCategories(entries: List<AppEntry>): List<AppCategory> {
    val present = entries.filter { !it.hidden && it.customCategoryId == null }.map { it.category }.toSet()
    return AppCategory.entries.filter { it != AppCategory.Other && it in present }
}
