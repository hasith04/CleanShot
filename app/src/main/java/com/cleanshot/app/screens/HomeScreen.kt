package com.cleanshot.app.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cleanshot.app.components.StorageCard
import com.cleanshot.app.models.StorageInfo
import androidx.compose.foundation.ExperimentalFoundationApi

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
