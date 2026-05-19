package com.cleanshot.app.ui.theme

import android.app.Activity
import android.content.Context
import android.graphics.drawable.ColorDrawable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Latest theme settings for [androidx.activity.SystemBarStyle] dark-mode detection.
 */
object ThemeRuntime {
    @Volatile
    var settings: ThemeSettings = ThemeSettings(
        theme = AppTheme.SYSTEM,
        useDynamicColors = true,
        useAmoledMode = false
    )
}

fun Context.themeBackgroundColor(settings: ThemeSettings): Color {
    val dark = ThemePreferences.resolveDarkTheme(settings, resources.configuration)
    return resolveColorScheme(
        context = this,
        darkTheme = dark,
        useDynamicColors = settings.useDynamicColors,
        useAmoledMode = settings.useAmoledMode
    ).background
}

/**
 * Applies window, decor, status bar, and navigation bar colors in one call.
 * Safe to invoke during composition so colors are set before the next draw.
 */
fun Activity.applyThemeWindow(background: Color) {
    ThemeWindowApplier.apply(this, background)
}

fun Activity.installThemeWindowPreDrawSync() {
    ThemeWindowApplier.installPreDrawListener(this)
}

private object ThemeWindowApplier {
    private val backgroundDrawable = ColorDrawable()
    private var lastArgb: Int = 0
    private val preDrawInstalled = mutableSetOf<Activity>()

    fun apply(activity: Activity, background: Color) {
        val argb = background.toArgb()
        lastArgb = argb

        val window = activity.window
        val decorView = window.decorView

        backgroundDrawable.color = argb
        window.setBackgroundDrawable(backgroundDrawable)
        decorView.setBackgroundColor(argb)

        @Suppress("DEPRECATION")
        window.statusBarColor = argb
        @Suppress("DEPRECATION")
        window.navigationBarColor = argb

        val insetsController: WindowInsetsControllerCompat =
            WindowCompat.getInsetsController(window, decorView)
        val useLightIcons = background.luminance() > 0.5f
        insetsController.isAppearanceLightStatusBars = useLightIcons
        insetsController.isAppearanceLightNavigationBars = useLightIcons
    }

    fun installPreDrawListener(activity: Activity) {
        if (!preDrawInstalled.add(activity)) return
        val window = activity.window
        val decorView = window.decorView
        decorView.viewTreeObserver.addOnPreDrawListener {
            if (lastArgb != 0) {
                backgroundDrawable.color = lastArgb
                window.setBackgroundDrawable(backgroundDrawable)
                decorView.setBackgroundColor(lastArgb)
                @Suppress("DEPRECATION")
                window.statusBarColor = lastArgb
                @Suppress("DEPRECATION")
                window.navigationBarColor = lastArgb
            }
            true
        }
    }
}
