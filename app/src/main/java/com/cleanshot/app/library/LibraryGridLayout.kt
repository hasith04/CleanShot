package com.cleanshot.app.library

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

object LibraryGridLayout {
    val horizontalPadding = 16.dp
    const val CONTENT_TYPE_HEADER = "header"
    const val CONTENT_TYPE_ITEM = "screenshot"
}

data class LibraryThumbnailSize(val widthPx: Int, val heightPx: Int)

@Composable
fun rememberLibraryThumbnailSize(gridSpec: LibraryGridSpec): LibraryThumbnailSize {
    val density = LocalDensity.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    return remember(density, screenWidthDp, gridSpec.columns, gridSpec.spacing, gridSpec.aspectRatio) {
        with(density) {
            val totalHorizontalPadding = LibraryGridLayout.horizontalPadding.roundToPx() * 2
            val totalSpacing =
                gridSpec.spacing.roundToPx() * (gridSpec.columns - 1)
            val cellWidth =
                (screenWidthDp.dp.roundToPx() - totalHorizontalPadding - totalSpacing) /
                    gridSpec.columns
            val cellHeight = (cellWidth / gridSpec.aspectRatio).roundToInt()
            LibraryThumbnailSize(cellWidth, cellHeight)
        }
    }
}

@Composable
fun rememberAnimatedGridSpec(viewMode: LibraryViewMode): LibraryGridSpec {
    val target = viewMode.toGridSpec()
    val spacing by animateDpAsState(
        targetValue = target.spacing,
        animationSpec = tween(durationMillis = 280),
        label = "library_grid_spacing"
    )
    return target.copy(spacing = spacing)
}

fun libraryGridCells(columns: Int): GridCells = GridCells.Fixed(columns)
