package com.cleanshot.app.ui.theme

import android.app.Activity
import com.cleanshot.app.R

/**
 * Applies splash + post-splash window themes from saved preferences before [Activity.onCreate].
 */
fun Activity.applyLaunchTheme(settings: ThemeSettings) {
    val isDark = ThemePreferences.resolveDarkTheme(settings, resources.configuration)
    val splashThemeRes = when {
        isDark && settings.useAmoledMode -> R.style.Theme_CleanShot_Splash_Amoled
        isDark -> R.style.Theme_CleanShot_Splash_Dark
        else -> R.style.Theme_CleanShot_Splash_Light
    }
    setTheme(splashThemeRes)
}
