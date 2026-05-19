package com.cleanshot.app.library.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import com.cleanshot.app.library.LibraryGridSpec
import com.cleanshot.app.library.libraryGridCells

@Composable
fun LibraryShimmerGrid(
    gridSpec: LibraryGridSpec,
    modifier: Modifier = Modifier,
    placeholderCount: Int = 9
) {
    LazyVerticalGrid(
        columns = libraryGridCells(gridSpec.columns),
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(gridSpec.spacing),
        verticalArrangement = Arrangement.spacedBy(gridSpec.spacing),
        contentPadding = PaddingValues(bottom = 24.dp),
        userScrollEnabled = false
    ) {
        items(placeholderCount, key = { "shimmer_$it" }) {
            LibraryShimmerCell(gridSpec = gridSpec)
        }
    }
}

@Composable
fun LibraryShimmerCell(
    gridSpec: LibraryGridSpec,
    modifier: Modifier = Modifier
) {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.surfaceContainerHigh
    val transition = rememberInfiniteTransition(label = "library_shimmer")
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )
    val brush = Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(offset * 400f - 200f, 0f),
        end = Offset(offset * 400f, 200f)
    )
    Box(
        modifier = modifier
            .aspectRatio(gridSpec.aspectRatio)
            .clip(RoundedCornerShape(gridSpec.cornerRadius))
            .background(brush)
    )
}

@Composable
fun LibraryItemLoadingPlaceholder(
    gridSpec: LibraryGridSpec,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        LibraryShimmerCell(gridSpec = gridSpec, modifier = Modifier.fillMaxSize())
    }
}
