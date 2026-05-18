package com.cleanshot.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    secondary = Color(0xFFCCC2DC),
    tertiary = Color(0xFFEFB8C8)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6650a4),
    secondary = Color(0xFF625b71),
    tertiary = Color(0xFF7D5260)
)

private val ThemeColorAnimationSpec = tween<Color>(
    durationMillis = 350,
    easing = FastOutSlowInEasing
)

internal fun resolveColorScheme(
    context: android.content.Context,
    darkTheme: Boolean,
    useDynamicColors: Boolean,
    useAmoledMode: Boolean
): ColorScheme {
    val base = when {
        useDynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    return if (darkTheme && useAmoledMode) {
        base.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = Color(0xFF111111),
            onBackground = Color.White,
            onSurface = Color.White
        )
    } else {
        base
    }
}

@Composable
private fun animateThemeColor(target: Color, label: String): Color =
    animateColorAsState(
        targetValue = target,
        animationSpec = ThemeColorAnimationSpec,
        label = label
    ).value

@Composable
private fun ColorScheme.withAnimatedColors(): ColorScheme = copy(
    primary = animateThemeColor(primary, "primary"),
    onPrimary = animateThemeColor(onPrimary, "onPrimary"),
    primaryContainer = animateThemeColor(primaryContainer, "primaryContainer"),
    onPrimaryContainer = animateThemeColor(onPrimaryContainer, "onPrimaryContainer"),
    secondary = animateThemeColor(secondary, "secondary"),
    onSecondary = animateThemeColor(onSecondary, "onSecondary"),
    secondaryContainer = animateThemeColor(secondaryContainer, "secondaryContainer"),
    onSecondaryContainer = animateThemeColor(onSecondaryContainer, "onSecondaryContainer"),
    tertiary = animateThemeColor(tertiary, "tertiary"),
    onTertiary = animateThemeColor(onTertiary, "onTertiary"),
    tertiaryContainer = animateThemeColor(tertiaryContainer, "tertiaryContainer"),
    onTertiaryContainer = animateThemeColor(onTertiaryContainer, "onTertiaryContainer"),
    background = animateThemeColor(background, "background"),
    onBackground = animateThemeColor(onBackground, "onBackground"),
    surface = animateThemeColor(surface, "surface"),
    onSurface = animateThemeColor(onSurface, "onSurface"),
    surfaceVariant = animateThemeColor(surfaceVariant, "surfaceVariant"),
    onSurfaceVariant = animateThemeColor(onSurfaceVariant, "onSurfaceVariant"),
    surfaceBright = animateThemeColor(surfaceBright, "surfaceBright"),
    surfaceDim = animateThemeColor(surfaceDim, "surfaceDim"),
    surfaceContainer = animateThemeColor(surfaceContainer, "surfaceContainer"),
    surfaceContainerHigh = animateThemeColor(surfaceContainerHigh, "surfaceContainerHigh"),
    surfaceContainerHighest = animateThemeColor(surfaceContainerHighest, "surfaceContainerHighest"),
    surfaceContainerLow = animateThemeColor(surfaceContainerLow, "surfaceContainerLow"),
    surfaceContainerLowest = animateThemeColor(surfaceContainerLowest, "surfaceContainerLowest"),
    outline = animateThemeColor(outline, "outline"),
    outlineVariant = animateThemeColor(outlineVariant, "outlineVariant"),
    error = animateThemeColor(error, "error"),
    onError = animateThemeColor(onError, "onError"),
    errorContainer = animateThemeColor(errorContainer, "errorContainer"),
    onErrorContainer = animateThemeColor(onErrorContainer, "onErrorContainer"),
    inverseSurface = animateThemeColor(inverseSurface, "inverseSurface"),
    inverseOnSurface = animateThemeColor(inverseOnSurface, "inverseOnSurface"),
    inversePrimary = animateThemeColor(inversePrimary, "inversePrimary"),
    surfaceTint = animateThemeColor(surfaceTint, "surfaceTint"),
    scrim = animateThemeColor(scrim, "scrim")
)

@Composable
fun CleanShotTheme(
    theme: AppTheme = AppTheme.SYSTEM,
    useDynamicColors: Boolean = true,
    useAmoledMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (theme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    val context = LocalContext.current
    val targetScheme = remember(theme, useDynamicColors, useAmoledMode, darkTheme, context) {
        resolveColorScheme(context, darkTheme, useDynamicColors, useAmoledMode)
    }
    val colorScheme = targetScheme.withAnimatedColors()

    if (!LocalInspectionMode.current) {
        val activity = LocalContext.current as Activity
        activity.applyThemeWindow(targetScheme.background)
        activity.applyThemeWindow(colorScheme.background)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colorScheme.background
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
