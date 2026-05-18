package com.cleanshot.app.screens

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Scale

private val LibraryGridItemShape = RoundedCornerShape(12.dp)
private const val LibraryGridColumns = 3
private const val LibraryGridItemAspectRatio = 0.6f
private const val LibraryGridContentType = "library_screenshot"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    allScreenshots: List<Uri>,
    selectedUris: Set<Uri>,
    onToggleSelection: (Uri) -> Unit,
    onScreenshotClick: (Uri) -> Unit
) {
    val screenshotUris = remember(allScreenshots) { allScreenshots }

    val collectionTitle = remember(screenshotUris.size) {
        "Your Collection (${screenshotUris.size})"
    }
    val selectionMode by remember {
        derivedStateOf { selectedUris.isNotEmpty() }
    }

    val colorScheme = MaterialTheme.colorScheme
    val surfaceVariantColor = colorScheme.surfaceVariant
    val primaryColor = colorScheme.primary
    val selectionOverlayColor = remember(primaryColor) {
        primaryColor.copy(alpha = 0.2f)
    }

    val onToggleSelectionState by rememberUpdatedState(onToggleSelection)
    val onScreenshotClickState by rememberUpdatedState(onScreenshotClick)

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(
            text = collectionTitle,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(vertical = 16.dp)
        )

        if (screenshotUris.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No images found.")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(LibraryGridColumns),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(
                    items = screenshotUris,
                    key = { uri -> uri.toString() },
                    contentType = { LibraryGridContentType }
                ) { uri ->
                    LibraryScreenshotGridItem(
                        uri = uri,
                        isSelected = uri in selectedUris,
                        selectionMode = selectionMode,
                        surfaceVariantColor = surfaceVariantColor,
                        primaryColor = primaryColor,
                        selectionOverlayColor = selectionOverlayColor,
                        onToggleSelection = onToggleSelectionState,
                        onScreenshotClick = onScreenshotClickState
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryScreenshotGridItem(
    uri: Uri,
    isSelected: Boolean,
    selectionMode: Boolean,
    surfaceVariantColor: Color,
    primaryColor: Color,
    selectionOverlayColor: Color,
    onToggleSelection: (Uri) -> Unit,
    onScreenshotClick: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp

    val thumbnailSizePx = remember(density, screenWidthDp) {
        with(density) {
            val totalHorizontalPadding = 32.dp.roundToPx()
            val totalSpacing = 8.dp.roundToPx() * (LibraryGridColumns - 1)
            val cellWidth =
                (screenWidthDp.dp.roundToPx() - totalHorizontalPadding - totalSpacing) /
                    LibraryGridColumns
            val cellHeight = (cellWidth / LibraryGridItemAspectRatio).roundToInt()
            cellWidth to cellHeight
        }
    }

    val imageRequest = remember(uri, thumbnailSizePx) {
        val (widthPx, heightPx) = thumbnailSizePx
        ImageRequest.Builder(context)
            .data(uri)
            .size(widthPx, heightPx)
            .scale(Scale.FILL)
            .crossfade(false)
            .memoryCacheKey(uri.toString())
            .diskCacheKey(uri.toString())
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    val onClick = remember(uri, selectionMode, onToggleSelection, onScreenshotClick) {
        {
            if (selectionMode) {
                onToggleSelection(uri)
            } else {
                onScreenshotClick(uri)
            }
        }
    }
    val onLongClick = remember(uri, onToggleSelection) {
        { onToggleSelection(uri) }
    }

    Box(
        modifier = modifier
            .aspectRatio(LibraryGridItemAspectRatio)
            .clip(LibraryGridItemShape)
            .background(surfaceVariantColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = if (isSelected) 0.5f else 1f
                },
            contentScale = ContentScale.Crop
        )

        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(selectionOverlayColor),
                contentAlignment = Alignment.TopEnd
            ) {
                Surface(
                    modifier = Modifier.padding(8.dp),
                    shape = CircleShape,
                    color = primaryColor
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp).padding(2.dp)
                    )
                }
            }
        }
    }
}
