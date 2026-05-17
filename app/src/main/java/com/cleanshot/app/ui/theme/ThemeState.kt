package com.cleanshot.app.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object ThemeState {
    var currentTheme by mutableStateOf(ThemeMode.SYSTEM)
    var useDynamicColors by mutableStateOf(true)
    var useAmoledMode by mutableStateOf(false)
}
