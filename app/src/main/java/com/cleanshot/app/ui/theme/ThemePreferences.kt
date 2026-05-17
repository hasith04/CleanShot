package com.cleanshot.app.ui.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_prefs")

data class ThemeSettings(
    val theme: AppTheme,
    val useDynamicColors: Boolean,
    val useAmoledMode: Boolean
)

class ThemePreferences(private val context: Context) {
    companion object {
        private val THEME_KEY = stringPreferencesKey("selected_theme")
        private val DYNAMIC_COLORS_KEY = booleanPreferencesKey("dynamic_colors")
        private val AMOLED_MODE_KEY = booleanPreferencesKey("amoled_mode")
    }

    val themeSettingsFlow: Flow<ThemeSettings> = context.dataStore.data.map { prefs ->
        ThemeSettings(
            theme = try {
                AppTheme.valueOf(prefs[THEME_KEY] ?: AppTheme.SYSTEM.name)
            } catch (e: Exception) {
                AppTheme.SYSTEM
            },
            useDynamicColors = prefs[DYNAMIC_COLORS_KEY] ?: true,
            useAmoledMode = prefs[AMOLED_MODE_KEY] ?: false
        )
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
