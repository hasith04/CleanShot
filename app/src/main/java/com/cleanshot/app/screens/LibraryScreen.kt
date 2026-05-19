package com.cleanshot.app.screens

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cleanshot.app.library.LibraryDateGrouping
import com.cleanshot.app.library.LibraryDisplayPreferences
import com.cleanshot.app.library.LibraryDisplaySettings
import com.cleanshot.app.library.LibraryGridEntry
import com.cleanshot.app.library.LibraryGridLayout
import com.cleanshot.app.library.LibraryGridSpec
import com.cleanshot.app.library.LibrarySearchFilter
import com.cleanshot.app.library.LibrarySectionBuilder
import com.cleanshot.app.library.LibraryThumbnailSize
import com.cleanshot.app.library.libraryGridCells
import com.cleanshot.app.library.rememberAnimatedGridSpec
import com.cleanshot.app.library.rememberLibraryThumbnailSize
import com.cleanshot.app.library.ui.LibraryEmptyState
import com.cleanshot.app.library.ui.LibraryGridItem
import com.cleanshot.app.library.ui.LibrarySearchField
import com.cleanshot.app.library.ui.LibrarySectionHeader
import com.cleanshot.app.library.ui.LibraryShimmerGrid
import com.cleanshot.app.library.ui.LibraryViewOptionsSheet
import com.cleanshot.app.models.LibrarySearchQuery
import com.cleanshot.app.models.ScreenshotItem
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, FlowPreview::class)
@Composable
fun LibraryScreen(
    items: List<ScreenshotItem>,
    selectedUris: Set<Uri>,
    isLoading: Boolean = false,
    optionsSheetOpen: Boolean = false,
    onOptionsSheetDismiss: () -> Unit = {},
    onToggleSelection: (Uri) -> Unit,
    onScreenshotClick: (Uri) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val displayPreferences = remember { LibraryDisplayPreferences(context) }

    var displaySettings by remember {
        mutableStateOf(LibraryDisplaySettings.DEFAULT)
    }

    LaunchedEffect(displayPreferences) {
        displayPreferences.displaySettingsFlow.collect { displaySettings = it }
    }

    var searchInput by remember { mutableStateOf("") }
    var debouncedSearch by remember { mutableStateOf("") }
    LaunchedEffect(searchInput) {
        snapshotFlow { searchInput }
            .debounce(280)
            .distinctUntilChanged()
            .collect { debouncedSearch = it }
    }

    val searchQuery = remember(debouncedSearch) {
        LibrarySearchQuery(text = debouncedSearch)
    }

    val filteredItems = remember(items, searchQuery) {
        LibrarySearchFilter.filterItems(items, searchQuery)
    }

    val gridEntries = remember(filteredItems, displaySettings.sortOrder) {
        LibrarySectionBuilder.buildGridEntries(filteredItems, displaySettings.sortOrder)
    }

    val gridSpec = rememberAnimatedGridSpec(displaySettings.viewMode)
    val thumbnailSize = rememberLibraryThumbnailSize(gridSpec)

    val gridVisualKey = remember(displaySettings.viewMode, displaySettings.sortOrder) {
        "${displaySettings.viewMode.name}_${displaySettings.sortOrder.name}"
    }
    val gridScale = remember { Animatable(1f) }
    LaunchedEffect(gridVisualKey) {
        gridScale.snapTo(0.96f)
        gridScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }

    val selectionMode by remember {
        derivedStateOf { selectedUris.isNotEmpty() }
    }

    val colorScheme = MaterialTheme.colorScheme
    val surfaceVariantColor = colorScheme.surfaceVariant
    val primaryColor = colorScheme.primary
    val selectionOverlayColor = remember(primaryColor) {
        primaryColor.copy(alpha = 0.22f)
    }

    val onToggleSelectionState by rememberUpdatedState(onToggleSelection)
    val onScreenshotClickState by rememberUpdatedState(onScreenshotClick)

    val collectionSubtitle = remember(items, filteredItems.size, searchQuery.isActive) {
        val countText = when {
            searchQuery.isActive && filteredItems.size != items.size ->
                "${filteredItems.size} of ${items.size} screenshots"
            else -> "${items.size} screenshots"
        }
        val updatedSuffix = LibraryDateGrouping.collectionUpdatedSuffix(items)
        if (updatedSuffix != null) "$countText • $updatedSuffix" else countText
    }

    if (optionsSheetOpen) {
        LibraryViewOptionsSheet(
            viewMode = displaySettings.viewMode,
            sortOrder = displaySettings.sortOrder,
            onViewModeSelected = { mode ->
                scope.launch { displayPreferences.saveViewMode(mode) }
            },
            onSortOrderSelected = { order ->
                scope.launch { displayPreferences.saveSortOrder(order) }
            },
            onDismiss = onOptionsSheetDismiss
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = LibraryGridLayout.horizontalPadding)
    ) {
        LibrarySearchField(
            query = searchInput,
            onQueryChange = { searchInput = it },
            modifier = Modifier.padding(top = 4.dp)
        )

        Text(
            text = collectionSubtitle,
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
            modifier = Modifier.padding(top = 6.dp, bottom = 10.dp)
        )

        AnimatedContent(
            targetState = when {
                isLoading -> LibraryContentState.Loading
                gridEntries.isEmpty() -> LibraryContentState.Empty
                else -> LibraryContentState.Grid
            },
            transitionSpec = {
                fadeIn(tween(220)) togetherWith fadeOut(tween(180))
            },
            label = "library_content"
        ) { state ->
            when (state) {
                LibraryContentState.Loading -> {
                    LibraryShimmerGrid(
                        gridSpec = gridSpec,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                LibraryContentState.Empty -> {
                    LibraryEmptyState(hasSearchQuery = searchQuery.isActive)
                }

                LibraryContentState.Grid -> {
                    LibraryScreenshotGrid(
                        gridEntries = gridEntries,
                        gridSpec = gridSpec,
                        gridScale = gridScale.value,
                        thumbnailSize = thumbnailSize,
                        selectedUris = selectedUris,
                        selectionMode = selectionMode,
                        selectionOverlayColor = selectionOverlayColor,
                        primaryColor = primaryColor,
                        surfaceVariantColor = surfaceVariantColor,
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
private fun LibraryScreenshotGrid(
    gridEntries: List<LibraryGridEntry>,
    gridSpec: LibraryGridSpec,
    gridScale: Float,
    thumbnailSize: LibraryThumbnailSize,
    selectedUris: Set<Uri>,
    selectionMode: Boolean,
    selectionOverlayColor: Color,
    primaryColor: Color,
    surfaceVariantColor: Color,
    onToggleSelection: (Uri) -> Unit,
    onScreenshotClick: (Uri) -> Unit
) {
    LazyVerticalGrid(
        columns = libraryGridCells(gridSpec.columns),
        horizontalArrangement = Arrangement.spacedBy(gridSpec.spacing),
        verticalArrangement = Arrangement.spacedBy(gridSpec.spacing),
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = gridScale
                scaleY = gridScale
            },
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        items(
            items = gridEntries,
            key = { entry -> entry.key },
            span = { entry ->
                if (entry is LibraryGridEntry.Header) {
                    GridItemSpan(gridSpec.columns)
                } else {
                    GridItemSpan(1)
                }
            },
            contentType = { entry ->
                when (entry) {
                    is LibraryGridEntry.Header -> LibraryGridLayout.CONTENT_TYPE_HEADER
                    is LibraryGridEntry.Shot -> LibraryGridLayout.CONTENT_TYPE_ITEM
                }
            }
        ) { entry ->
            when (entry) {
                is LibraryGridEntry.Header -> {
                    LibrarySectionHeader(title = entry.title)
                }

                is LibraryGridEntry.Shot -> {
                    val uri = entry.item.uri
                    val isSelected = uri in selectedUris
                    LibraryGridItem(
                        uri = uri,
                        isSelected = isSelected,
                        selectionMode = selectionMode,
                        gridSpec = gridSpec,
                        thumbnailSize = thumbnailSize,
                        selectionOverlayColor = selectionOverlayColor,
                        primaryColor = primaryColor,
                        surfaceVariantColor = surfaceVariantColor,
                        onClick = {
                            if (selectionMode) {
                                onToggleSelection(uri)
                            } else {
                                onScreenshotClick(uri)
                            }
                        },
                        onLongClick = { onToggleSelection(uri) },
                        modifier = Modifier
                            .animateItem(
                                fadeInSpec = tween(
                                    durationMillis = 320,
                                    delayMillis = (entry.staggerIndex % 12) * 28
                                ),
                                fadeOutSpec = tween(180),
                                placementSpec = tween(280)
                            )
                    )
                }
            }
        }
    }
}

private enum class LibraryContentState {
    Loading,
    Empty,
    Grid
}
