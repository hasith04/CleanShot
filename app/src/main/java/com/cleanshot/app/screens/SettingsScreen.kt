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
import androidx.compose.foundation.Image
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
import android.text.format.Formatter
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.painterResource
import com.cleanshot.app.R
import com.cleanshot.app.data.AppInfo
import com.cleanshot.app.data.ChangelogData
import com.cleanshot.app.settings.PresetThemeSelector
import com.cleanshot.app.settings.SettingsDesign
import com.cleanshot.app.settings.ThemeModeSelector
import com.cleanshot.app.ui.theme.ThemeViewModel
import com.cleanshot.app.updates.UpdateCheckResult
import com.cleanshot.app.updates.checkForAppUpdate
import com.cleanshot.app.utils.StorageViewModel
import com.cleanshot.app.utils.calculateCacheBytes
import com.cleanshot.app.utils.clearAppCache
import kotlinx.coroutines.launch

private enum class SettingsView {
    Main, Version, Changelog, Credits, ManageFolders
}

private fun openLink(context: Context, url: String) {
    if (url.isBlank()) return
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        context.startActivity(Intent.createChooser(intent, null))
    } catch (_: Exception) {
        Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
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

    BackHandler(enabled = currentView != SettingsView.Main) {
        currentView = when (currentView) {
            SettingsView.Changelog -> SettingsView.Version
            else -> SettingsView.Main
        }
    }

    when (currentView) {
        SettingsView.Main -> {
            MainSettingsList(
                themeSettings = themeSettings,
                themeViewModel = themeViewModel,
                storageSettings = storageSettings,
                storageViewModel = storageViewModel,
                onNavigateToVersion = { currentView = SettingsView.Version },
                onNavigateToChangelog = { currentView = SettingsView.Changelog },
                onNavigateToCredits = { currentView = SettingsView.Credits },
                onNavigateToManageFolders = { currentView = SettingsView.ManageFolders }
            )
        }
        SettingsView.Version -> {
            VersionScreen(
                onBack = { currentView = SettingsView.Main },
                onOpenChangelog = { currentView = SettingsView.Changelog }
            )
        }
        SettingsView.Changelog -> {
            ChangelogScreen(onBack = { currentView = SettingsView.Version })
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
    onNavigateToChangelog: () -> Unit,
    onNavigateToCredits: () -> Unit,
    onNavigateToManageFolders: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var cacheBytes by remember { mutableStateOf(0L) }
    var isCacheLoading by remember { mutableStateOf(true) }
    var isClearingCache by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isCacheLoading = true
        cacheBytes = calculateCacheBytes(context)
        isCacheLoading = false
    }

    val cacheDescription = when {
        isCacheLoading -> "Calculating cache size…"
        isClearingCache -> "Clearing cache…"
        cacheBytes <= 0L -> "Nothing to clear"
        else -> "${Formatter.formatShortFileSize(context, cacheBytes)} available to clear"
    }

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
                        showClearCacheDialog = false
                        scope.launch {
                            isClearingCache = true
                            clearAppCache(context)
                            cacheBytes = calculateCacheBytes(context)
                            isClearingCache = false
                            Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
                        }
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = SettingsDesign.HorizontalPadding),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(SettingsDesign.SectionSpacing)
    ) {
            item {
                SettingSection(title = "Appearance") {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                    ThemeModeSelector(
                        selectedTheme = themeSettings.theme,
                        onThemeSelected = { themeViewModel.setTheme(it) }
                    )

                    PresetThemeSelector(
                        selectedPreset = themeSettings.colorPreset,
                        dynamicColorsEnabled = themeSettings.useDynamicColors,
                        onPresetSelected = { themeViewModel.setColorPreset(it) }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

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
                        description = cacheDescription,
                        icon = Icons.Outlined.DeleteSweep,
                        onClick = { if (!isClearingCache) showClearCacheDialog = true }
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
                        description = "CleanShot source code and issues",
                        icon = Icons.Outlined.Code,
                        onClick = { openLink(context, AppInfo.GITHUB_REPO) }
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
private fun VersionScreen(
    onBack: () -> Unit,
    onOpenChangelog: () -> Unit
) {
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
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        var isCheckingUpdate by remember { mutableStateOf(false) }
        var updateDialogMessage by remember { mutableStateOf<String?>(null) }
        var updateReleaseUrl by remember { mutableStateOf<String?>(null) }

        updateDialogMessage?.let { message ->
            val releaseUrl = updateReleaseUrl
            AlertDialog(
                onDismissRequest = {
                    updateDialogMessage = null
                    updateReleaseUrl = null
                },
                title = { Text("Updates") },
                text = { Text(message) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            releaseUrl?.let { openLink(context, it) }
                            updateDialogMessage = null
                            updateReleaseUrl = null
                        }
                    ) {
                        Text(if (releaseUrl != null) "View release" else "OK")
                    }
                },
                dismissButton = if (releaseUrl != null) {
                    {
                        TextButton(onClick = {
                            updateDialogMessage = null
                            updateReleaseUrl = null
                        }) {
                            Text("Close")
                        }
                    }
                } else {
                    null
                }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = SettingsDesign.HorizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(
                            0.5.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)
                        )
                    ) {
                        Image(
                            painter = painterResource(R.mipmap.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = AppInfo.NAME,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Text(
                        text = "v${AppInfo.VERSION_NAME}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = AppInfo.TAGLINE,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }

            item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VersionActionButton(
                    text = if (isCheckingUpdate) "Checking…" else "Check for Updates",
                    icon = Icons.Outlined.Update,
                    onClick = {
                        if (isCheckingUpdate) return@VersionActionButton
                        scope.launch {
                            isCheckingUpdate = true
                            when (val result = checkForAppUpdate(AppInfo.VERSION_NAME)) {
                                is UpdateCheckResult.UpToDate ->
                                    updateDialogMessage = "You're up to date."
                                is UpdateCheckResult.UpdateAvailable -> {
                                    updateDialogMessage =
                                        "Update available: v${result.latestVersion}\n\n${result.releaseNotes}"
                                    updateReleaseUrl = result.releaseUrl
                                }
                                is UpdateCheckResult.Error ->
                                    updateDialogMessage = result.message
                            }
                            isCheckingUpdate = false
                        }
                    }
                )
                VersionActionButton(
                    text = "View Changelog",
                    icon = Icons.Outlined.History,
                    onClick = onOpenChangelog
                )
                VersionActionButton(
                    text = "Report Bugs",
                    icon = Icons.Outlined.BugReport,
                    onClick = { openLink(context, AppInfo.GITHUB_ISSUES) }
                )
            }
            }

            item {
                Text(
                    text = "Made with ❤️ by Jai Hasith",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VersionActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsDesign.CardShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(SettingsDesign.ItemIconSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
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
                .padding(horizontal = SettingsDesign.HorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
        ) {
            item {
                CreditCardSection(title = "Developer", icon = Icons.Outlined.Person) {
                    ListItem(
                        modifier = Modifier.clickable {
                            openLink(context, AppInfo.DEVELOPER_WEBSITE)
                        },
                        headlineContent = {
                            Text("Jai Hasith", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        },
                        supportingContent = { Text("Lead Developer · Tap to visit website") },
                        leadingContent = {
                            Icon(
                                Icons.Outlined.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(SettingsDesign.ItemIconSize)
                            )
                        },
                        trailingContent = {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }

            item {
                CreditCardSection(title = "Social Links", icon = Icons.Outlined.Public) {
                    SocialLinkCard(
                        title = "GitHub",
                        subtitle = "CleanShot source code and issues",
                        icon = Icons.Outlined.Code,
                        onClick = { openLink(context, AppInfo.GITHUB_REPO) }
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
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = SettingsDesign.CardShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(
                0.5.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
            ),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
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
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        },
        supportingContent = {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
            )
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(SettingsDesign.ItemIconSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = SettingsDesign.CardShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(
                0.5.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
            ),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                content = content
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangelogScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Changelog", fontWeight = FontWeight.SemiBold) },
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
                .padding(horizontal = SettingsDesign.HorizontalPadding),
            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(ChangelogData.entries) { entry ->
                var expanded by remember(entry.version) { mutableStateOf(true) }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded },
                    shape = SettingsDesign.InnerCardShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(
                        0.5.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
                    ),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = entry.version,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = if (expanded) {
                                    Icons.Default.ExpandLess
                                } else {
                                    Icons.Default.ExpandMore
                                },
                                contentDescription = null
                            )
                        }
                        if (expanded) {
                            Spacer(modifier = Modifier.height(10.dp))
                            entry.notes.forEach { note ->
                                Row(
                                    modifier = Modifier.padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        "• ",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = note,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 52.dp, end = 12.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
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
            .clip(RoundedCornerShape(SettingsDesign.InnerCardRadius))
            .clickable { onCheckedChange(!checked) }
            .animateContentSize(),
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
            )
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(SettingsDesign.ItemIconSize)
            )
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
            .clip(RoundedCornerShape(SettingsDesign.InnerCardRadius))
            .clickable(onClick = onClick),
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
            )
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(SettingsDesign.ItemIconSize)
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.size(18.dp)
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
