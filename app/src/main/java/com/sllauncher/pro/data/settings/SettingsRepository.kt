package com.sllauncher.pro.data.settings

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sllauncher.pro.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SettingsRepository"
private val SettingsKey = stringPreferencesKey("launcher_settings_json")

/**
 * Reads and writes [LauncherSettings]. A corrupt or unreadable document
 * falls back to defaults instead of crashing the launcher.
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
    @ApplicationScope scope: CoroutineScope,
) {
    /** Null until the first read finishes, so the UI can avoid flashing defaults. */
    val settingsOrNull: StateFlow<LauncherSettings?> = dataStore.data
        .map { prefs -> decode(prefs[SettingsKey]) }
        .stateIn(scope, SharingStarted.Eagerly, null)

    val settings: Flow<LauncherSettings> = dataStore.data.map { decode(it[SettingsKey]) }

    /** Latest known settings (defaults before the first read). */
    val current: LauncherSettings get() = settingsOrNull.value ?: LauncherSettings()

    suspend fun update(transform: (LauncherSettings) -> LauncherSettings) {
        dataStore.edit { prefs ->
            val next = transform(decode(prefs[SettingsKey]))
            prefs[SettingsKey] = json.encodeToString(LauncherSettings.serializer(), next)
        }
    }

    suspend fun replace(settings: LauncherSettings) = update { settings }

    fun encode(settings: LauncherSettings): String = json.encodeToString(LauncherSettings.serializer(), settings)

    fun decodeOrNull(raw: String): LauncherSettings? = try {
        json.decodeFromString(LauncherSettings.serializer(), raw)
    } catch (e: IllegalArgumentException) {
        Log.w(TAG, "Unreadable settings document", e)
        null
    }

    private fun decode(raw: String?): LauncherSettings =
        if (raw == null) LauncherSettings() else decodeOrNull(raw) ?: LauncherSettings()
}
