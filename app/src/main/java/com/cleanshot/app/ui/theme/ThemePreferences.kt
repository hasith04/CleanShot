package com.cleanshot.app.ui.theme

import android.content.Context
import android.content.res.Configuration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_prefs")

private val THEME_KEY = stringPreferencesKey("selected_theme")
private val DYNAMIC_COLORS_KEY = booleanPreferencesKey("dynamic_colors")
private val AMOLED_MODE_KEY = booleanPreferencesKey("amoled_mode")

data class ThemeSettings(
    val theme: AppTheme,
    val useDynamicColors: Boolean,
    val useAmoledMode: Boolean
)

class ThemePreferences(private val context: Context) {
    companion object {
        /** Reads persisted theme before Compose draws (cold start only). */
        fun readBlocking(context: Context): ThemeSettings {
            return runBlocking(Dispatchers.IO) {
                context.dataStore.data.first().toThemeSettings()
            }
        }

        fun resolveDarkTheme(settings: ThemeSettings, configuration: Configuration): Boolean {
            return when (settings.theme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> {
                    (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                        Configuration.UI_MODE_NIGHT_YES
                }
            }
        }
    }

    val themeSettingsFlow: Flow<ThemeSettings> = context.dataStore.data.map { prefs ->
        prefs.toThemeSettings()
    }

    suspend fun saveTheme(theme: AppTheme) {
        context.dataStore.edit { prefs ->
            prefs[THEME_KEY] = theme.name
        }
    }

    suspend fun saveDynamicColors(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[DYNAMIC_COLORS_KEY] = enabled
        }
    }

    suspend fun saveAmoledMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[AMOLED_MODE_KEY] = enabled
        }
    }
}

private fun Preferences.toThemeSettings(): ThemeSettings {
    return ThemeSettings(
        theme = try {
            AppTheme.valueOf(this[THEME_KEY] ?: AppTheme.SYSTEM.name)
        } catch (_: Exception) {
            AppTheme.SYSTEM
        },
        useDynamicColors = this[DYNAMIC_COLORS_KEY] ?: true,
        useAmoledMode = this[AMOLED_MODE_KEY] ?: false
    )
}
