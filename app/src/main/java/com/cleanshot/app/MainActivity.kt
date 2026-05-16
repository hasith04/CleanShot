package com.cleanshot.app

import android.Manifest
import android.app.Activity
import android.app.usage.StorageStatsManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import android.provider.MediaStore
import android.text.format.Formatter
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import com.cleanshot.app.ui.theme.CleanShotTheme
import java.util.ArrayList
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CleanShotTheme {
                MainContainer()
            }
        }
    }
}

/**
 * 1. Navigation Setup
 */
enum class Screen { Home, Library, Settings, Viewer }

/**
 * Data class to hold screenshot results
 */
data class ScreenshotResults(
    val recent: List<Uri>,
    val all: List<Uri>,
    val totalCount: Int
)

/**
 * Data class to hold our real storage info
 */
data class StorageInfo(
    val totalSpace: String,
    val usedSpace: String,
    val freeSpace: String,
    val progress: Float
)

/**
 * Helper function to calculate REAL device storage.
 */
fun getStorageInfo(context: Context): StorageInfo {
    val stat = StatFs(Environment.getDataDirectory().path)
    var totalBytes: Long = 0
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        try {
            val statsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
            totalBytes = statsManager.getTotalBytes(StorageManager.UUID_DEFAULT)
        } catch (e: Exception) {
            totalBytes = stat.totalBytes
        }
    } else {
        totalBytes = stat.totalBytes
    }
    val freeBytes = stat.availableBytes
    val usedBytes = totalBytes - freeBytes
    val totalSpaceStr = Formatter.formatShortFileSize(context, totalBytes)
    val usedSpaceStr = Formatter.formatShortFileSize(context, usedBytes)
    val freeSpaceStr = Formatter.formatShortFileSize(context, freeBytes)
    val progress = if (totalBytes > 0) usedBytes.toFloat() / totalBytes.toFloat() else 0f
    return StorageInfo(totalSpaceStr, usedSpaceStr, freeSpaceStr, progress)
}

