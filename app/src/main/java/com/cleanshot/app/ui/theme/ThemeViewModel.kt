package com.cleanshot.app.ui.theme

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val themePreferences = ThemePreferences(application)

    val themeSettings: StateFlow<ThemeSettings> = themePreferences.themeSettingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeSettings(AppTheme.SYSTEM, true, false)
        )

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            themePreferences.saveTheme(theme)
        }
    }

    fun setDynamicColors(enabled: Boolean) {
        viewModelScope.launch {
            themePreferences.saveDynamicColors(enabled)
        }
    }

    fun setAmoledMode(enabled: Boolean) {
        viewModelScope.launch {
            themePreferences.saveAmoledMode(enabled)
        }
    }
}
