package com.cleanshot.app.ui.theme

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThemeViewModel(
    application: Application,
    initialSettings: ThemeSettings
) : AndroidViewModel(application) {
    private val themePreferences = ThemePreferences(application)

    val themeSettings: StateFlow<ThemeSettings> = themePreferences.themeSettingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = initialSettings
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

    fun setColorPreset(preset: ColorPreset) {
        viewModelScope.launch {
            themePreferences.saveColorPreset(preset)
            themePreferences.saveDynamicColors(false)
        }
    }

    fun setAmoledMode(enabled: Boolean) {
        viewModelScope.launch {
            themePreferences.saveAmoledMode(enabled)
        }
    }

    companion object {
        fun factory(
            application: Application,
            initialSettings: ThemeSettings
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(ThemeViewModel::class.java)) {
                        "Unknown ViewModel class: ${modelClass.name}"
                    }
                    return ThemeViewModel(application, initialSettings) as T
                }
            }
        }
    }
}