/**
 * The Root Composable that handles Navigation, Data Loading, and Selection logic
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContainer() {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(Screen.Home) }
    var screenshotData by remember { mutableStateOf(ScreenshotResults(emptyList(), emptyList(), 0)) }
    
    // Selection and Deletion State
    var selectedUris by remember { mutableStateOf(setOf<Uri>()) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Viewer state
    var initialViewerIndex by remember { mutableIntStateOf(0) }

    val storageInfo = remember(screenshotData) { getStorageInfo(context) }

    // Launcher for handling the system delete confirmation (Required for Android 11+)
    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Success! Refresh data and clear selection
            screenshotData = fetchScreenshotsData(context)
            selectedUris = emptySet()
            if (currentScreen == Screen.Viewer) {
                currentScreen = Screen.Library
            }
        }
    }

    val permissionNeeded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            screenshotData = fetchScreenshotsData(context)
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, permissionNeeded) == PackageManager.PERMISSION_GRANTED) {
            screenshotData = fetchScreenshotsData(context)
        } else {
            permissionLauncher.launch(permissionNeeded)
        }
    }

    Scaffold(
        topBar = {
            if (currentScreen != Screen.Viewer) {
                if (selectedUris.isEmpty()) {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = if (currentScreen == Screen.Home) "CleanShot" else currentScreen.name,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                } else {
                    // PROBLEM 2 FIXED: Multi-Select Toolbar with Share
                    TopAppBar(
                        title = { Text("${selectedUris.size} selected") },
                        navigationIcon = {
                            IconButton(onClick = { selectedUris = emptySet() }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear selection")
                            }
                        },
                        actions = {
                            IconButton(onClick = { shareScreenshots(context, selectedUris.toList()) }) {
                                Icon(Icons.Default.Share, contentDescription = "Share selected")
                            }
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete selected")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    )
                }
            }
        },
        bottomBar = {
            if (currentScreen != Screen.Viewer) {
                BottomNavigationBar(currentScreen) { 
                    currentScreen = it 
                    selectedUris = emptySet() // Clear selection when switching screens
                }
            }
        },
        containerColor = if (currentScreen == Screen.Viewer) Color.Black else MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.padding(if (currentScreen == Screen.Viewer) PaddingValues(0.dp) else innerPadding)) {
            when (currentScreen) {
                Screen.Home -> HomeScreen(
                    storageInfo = storageInfo,
                    totalCount = screenshotData.totalCount,
                    recentScreenshots = screenshotData.recent,
                    onViewAllClick = { currentScreen = Screen.Library },
                    onScreenshotClick = { uri ->
                        initialViewerIndex = screenshotData.all.indexOf(uri).coerceAtLeast(0)
                        currentScreen = Screen.Viewer
                    }
                )
                Screen.Library -> LibraryScreen(
                    allScreenshots = screenshotData.all,
                    selectedUris = selectedUris,
                    onToggleSelection = { uri ->
                        selectedUris = if (selectedUris.contains(uri)) {
                            selectedUris - uri
                        } else {
                            selectedUris + uri
                        }
                    },
                    onScreenshotClick = { uri ->
                        initialViewerIndex = screenshotData.all.indexOf(uri).coerceAtLeast(0)
                        currentScreen = Screen.Viewer
                    }
                )
                Screen.Viewer -> {
                    FullscreenViewer(
                        screenshots = screenshotData.all,
                        initialIndex = initialViewerIndex,
                        onBack = { currentScreen = Screen.Library },
                        onDelete = { uri ->
                            selectedUris = setOf(uri)
                            showDeleteDialog = true
                        }
                    )
                }
                Screen.Settings -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Settings screen coming soon!")
                }
            }
        }

        // Delete Confirmation Dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Screenshots") },
                text = { Text("Are you sure you want to delete ${if (currentScreen == Screen.Viewer) "this screenshot" else "${selectedUris.size} screenshots"}? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteDialog = false
                            initiateDeletion(context, selectedUris.toList()) { request ->
                                deleteLauncher.launch(request)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

/**
 * 2. PROBLEM 1 & STATUS BAR FIXED: Full Screen Image Viewer Composable
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FullscreenViewer(
    screenshots: List<Uri>,
    initialIndex: Int,
    onBack: () -> Unit,
    onDelete: (Uri) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { screenshots.size })
    var showInfoSheet by remember { mutableStateOf(false) }
    var uiVisible by remember { mutableStateOf(true) }

    // Logic to hide/show status bar and navigation bar
    val window = (context as? Activity)?.window
    if (window != null) {
        val controller = remember { WindowCompat.getInsetsController(window, view) }
        LaunchedEffect(uiVisible) {
            if (uiVisible) {
                controller.show(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        DisposableEffect(Unit) {
            onDispose {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Horizontal Pager for swiping between images
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { uiVisible = !uiVisible },
            pageSpacing = 16.dp
        ) { page ->
            AsyncImage(
                model = screenshots[page],
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                alignment = Alignment.Center
            )
        }

        // Immersive Top Bar Overlay (Centered page count and Back button)
        AnimatedVisibility(
            visible = uiVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${screenshots.size}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.5f)
                )
            )
        }

        // Immersive Bottom Bar Overlay (Info, Delete, Share)
        AnimatedVisibility(
            visible = uiVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                contentColor = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showInfoSheet = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Info", tint = Color.White)
                    }
                    IconButton(onClick = { onDelete(screenshots[pagerState.currentPage]) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                    }
                    IconButton(onClick = { shareScreenshots(context, listOf(screenshots[pagerState.currentPage])) }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                    }
                }
            }
        }
    }

    if (showInfoSheet) {
        ScreenshotInfoSheet(
            uri = screenshots[pagerState.currentPage],
            onDismiss = { showInfoSheet = false }
        )
    }
}

/**
 * 3. PROBLEM 2 FIXED: Share Feature Logic - Supports single and multi-selection
 */
fun shareScreenshots(context: Context, uris: List<Uri>) {
    if (uris.isEmpty()) return
    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uris[0])
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        }
    }
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(intent, "Share Screenshots"))
}

/**
 * 4. Screenshot Info Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenshotInfoSheet(uri: Uri, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val details = remember(uri) { getScreenshotDetails(context, uri) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Details",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            
            details.forEach { (label, value) ->
                Column {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        value,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

/**
 * Helper to fetch detailed info from MediaStore
 */
fun getScreenshotDetails(context: Context, uri: Uri): Map<String, String> {
    val details = mutableMapOf<String, String>()
    val projection = arrayOf(
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.SIZE,
        MediaStore.Images.Media.DATE_ADDED,
        MediaStore.Images.Media.WIDTH,
        MediaStore.Images.Media.HEIGHT,
        MediaStore.Images.Media.DATA
    )

    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
            val size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE))
            val date = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED))
            val width = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH))
            val height = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT))
            val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))

            details["Name"] = name
            details["Size"] = Formatter.formatShortFileSize(context, size)
            details["Date"] = Date(date * 1000).toString()
            details["Resolution"] = "${width}x${height}"
            details["Path"] = path
        }
    }
    return details
}

/**
 * Helper to initiate media deletion.
 */
fun initiateDeletion(
    context: Context,
    uris: List<Uri>,
    onIntentSenderReady: (IntentSenderRequest) -> Unit
) {
    if (uris.isEmpty()) return
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, uris)
        onIntentSenderReady(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
    } else {
        uris.forEach { context.contentResolver.delete(it, null, null) }
    }
}

