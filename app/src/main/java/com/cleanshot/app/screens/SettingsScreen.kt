package com.cleanshot.app.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cleanshot.app.ui.theme.AppTheme
import com.cleanshot.app.ui.theme.ThemeViewModel
import com.cleanshot.app.utils.StorageViewModel

private enum class SettingsView {
    Main, Version, Credits, ManageFolders
}

/**
 * Reusable helper to open external links
 */
private fun openLink(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        // Handle case where no browser is installed
    }
}

/**
 * Helper to clear app cache safely
 */
private fun clearAppCache(context: Context) {
    try {
        context.cacheDir.deleteRecursively()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeViewModel: ThemeViewModel = viewModel(),
    storageViewModel: StorageViewModel = viewModel()
) {
    val themeSettings by themeViewModel.themeSettings.collectAsState()
    val storageSettings by storageViewModel.storageSettings.collectAsState()
    var currentView by remember { mutableStateOf(SettingsView.Main) }

    // Handle back press to return to main settings list
    BackHandler(enabled = currentView != SettingsView.Main) {
        currentView = SettingsView.Main
    }

    when (currentView) {
        SettingsView.Main -> {
            MainSettingsList(
                themeSettings = themeSettings,
                themeViewModel = themeViewModel,
                storageSettings = storageSettings,
                storageViewModel = storageViewModel,
                onNavigateToVersion = { currentView = SettingsView.Version },
                onNavigateToCredits = { currentView = SettingsView.Credits },
                onNavigateToManageFolders = { currentView = SettingsView.ManageFolders }
            )
        }
        SettingsView.Version -> {
            VersionScreen(onBack = { currentView = SettingsView.Main })
        }
        SettingsView.Credits -> {
            CreditsScreen(onBack = { currentView = SettingsView.Main })
        }
        SettingsView.ManageFolders -> {
            ManageFoldersScreen(
                storageViewModel = storageViewModel,
                onBack = { currentView = SettingsView.Main }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainSettingsList(
    themeSettings: com.cleanshot.app.ui.theme.ThemeSettings,
    themeViewModel: ThemeViewModel,
    storageSettings: com.cleanshot.app.utils.StorageSettings,
    storageViewModel: StorageViewModel,
    onNavigateToVersion: () -> Unit,
    onNavigateToCredits: () -> Unit,
    onNavigateToManageFolders: () -> Unit
) {
    val context = LocalContext.current
    var showClearCacheDialog by remember { mutableStateOf(false) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { storageViewModel.setMainFolder(it) }
    }

    val mainFolderDescription = remember(storageSettings.mainScreenshotFolder) {
        storageSettings.mainScreenshotFolder?.let { uriString ->
            try {
                val decodedUri = Uri.decode(uriString)
                val treeId = decodedUri.substringAfter("/tree/", "")
                val path = if (treeId.startsWith("primary:")) {
                    treeId.substringAfter("primary:")
                } else {
                    treeId.substringAfter(":", treeId)
                }
                path.ifEmpty { "Selected Folder" }
            } catch (e: Exception) {
                "Selected Folder"
            }
        } ?: "Default directory"
    }

    // Cache Clearing Confirmation Dialog
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("Clear Cache") },
            text = { Text("Clear temporary cache files?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        clearAppCache(context)
                        showClearCacheDialog = false
                        Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
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
                        onThemeSelected = { themeViewModel.setTheme(it) }
                    )

                    SettingDivider()

                    SettingToggleItem(
                        title = "Dynamic Colors",
                        description = "Sync with system wallpaper colors",
                        icon = Icons.Outlined.Palette,
                        checked = themeSettings.useDynamicColors,
                        onCheckedChange = { themeViewModel.setDynamicColors(it) }
                    )

                    SettingDivider()

                    SettingToggleItem(
                        title = "AMOLED Mode",
                        description = "Pure black theme for OLED displays",
                        icon = Icons.Outlined.DarkMode,
                        checked = themeSettings.useAmoledMode,
                        onCheckedChange = { themeViewModel.setAmoledMode(it) }
                    )
                }
            }

            // STORAGE
            item {
                SettingSection(title = "Storage") {
                    SettingActionItem(
                        title = "Screenshot Folder",
                        description = mainFolderDescription,
                        icon = Icons.Outlined.Folder,
                        onClick = { folderPickerLauncher.launch(null) }
                    )

                    SettingDivider()

                    SettingActionItem(
                        title = "Manage Folders",
                        description = "${storageSettings.monitoredFolders.size} folders monitored",
                        icon = Icons.Outlined.FolderCopy,
                        onClick = onNavigateToManageFolders
                    )

                    SettingDivider()

                    SettingActionItem(
                        title = "Clear Cache",
                        description = "Free temporary storage space",
                        icon = Icons.Outlined.DeleteSweep,
                        onClick = { showClearCacheDialog = true }
                    )
                }
            }

            // ABOUT
            item {
                SettingSection(title = "About") {
                    SettingActionItem(
                        title = "Version",
                        description = "App version and updates",
                        icon = Icons.Outlined.Info,
                        onClick = onNavigateToVersion
                    )

                    SettingDivider()

                    SettingActionItem(
                        title = "GitHub",
                        description = "Source code and issues",
                        icon = Icons.Default.Code,
                        onClick = { openLink(context, "https://github.com/hasith04") }
                    )

                    SettingDivider()

                    SettingActionItem(
                        title = "Credits",
                        description = "Developer info and social links",
                        icon = Icons.Outlined.Favorite,
                        onClick = onNavigateToCredits
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManageFoldersScreen(
    storageViewModel: StorageViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val storageSettings by storageViewModel.storageSettings.collectAsState()

    val addFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { storageViewModel.addMonitoredFolder(it) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Manage Folders", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { addFolderLauncher.launch(null) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Folder")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (storageSettings.monitoredFolders.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillParentMaxSize()
                            .padding(bottom = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.FolderOff,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No monitored folders",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Add folders to scan for screenshots",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                items(storageSettings.monitoredFolders.toList()) { uriString ->
                    val uri = Uri.parse(uriString)
                    val folderName = remember(uriString) {
                        try {
                            DocumentFile.fromTreeUri(context, uri)?.name ?: "Unknown Folder"
                        } catch (e: Exception) {
                            "Unknown Folder"
                        }
                    }

                    val folderPath = remember(uriString) {
                        try {
                            val decodedUri = Uri.decode(uriString)
                            val treeId = decodedUri.substringAfter("/tree/", "")
                            if (treeId.startsWith("primary:")) {
                                treeId.substringAfter("primary:")
                            } else {
                                treeId.substringAfter(":", treeId)
                            }
                        } catch (e: Exception) { "" }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = folderPath,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = folderName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                                if (uriString == storageSettings.mainScreenshotFolder) {
                                    Text(
                                        text = "Main screenshot folder",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            IconButton(onClick = { storageViewModel.removeMonitoredFolder(uriString) }) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Remove Folder",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VersionScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Version Info", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp, horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(90.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shadowElevation = 2.dp
                    ) {
                        Icon(
                            imageVector = Icons.Default.Camera,
                            contentDescription = null,
                            modifier = Modifier.padding(22.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "CleanShot",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "v0.4 Alpha",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Modern screenshot organizer built with Kotlin and Material You.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                VersionActionButton(
                    text = "Check for Updates",
                    icon = Icons.Outlined.Update,
                    onClick = {}
                )
                VersionActionButton(
                    text = "View Changelog",
                    icon = Icons.Outlined.History,
                    onClick = {}
                )
                VersionActionButton(
                    text = "Report Bugs",
                    icon = Icons.Outlined.BugReport,
                    onClick = {}
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Made with ❤️ by Jai Hasith",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun VersionActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreditsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Credits", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
        ) {
            item {
                CreditCardSection(title = "Developer", icon = Icons.Outlined.Person) {
                    ListItem(
                        headlineContent = {
                            Text("Jai Hasith", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        },
                        supportingContent = { Text("Lead Developer") },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }

            item {
                CreditCardSection(title = "Social Links", icon = Icons.Outlined.Public) {
                    SocialLinkCard(
                        title = "GitHub",
                        subtitle = "Explore my projects & source code",
                        icon = Icons.Default.Code,
                        onClick = { openLink(context, "https://github.com/hasith04") }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SocialLinkCard(
                        title = "LinkedIn",
                        subtitle = "Let's connect professionally",
                        icon = Icons.Default.Work,
                        onClick = { openLink(context, "https://www.linkedin.com/in/hasith04") }
                    )
                }
            }

            item {
                Text(
                    text = "Made with ❤️ by Jai Hasith",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun CreditCardSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, bottom = 12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SocialLinkCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Bold) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
fun SettingSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 12.dp)
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
            Column(modifier = Modifier.padding(4.dp), content = content)
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
            .clickable { onCheckedChange(!checked) }
            .animateContentSize(),
        headlineContent = {
            Text(text = title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        },
        supportingContent = {
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        leadingContent = {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
        },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
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
            Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        },
        supportingContent = {
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        leadingContent = {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Theme",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                text = "Light, dark, or system default",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
                        ),
                        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
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