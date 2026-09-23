package dev.keyset.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.keyset.core.model.KeySetSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore("settings")

/** The Settings screen's preferences, saved with DataStore. */
class SettingsStore(context: Context) {
    private val dataStore = context.applicationContext.settingsDataStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val settings: StateFlow<KeySetSettings> = dataStore.data
        .map(::toSettings)
        .stateIn(scope, SharingStarted.Eagerly, KeySetSettings())

    /** Changes settings in the background. */
    fun update(transform: (KeySetSettings) -> KeySetSettings) {
        scope.launch { edit(transform) }
    }

    /** Changes settings and returns once they're saved. */
    suspend fun edit(transform: (KeySetSettings) -> KeySetSettings) {
        dataStore.edit { prefs ->
            val new = transform(toSettings(prefs))
            prefs[BUTTON_STYLE] = new.buttonStyle.name
            prefs[SHOW_SHORTCUT] = new.showShortcut
            prefs[VIBRATE] = new.vibrate
            prefs[KEEP_SCREEN_ON] = new.keepScreenOn
            prefs[SPACING] = new.spacing.name
        }
    }

    private fun toSettings(prefs: Preferences): KeySetSettings {
        val defaults = KeySetSettings()
        return KeySetSettings(
            buttonStyle = enumOrDefault(prefs[BUTTON_STYLE], defaults.buttonStyle),
            showShortcut = prefs[SHOW_SHORTCUT] ?: defaults.showShortcut,
            vibrate = prefs[VIBRATE] ?: defaults.vibrate,
            keepScreenOn = prefs[KEEP_SCREEN_ON] ?: defaults.keepScreenOn,
            spacing = enumOrDefault(prefs[SPACING], defaults.spacing),
        )
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    private companion object {
        val BUTTON_STYLE = stringPreferencesKey("button_style")
        val SHOW_SHORTCUT = booleanPreferencesKey("show_shortcut")
        val VIBRATE = booleanPreferencesKey("vibrate")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val SPACING = stringPreferencesKey("spacing")
    }
}
