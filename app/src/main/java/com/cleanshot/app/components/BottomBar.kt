package com.cleanshot.app.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.cleanshot.app.models.Screen

private val NavColorAnimation = tween<Color>(durationMillis = 350)

@Composable
fun BottomNavigationBar(selectedScreen: Screen, onScreenSelected: (Screen) -> Unit) {
    val containerColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = NavColorAnimation,
        label = "nav_container"
    )
    val indicatorColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.secondaryContainer,
        animationSpec = NavColorAnimation,
        label = "nav_indicator"
    )
    val selectedColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.primary,
        animationSpec = NavColorAnimation,
        label = "nav_selected"
    )
    val unselectedColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = NavColorAnimation,
        label = "nav_unselected"
    )

    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = selectedColor,
        selectedTextColor = selectedColor,
        unselectedIconColor = unselectedColor,
        unselectedTextColor = unselectedColor,
        indicatorColor = indicatorColor
    )

    NavigationBar(containerColor = containerColor) {
        NavigationBarItem(
            selected = selectedScreen == Screen.Home,
            onClick = { onScreenSelected(Screen.Home) },
            icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
            label = { Text("Home") },
            colors = itemColors
        )
        NavigationBarItem(
            selected = selectedScreen == Screen.Library,
            onClick = { onScreenSelected(Screen.Library) },
            icon = { Icon(Icons.Default.Folder, contentDescription = null) },
            label = { Text("Search") },
            colors = itemColors
        )
        NavigationBarItem(
            selected = selectedScreen == Screen.Settings,
            onClick = { onScreenSelected(Screen.Settings) },
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text("Settings") },
            colors = itemColors
        )
    }
}
