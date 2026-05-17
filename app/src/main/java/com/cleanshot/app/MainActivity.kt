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
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cleanshot.app.components.BottomNavigationBar
import com.cleanshot.app.models.Screen
import com.cleanshot.app.models.ScreenshotResults
import com.cleanshot.app.screens.FullscreenViewer
import com.cleanshot.app.screens.HomeScreen
import com.cleanshot.app.screens.LibraryScreen
import com.cleanshot.app.screens.SettingsScreen
import com.cleanshot.app.ui.theme.CleanShotTheme
import com.cleanshot.app.ui.theme.ThemeViewModel
import com.cleanshot.app.utils.fetchScreenshotsData
import com.cleanshot.app.utils.getStorageInfo
import com.cleanshot.app.utils.initiateDeletion
import com.cleanshot.app.utils.shareScreenshots

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            val themeViewModel: ThemeViewModel = viewModel()
            val themeSettings by themeViewModel.themeSettings.collectAsState()

            CleanShotTheme(
                theme = themeSettings.theme,
                useDynamicColors = themeSettings.useDynamicColors,
                useAmoledMode = themeSettings.useAmoledMode
            ) {
                MainContainer(themeViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContainer(themeViewModel: ThemeViewModel) {

    val context = LocalContext.current

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
                totalCount = 0
            )
        )
    }

    var selectedUris by remember {
        mutableStateOf(setOf<Uri>())
    }

    var showDeleteDialog by remember {
        mutableStateOf(false)
    }

    var initialViewerIndex by remember {
        mutableIntStateOf(0)
    }

    val storageInfo = remember(screenshotData) {
        getStorageInfo(context)
    }

    /*
     * Back handling
     */
    BackHandler(enabled = currentScreen == Screen.Viewer) {
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

            screenshotData = fetchScreenshotsData(context)

            selectedUris = emptySet()

            if (currentScreen == Screen.Viewer) {
                currentScreen = Screen.Library
            }
        }
    }

    /*
     * Permission handling
     */
    val permissionNeeded =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->

        if (granted) {
            screenshotData = fetchScreenshotsData(context)
        }
    }

    LaunchedEffect(Unit) {

        if (
            ContextCompat.checkSelfPermission(
                context,
                permissionNeeded
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            screenshotData = fetchScreenshotsData(context)

        } else {

            permissionLauncher.launch(permissionNeeded)
        }
    }

    /*
     * Main UI
     */
    Scaffold(

        topBar = {

            if (currentScreen != Screen.Viewer) {

                if (selectedUris.isEmpty()) {

                    CenterAlignedTopAppBar(

                        title = {

                            Text(
                                text = if (currentScreen == Screen.Home)
                                    "CleanShot"
                                else
                                    currentScreen.name
                            )
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

            if (currentScreen != Screen.Viewer) {

                BottomNavigationBar(
                    selectedScreen = currentScreen
                ) {

                    currentScreen = it

                    selectedUris = emptySet()
                }
            }
        },

        containerColor =
            if (currentScreen == Screen.Viewer)
                Color.Black
            else
                MaterialTheme.colorScheme.background

    ) { innerPadding ->

        Box(
            modifier = Modifier.padding(
                if (currentScreen == Screen.Viewer)
                    PaddingValues()
                else
                    innerPadding
            )
        ) {

            when (currentScreen) {

                Screen.Home -> {

                    HomeScreen(
                        storageInfo = storageInfo,
                        totalCount = screenshotData.totalCount,
                        recentScreenshots = screenshotData.recent,

                        onViewAllClick = {

                            previousScreen = currentScreen
                            currentScreen = Screen.Library
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

                Screen.Library -> {

                    LibraryScreen(
                        allScreenshots = screenshotData.all,
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

                    SettingsScreen(themeViewModel)
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