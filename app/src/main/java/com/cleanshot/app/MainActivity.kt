package com.cleanshot.app

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.text.format.Formatter
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ChevronRight
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.cleanshot.app.ui.theme.CleanShotTheme

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
 * 1. Simple Navigation Screen setup
 */
enum class Screen {
    Home, Library, Settings
}

/**
 * Result wrapper for screenshot fetching
 */
data class ScreenshotData(
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
    val blockSize = stat.blockSizeLong
    val totalBlocks = stat.blockCountLong
    val availableBlocks = stat.availableBlocksLong
    
    val totalBytes = totalBlocks * blockSize
    val availableBytes = availableBlocks * blockSize
    val usedBytes = totalBytes - availableBytes
    
    val totalSpaceStr = Formatter.formatShortFileSize(context, totalBytes)
    val usedSpaceStr = Formatter.formatShortFileSize(context, usedBytes)
    val freeSpaceStr = Formatter.formatShortFileSize(context, availableBytes)
    
    val progress = if (totalBytes > 0) usedBytes.toFloat() / totalBytes.toFloat() else 0f
    
    return StorageInfo(totalSpaceStr, usedSpaceStr, freeSpaceStr, progress)
}

/**
 * Main Container to handle Screen Switching
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContainer() {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(Screen.Home) }
    
    // Screenshot State
    var screenshotData by remember { mutableStateOf(ScreenshotData(emptyList(), emptyList(), 0)) }
    
    val storageInfo = remember(screenshotData) { getStorageInfo(context) }

    val permissionNeeded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            screenshotData = fetchScreenshotsData(context)
        }
    }

    LaunchedEffect(Unit) {
        val isGranted = ContextCompat.checkSelfPermission(context, permissionNeeded) == PackageManager.PERMISSION_GRANTED
        if (isGranted) {
            screenshotData = fetchScreenshotsData(context)
        } else {
            launcher.launch(permissionNeeded)
        }
    }

    Scaffold(
        topBar = {
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
        },
        bottomBar = {
            BottomNavigationBar(
                selectedScreen = currentScreen,
                onScreenSelected = { currentScreen = it }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentScreen) {
                Screen.Home -> HomeScreenContent(
                    storageInfo = storageInfo,
                    screenshotData = screenshotData,
                    onViewAllClick = { currentScreen = Screen.Library }
                )
                Screen.Library -> LibraryScreen(screenshotData.all)
                Screen.Settings -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Settings Coming Soon")
                }
            }
        }
    }
}

@Composable
fun HomeScreenContent(
    storageInfo: StorageInfo,
    screenshotData: ScreenshotData,
    onViewAllClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    text = "Hello there,",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Ready to clean up?",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        item { StorageCard(storageInfo) }
        item { QuickActionsRow() }
        
        item { 
            ScreenshotCountCard(screenshotData.totalCount) 
        }

        item {
            RecentScreenshotsSection(
                screenshots = screenshotData.recent,
                onViewAllClick = onViewAllClick
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

/**
 * 2. New Library Screen with Grid Layout
 */
@Composable
fun LibraryScreen(allScreenshots: List<Uri>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "All Screenshots (${allScreenshots.size})",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(vertical = 16.dp)
        )
        
        if (allScreenshots.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No images found.")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(allScreenshots) { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        modifier = Modifier
                            .aspectRatio(0.6f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
fun StorageCard(info: StorageInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Storage Used",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Icon(Icons.Default.Storage, contentDescription = null)
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            LinearProgressIndicator(
                progress = { info.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${info.usedSpace} of ${info.totalSpace} used",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "${info.freeSpace} free",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun QuickActionsRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickActionButton("Scan", Icons.Default.Search, Modifier.weight(1f))
        QuickActionButton("Organize", Icons.Default.AutoAwesome, Modifier.weight(1f))
        QuickActionButton("Cleanup", Icons.Default.DeleteSweep, Modifier.weight(1f))
    }
}

@Composable
fun QuickActionButton(label: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        onClick = { /* Action later */ }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun ScreenshotCountCard(count: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
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

/**
 * 3. Updated Recent Section with "View All" Button
 */
@Composable
fun RecentScreenshotsSection(screenshots: List<Uri>, onViewAllClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Recent Screenshots",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            TextButton(onClick = onViewAllClick) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("View All")
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        
        if (screenshots.isEmpty()) {
            Text("No images found. Try taking a screenshot!", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(screenshots) { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = "Screenshot",
                        modifier = Modifier
                            .size(120.dp, 200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(selectedScreen: Screen, onScreenSelected: (Screen) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
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
            label = { Text("Library") }
        )
        NavigationBarItem(
            selected = selectedScreen == Screen.Settings,
            onClick = { onScreenSelected(Screen.Settings) },
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text("Settings") }
        )
    }
}

/**
 * 4. Improved MediaStore query for REAL total count
 */
fun fetchScreenshotsData(context: Context): ScreenshotData {
    val screenshotsOnly = mutableListOf<Uri>()
    val fallbackImages = mutableListOf<Uri>()

    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.DATE_TAKEN
    )

    val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

    val query = context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        null,
        null,
        sortOrder
    )

    query?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val name = cursor.getString(nameColumn) ?: ""

            val contentUri = ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                id
            )
            
            if (name.contains("Screenshot", ignoreCase = true)) {
                screenshotsOnly.add(contentUri)
            }
            
            // Collect any image as fallback (limited for performance)
            if (fallbackImages.size < 50) {
                fallbackImages.add(contentUri)
            }
        }
    }
    
    // Logic: Use screenshots if found, otherwise fallback to any recent images (for emulators)
    val hasScreenshots = screenshotsOnly.isNotEmpty()
    val fullList = if (hasScreenshots) screenshotsOnly else fallbackImages
    
    return ScreenshotData(
        recent = fullList.take(10),
        all = fullList,
        totalCount = fullList.size
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DashboardPreview() {
    CleanShotTheme(darkTheme = true) {
        MainContainer()
    }
}
