package com.cleanshot.app.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.cleanshot.app.models.Screen

@Composable
fun BottomNavigationBar(selectedScreen: Screen, onScreenSelected: (Screen) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        NavigationBarItem(
            selected = selectedScreen == Screen.Home,
            onClick = { onScreenSelected(Screen.Home) },
            icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = selectedScreen == Screen.Library,
            onClick = { onScreenSelected(Screen.Library) },
            icon = { Icon(Icons.Default.Folder, contentDescription = null) },
            label = { Text("Search") }
        )
        NavigationBarItem(
            selected = selectedScreen == Screen.Settings,
            onClick = { onScreenSelected(Screen.Settings) },
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text("Settings") }
        )
    }
}