@Composable
fun HomeScreen(
    storageInfo: StorageInfo,
    totalCount: Int,
    recentScreenshots: List<Uri>,
    onViewAllClick: () -> Unit,
    onScreenshotClick: (Uri) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text(text = "Hello there,", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "Ready to clean up?", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
            }
        }
        item { StorageCard(storageInfo) }
        item { QuickActionsRow() }
        item { ScreenshotCountCard(totalCount) }
        item { 
            RecentScreenshotsSection(
                screenshots = recentScreenshots, 
                onViewAllClick = onViewAllClick,
                onScreenshotClick = onScreenshotClick
            ) 
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

/**
 * Updated Library Screen with Multi-Select and View Support
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    allScreenshots: List<Uri>,
    selectedUris: Set<Uri>,
    onToggleSelection: (Uri) -> Unit,
    onScreenshotClick: (Uri) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(text = "Your Collection (${allScreenshots.size})", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(vertical = 16.dp))
        
        if (allScreenshots.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No images found.") }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(allScreenshots) { uri ->
                    val isSelected = selectedUris.contains(uri)
                    Box(
                        modifier = Modifier
                            .aspectRatio(0.6f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .combinedClickable(
                                onClick = {
                                    if (selectedUris.isNotEmpty()) onToggleSelection(uri)
                                    else onScreenshotClick(uri)
                                },
                                onLongClick = { onToggleSelection(uri) }
                            )
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Screenshot",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alpha = if (isSelected) 0.5f else 1.0f
                        )
                        if (isSelected) {
                            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)), contentAlignment = Alignment.TopEnd) {
                                Surface(modifier = Modifier.padding(8.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp).padding(2.dp))
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
fun StorageCard(info: StorageInfo) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Storage Used", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Icon(Icons.Default.Storage, contentDescription = null)
            }
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(progress = { info.progress }, modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${info.usedSpace} of ${info.totalSpace} used", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("${info.freeSpace} free", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
fun QuickActionsRow() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        QuickActionButton("Scan", Icons.Default.Search, Modifier.weight(1f))
        QuickActionButton("Organize", Icons.Default.AutoAwesome, Modifier.weight(1f))
        QuickActionButton("Cleanup", Icons.Default.DeleteSweep, Modifier.weight(1f))
    }
}

@Composable
fun QuickActionButton(label: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.secondaryContainer, onClick = { }) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun ScreenshotCountCard(count: Int) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(modifier = Modifier.padding(24.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Image, contentDescription = null, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Total Screenshots Found", style = MaterialTheme.typography.labelLarge)
                Text("$count Images", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentScreenshotsSection(
    screenshots: List<Uri>,
    onViewAllClick: () -> Unit,
    onScreenshotClick: (Uri) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Recent Screenshots", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            TextButton(onClick = onViewAllClick) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("View All")
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp).padding(start = 4.dp))
                }
            }
        }
        if (screenshots.isEmpty()) {
            Text("No images found.", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(screenshots) { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = "Screenshot",
                        modifier = Modifier
                            .size(120.dp, 200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .combinedClickable(onClick = { onScreenshotClick(uri) }),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(selectedScreen: Screen, onScreenSelected: (Screen) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        NavigationBarItem(selected = selectedScreen == Screen.Home, onClick = { onScreenSelected(Screen.Home) }, icon = { Icon(Icons.Default.Dashboard, contentDescription = null) }, label = { Text("Home") })
        NavigationBarItem(selected = selectedScreen == Screen.Library, onClick = { onScreenSelected(Screen.Library) }, icon = { Icon(Icons.Default.Folder, contentDescription = null) }, label = { Text("Library") })
        NavigationBarItem(selected = selectedScreen == Screen.Settings, onClick = { onScreenSelected(Screen.Settings) }, icon = { Icon(Icons.Default.Settings, contentDescription = null) }, label = { Text("Settings") })
    }
}

fun fetchScreenshotsData(context: Context): ScreenshotResults {
    val screenshotsOnly = mutableListOf<Uri>()
    val allImages = mutableListOf<Uri>()
    val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATE_TAKEN)
    val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"
    val query = context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, sortOrder)
    query?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val name = cursor.getString(nameColumn) ?: ""
            val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            if (name.contains("Screenshot", ignoreCase = true)) screenshotsOnly.add(contentUri)
            allImages.add(contentUri)
        }
    }
    val hasScreenshots = screenshotsOnly.isNotEmpty()
    val finalAllList = if (hasScreenshots) screenshotsOnly else allImages
    return ScreenshotResults(recent = finalAllList.take(10), all = finalAllList, totalCount = if (hasScreenshots) screenshotsOnly.size else 0)
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DashboardPreview() {
    CleanShotTheme(darkTheme = true) { MainContainer() }
}
