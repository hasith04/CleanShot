package com.cleanshot.app.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cleanshot.app.ui.theme.AppTheme
import com.cleanshot.app.ui.theme.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(themeViewModel: ThemeViewModel = viewModel()) {

    val themeSettings by themeViewModel.themeSettings.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),

            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = 32.dp
            ),

            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // APPEARANCE
            item {
                SettingSection(title = "Appearance") {

                    ThemeSelectorCard(
                        selectedThemeMode = themeSettings.theme,
                        onThemeSelected = {
                            themeViewModel.setTheme(it)
                        }
                    )

                    SettingDivider()

                    SettingToggleItem(
                        title = "AMOLED Mode",
                        description = "Pure black background in dark mode",
                        icon = Icons.Outlined.DarkMode,
                        checked = themeSettings.useAmoledMode,
                        onCheckedChange = {
                            themeViewModel.setAmoledMode(it)
                        }
                    )

                    SettingDivider()

                    SettingToggleItem(
                        title = "Dynamic Colors",
                        description = "Sync with system wallpaper colors",
                        icon = Icons.Outlined.Palette,
                        checked = themeSettings.useDynamicColors,
                        onCheckedChange = {
                            themeViewModel.setDynamicColors(it)
                        }
                    )
                }
            }

            // VIEWER
            item {

                var doubleTapZoom by remember { mutableStateOf(true) }
                var swipeNav by remember { mutableStateOf(true) }
                var keepAwake by remember { mutableStateOf(false) }

                SettingSection(title = "Viewer") {

                    SettingToggleItem(
                        title = "Double Tap Zoom",
                        description = "Quick zoom in viewing mode",
                        icon = Icons.Outlined.ZoomIn,
                        checked = doubleTapZoom,
                        onCheckedChange = {
                            doubleTapZoom = it
                        }
                    )

                    SettingDivider()

                    SettingToggleItem(
                        title = "Swipe Navigation",
                        description = "Navigate between screenshots",
                        icon = Icons.Outlined.Gesture,
                        checked = swipeNav,
                        onCheckedChange = {
                            swipeNav = it
                        }
                    )

                    SettingDivider()

                    SettingToggleItem(
                        title = "Keep Screen Awake",
                        description = "Prevent timeout while viewing",
                        icon = Icons.Outlined.Visibility,
                        checked = keepAwake,
                        onCheckedChange = {
                            keepAwake = it
                        }
                    )
                }
            }

            // STORAGE
            item {

                SettingSection(title = "Storage") {

                    SettingActionItem(
                        title = "Screenshot Folder",
                        description = "/Pictures/Screenshots",
                        icon = Icons.Outlined.Folder,
                        onClick = {}
                    )

                    SettingDivider()

                    SettingActionItem(
                        title = "Clear Cache",
                        description = "Free up temporary space",
                        icon = Icons.Outlined.DeleteSweep,
                        onClick = {}
                    )

                    SettingDivider()

                    SettingActionItem(
                        title = "Refresh Media",
                        description = "Manual gallery scan",
                        icon = Icons.Outlined.Refresh,
                        onClick = {}
                    )
                }
            }

            // ABOUT
            item {

                SettingSection(title = "About") {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),

                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Surface(
                            modifier = Modifier.size(90.dp),
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {

                            Icon(
                                imageVector = Icons.Default.Camera,
                                contentDescription = null,
                                modifier = Modifier.padding(22.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "CleanShot",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.headlineSmall
                        )

                        Text(
                            text = "v0.4 Alpha",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Modern screenshot organizer built with Kotlin and Jetpack Compose.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Developed by Jai Hasith",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {

                            SocialIconButton(
                                icon = Icons.Default.Code,
                                contentDescription = "GitHub"
                            )

                            SocialIconButton(
                                icon = Icons.Default.Work,
                                contentDescription = "LinkedIn"
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Made with ❤️ by Jai",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // ADVANCED
            item {

                var experimentalFeatures by remember { mutableStateOf(false) }

                SettingSection(title = "Advanced") {

                    SettingToggleItem(
                        title = "Experimental Features",
                        description = "Try unreleased features",
                        icon = Icons.Outlined.Science,
                        checked = experimentalFeatures,
                        onCheckedChange = {
                            experimentalFeatures = it
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(
                start = 8.dp,
                bottom = 12.dp
            )
        )

        Card(
            modifier = Modifier.fillMaxWidth(),

            shape = RoundedCornerShape(28.dp),

            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            ),

            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {

            Column(
                modifier = Modifier.padding(4.dp),
                content = content
            )
        }
    }
}

@Composable
fun SettingDivider() {

    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 56.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@Composable
fun SettingToggleItem(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {

    ListItem(

        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable {
                onCheckedChange(!checked)
            }
            .animateContentSize(),

        headlineContent = {

            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },

        supportingContent = {

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },

        leadingContent = {

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        },

        trailingContent = {

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        },

        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        )
    )
}

@Composable
fun SettingActionItem(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {

    ListItem(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),

        headlineContent = {
            Text(
                title,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },

        supportingContent = {
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },

        leadingContent = {

            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        },

        trailingContent = {

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        },

        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        )
    )
}

@Composable
fun ThemeSelectorCard(
    selectedThemeMode: AppTheme,
    onThemeSelected: (AppTheme) -> Unit
) {

    val themeOptions = listOf(
        Triple("Light", AppTheme.LIGHT, Icons.Outlined.LightMode),
        Triple("Dark", AppTheme.DARK, Icons.Outlined.DarkMode),
        Triple("Device", AppTheme.SYSTEM, Icons.Outlined.SettingsSuggest)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(24.dp),

        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),

        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            Text(
                text = "Theme",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                themeOptions.forEach { (label, mode, icon) ->

                    val selected = selectedThemeMode == mode

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(80.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onThemeSelected(mode) },

                        shape = RoundedCornerShape(18.dp),

                        colors = CardDefaults.cardColors(
                            containerColor =
                                if (selected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                        ),

                        border = BorderStroke(
                            1.dp,
                            if (selected)
                                MaterialTheme.colorScheme.primary
                            else
                                Color.Transparent
                        )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = label,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                }
            }
        }
    }
}

@Composable
fun SocialIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit = {}
) {

    FilledIconButton(
        onClick = onClick,

        modifier = Modifier.size(48.dp),

        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {

        Icon(
            icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp)
        )
    }
}
