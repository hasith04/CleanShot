package com.cleanshot.app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cleanshot.app.ui.theme.ThemePreferences
import com.cleanshot.app.ui.theme.applyLaunchTheme
import com.cleanshot.app.components.BottomNavigationBar
import com.cleanshot.app.models.Screen
import com.cleanshot.app.models.ScreenshotResults
import com.cleanshot.app.screens.FullscreenViewer
import com.cleanshot.app.screens.HomeScreen
import com.cleanshot.app.screens.LibraryScreen
import com.cleanshot.app.screens.SettingsScreen
import com.cleanshot.app.screens.CleanupScreen
import com.cleanshot.app.ui.theme.CleanShotTheme
import com.cleanshot.app.ui.theme.ThemeRuntime
import com.cleanshot.app.ui.theme.ThemeViewModel
import com.cleanshot.app.ui.theme.applyThemeWindow
import com.cleanshot.app.ui.theme.installThemeWindowPreDrawSync
import com.cleanshot.app.ui.theme.themeBackgroundColor
import com.cleanshot.app.utils.StorageViewModel
import com.cleanshot.app.home.ScanButtonUiState
import com.cleanshot.app.home.formatLastScannedLabel
import com.cleanshot.app.home.resultMessageForScan
import com.cleanshot.app.utils.fetchScreenshotsData
import com.cleanshot.app.utils.getScreenshotStorageInfo
import com.cleanshot.app.utils.refreshScreenshotLibrary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.cleanshot.app.utils.initiateDeletion
import com.cleanshot.app.utils.shareScreenshots

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val launchThemeSettings = ThemePreferences.readBlocking(applicationContext)
        applyLaunchTheme(launchThemeSettings)

        installSplashScreen()

        super.onCreate(savedInstanceState)

        ThemeRuntime.settings = launchThemeSettings
        applyThemeWindow(themeBackgroundColor(launchThemeSettings))
        installThemeWindowPreDrawSync()

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT
            ) { resources ->
                ThemePreferences.resolveDarkTheme(
                    ThemeRuntime.settings,
                    resources.configuration
                )
            },
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT
            ) { resources ->
                ThemePreferences.resolveDarkTheme(
                    ThemeRuntime.settings,
                    resources.configuration
                )
            }
        )

        setContent {
            val themeViewModel: ThemeViewModel = viewModel(
                factory = ThemeViewModel.factory(
                    application = application,
                    initialSettings = launchThemeSettings
                )
            )
            val storageViewModel: StorageViewModel = viewModel()
            val themeSettings by themeViewModel.themeSettings.collectAsState()
            ThemeRuntime.settings = themeSettings

            if (!LocalInspectionMode.current) {
                val targetBackground = themeBackgroundColor(themeSettings)
                applyThemeWindow(targetBackground)
            }

            CleanShotTheme(
                theme = themeSettings.theme,
                useDynamicColors = themeSettings.useDynamicColors,
                useAmoledMode = themeSettings.useAmoledMode,
                colorPreset = themeSettings.colorPreset
            ) {
                MainContainer(themeViewModel, storageViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContainer(themeViewModel: ThemeViewModel, storageViewModel: StorageViewModel) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val storageSettings by storageViewModel.storageSettings.collectAsState()

    var currentScreen by remember {
        mutableStateOf(Screen.Home)
    }

    var previousScreen by remember {
        mutableStateOf(Screen.Home)
    }

    var screenshotData by remember {
        mutableStateOf(
            ScreenshotResults(
                recent = emptyList(),
                all = emptyList(),
                items = emptyList(),
                totalCount = 0
            )
        )
    }

    var isLibraryLoading by remember { mutableStateOf(true) }
    var libraryOptionsOpen by remember { mutableStateOf(false) }
    var scanButtonState by remember { mutableStateOf<ScanButtonUiState>(ScanButtonUiState.Idle) }
    var lastScanEpochMs by remember { mutableLongStateOf(0L) }
    var showOrganizeComingSoon by remember { mutableStateOf(false) }

    var selectedUris by remember {
        mutableStateOf(setOf<Uri>())
    }

    var showDeleteDialog by remember {
        mutableStateOf(false)
    }

    var initialViewerIndex by remember {
        mutableIntStateOf(0)
    }

    val storageInfo = remember(screenshotData.items, screenshotData.totalCount) {
        getScreenshotStorageInfo(context, screenshotData.items)
    }

    val lastScannedLabel = remember(lastScanEpochMs) {
        formatLastScannedLabel(lastScanEpochMs)
    }

    val permissionNeeded =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    fun hasMediaPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, permissionNeeded) ==
            PackageManager.PERMISSION_GRANTED

    fun applyLibraryRefresh(
        data: ScreenshotResults,
        updateLastScan: Boolean = true
    ) {
        screenshotData = data
        if (updateLastScan) {
            lastScanEpochMs = System.currentTimeMillis()
        }
    }

    /*
     * Back handling
     */
    BackHandler(enabled = currentScreen == Screen.Viewer || currentScreen == Screen.Cleanup) {
        currentScreen = previousScreen
    }

    BackHandler(
        enabled = currentScreen == Screen.Library ||
                currentScreen == Screen.Settings
    ) {
        currentScreen = Screen.Home
    }

    /*
     * Delete launcher
     */
    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->

        if (result.resultCode == Activity.RESULT_OK) {
            scope.launch {
                isLibraryLoading = true
                val (data, _) = refreshScreenshotLibrary(
                    context = context,
                    monitoredFolders = storageSettings.monitoredFolders
                )
                applyLibraryRefresh(data)
                isLibraryLoading = false
            }

            selectedUris = emptySet()

            if (currentScreen == Screen.Viewer) {
                currentScreen = Screen.Library
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->

        if (granted) {
            scope.launch {
                isLibraryLoading = true
                val (data, _) = refreshScreenshotLibrary(
                    context = context,
                    monitoredFolders = storageSettings.monitoredFolders
                )
                applyLibraryRefresh(data)
                isLibraryLoading = false
            }
        }
    }

    LaunchedEffect(storageSettings.monitoredFolders) {
        isLibraryLoading = true
        if (hasMediaPermission()) {
            val (data, _) = refreshScreenshotLibrary(
                context = context,
                monitoredFolders = storageSettings.monitoredFolders
            )
            applyLibraryRefresh(data)
        } else {
            permissionLauncher.launch(permissionNeeded)
        }
        isLibraryLoading = false
    }

    fun runLibraryScan() {
        if (scanButtonState is ScanButtonUiState.Scanning) return
        if (!hasMediaPermission()) {
            permissionLauncher.launch(permissionNeeded)
            return
        }
        scope.launch {
            scanButtonState = ScanButtonUiState.Scanning
            val previousUris = screenshotData.all.toSet()
            val (data, outcome) = refreshScreenshotLibrary(
                context = context,
                monitoredFolders = storageSettings.monitoredFolders,
                previousUris = previousUris
            )
            applyLibraryRefresh(data)
            scanButtonState = ScanButtonUiState.Result(resultMessageForScan(outcome))
            delay(2_400)
            if (scanButtonState is ScanButtonUiState.Result) {
                scanButtonState = ScanButtonUiState.Idle
            }
        }
    }

    /*
     * Main UI
     */
    Scaffold(

        topBar = {
            // Hide app bar on full-bleed or self-titled screens
            if (currentScreen != Screen.Viewer &&
                currentScreen != Screen.Cleanup
            ) {

                if (selectedUris.isEmpty()) {

                    CenterAlignedTopAppBar(

                        title = {

                            Text(
                                text = when (currentScreen) {
                                    Screen.Home -> "CleanShot"
                                    Screen.Library -> "Search"
                                    Screen.Settings -> "Settings"
                                    else -> currentScreen.name
                                }
                            )
                        },

                        actions = {
                            if (currentScreen == Screen.Library) {
                                IconButton(onClick = { libraryOptionsOpen = true }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Tune,
                                        contentDescription = "Display options"
                                    )
                                }
                            }
                        }
                    )

                } else {

                    TopAppBar(

                        title = {
                            Text("${selectedUris.size} selected")
                        },

                        navigationIcon = {

                            IconButton(
                                onClick = {
                                    selectedUris = emptySet()
                                }
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = null
                                )
                            }
                        },

                        actions = {

                            IconButton(
                                onClick = {
                                    shareScreenshots(
                                        context,
                                        selectedUris.toList()
                                    )
                                }
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = null
                                )
                            }

                            IconButton(
                                onClick = {
                                    showDeleteDialog = true
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null
                                )
                            }
                        }
                    )
                }
            }
        },

        bottomBar = {
            // 9. Hide bottom navigation while on Cleanup screen
            if (currentScreen != Screen.Viewer && currentScreen != Screen.Cleanup) {

                BottomNavigationBar(
                    selectedScreen = currentScreen
                ) {

                    currentScreen = it

                    selectedUris = emptySet()
                    libraryOptionsOpen = false
                }
            }
        },

        // 2. FIX LIGHT/DARK THEME: Use background color scheme
        containerColor = MaterialTheme.colorScheme.background

    ) { innerPadding ->

        Box(
            modifier = Modifier.padding(
                if (currentScreen == Screen.Viewer || currentScreen == Screen.Cleanup)
                    PaddingValues()
                else
                    innerPadding
            )
        ) {

            when (currentScreen) {

                Screen.Home -> {

                    HomeScreen(
                        storageInfo = storageInfo,
                        recentScreenshots = screenshotData.recent,
                        scanButtonState = scanButtonState,
                        lastScannedLabel = lastScannedLabel,
                        isScanEnabled = scanButtonState is ScanButtonUiState.Idle,
                        onScanClick = { runLibraryScan() },
                        onOrganizeClick = { showOrganizeComingSoon = true },
                        showOrganizeComingSoon = showOrganizeComingSoon,
                        onDismissOrganizeComingSoon = { showOrganizeComingSoon = false },
                        onViewAllClick = {
                            previousScreen = currentScreen
                            currentScreen = Screen.Library
                        },
                        onScreenshotClick = { uri ->
                            initialViewerIndex = screenshotData.all.indexOf(uri).coerceAtLeast(0)
                            previousScreen = currentScreen
                            currentScreen = Screen.Viewer
                        },
                        onCleanupClick = {
                            previousScreen = currentScreen
                            currentScreen = Screen.Cleanup
                        }
                    )
                }

                Screen.Cleanup -> {
                    CleanupScreen(
                        screenshots = screenshotData.all,
                        onBack = {
                            currentScreen = previousScreen
                        },
                        onBackToLibrary = {
                            currentScreen = Screen.Library
                        },
                        onKeep = { },
                        onDelete = { _ ->
                            scope.launch {
                                isLibraryLoading = true
                                val (data, _) = refreshScreenshotLibrary(
                                    context = context,
                                    monitoredFolders = storageSettings.monitoredFolders
                                )
                                applyLibraryRefresh(data)
                                isLibraryLoading = false
                            }
                        }
                    )
                }

                Screen.Library -> {

                    LibraryScreen(
                        items = screenshotData.items,
                        isLoading = isLibraryLoading,
                        optionsSheetOpen = libraryOptionsOpen,
                        onOptionsSheetDismiss = { libraryOptionsOpen = false },
                        selectedUris = selectedUris,
                        onToggleSelection = { uri ->

                            selectedUris =
                                if (selectedUris.contains(uri)) {
                                    selectedUris - uri
                                } else {
                                    selectedUris + uri
                                }
                        },

                        onScreenshotClick = { uri ->

                            initialViewerIndex =
                                screenshotData.all.indexOf(uri)
                                    .coerceAtLeast(0)

                            previousScreen = currentScreen
                            currentScreen = Screen.Viewer
                        }
                    )
                }

                Screen.Viewer -> {

                    FullscreenViewer(
                        screenshots = screenshotData.all,
                        initialIndex = initialViewerIndex,

                        onBack = {
                            currentScreen = previousScreen
                        },

                        onDelete = { uri ->

                            selectedUris = setOf(uri)

                            showDeleteDialog = true
                        }
                    )
                }

                Screen.Settings -> {

                    SettingsScreen(themeViewModel, storageViewModel)
                }
            }
        }

        /*
         * Delete dialog
         */
        if (showDeleteDialog) {

            AlertDialog(

                onDismissRequest = {
                    showDeleteDialog = false
                },

                title = {
                    Text("Delete Screenshots")
                },

                text = {

                    Text(
                        if (currentScreen == Screen.Viewer)
                            "Delete this screenshot?"
                        else
                            "Delete ${selectedUris.size} screenshots?"
                    )
                },

                confirmButton = {

                    Button(
                        onClick = {

                            showDeleteDialog = false

                            initiateDeletion(
                                context,
                                selectedUris.toList()
                            ) { request ->

                                deleteLauncher.launch(request)
                            }
                        }
                    ) {
                        Text("Delete")
                    }
                },

                dismissButton = {

                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
