package com.cleanshot.app.library.ui

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Scale
import com.cleanshot.app.library.LibraryGridSpec
import com.cleanshot.app.library.LibraryThumbnailSize
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryGridItem(
    uri: Uri,
    isSelected: Boolean,
    selectionMode: Boolean,
    gridSpec: LibraryGridSpec,
    thumbnailSize: LibraryThumbnailSize,
    selectionOverlayColor: Color,
    primaryColor: Color,
    surfaceVariantColor: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val imageRequest = remember(uri, thumbnailSize) {
        ImageRequest.Builder(context)
            .data(uri)
            .size(thumbnailSize.widthPx, thumbnailSize.heightPx)
            .scale(Scale.FILL)
            .crossfade(220)
            .memoryCacheKey(uri.toString())
            .diskCacheKey(uri.toString())
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    val scale by animateFloatAsState(
        targetValue = when {
            isSelected -> 0.96f
            selectionMode -> 0.98f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "selection_scale"
    )

    val dimAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.55f else 1f,
        animationSpec = tween(200),
        label = "selection_dim"
    )

    val shape = remember(gridSpec.cornerRadius) { RoundedCornerShape(gridSpec.cornerRadius) }
    var suppressClickAfterLongPress by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .aspectRatio(gridSpec.aspectRatio)
            .scale(scale)
            .shadow(
                elevation = if (isSelected) 6.dp else gridSpec.elevation,
                shape = shape,
                clip = false
            )
            .clip(shape)
            .background(surfaceVariantColor)
            .semantics { role = Role.Button }
            .combinedClickable(
                onClick = {
                    if (suppressClickAfterLongPress) {
                        suppressClickAfterLongPress = false
                        return@combinedClickable
                    }
                    onClick()
                },
                onLongClick = {
                    suppressClickAfterLongPress = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
    ) {
        SubcomposeAsyncImage(
            model = imageRequest,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = dimAlpha },
            contentScale = ContentScale.Crop,
            loading = {
                LibraryItemLoadingPlaceholder(gridSpec = gridSpec)
            },
            success = {
                SubcomposeAsyncImageContent()
            },
            error = {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        )

        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn(tween(150)) + scaleIn(
                initialScale = 0.6f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
            exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.6f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(selectionOverlayColor),
                contentAlignment = Alignment.TopEnd
            ) {
                Surface(
                    modifier = Modifier.padding(8.dp),
                    shape = CircleShape,
                    color = primaryColor,
                    shadowElevation = 4.dp
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(22.dp)
                            .padding(3.dp)
                    )
                }
            }
        }
    }
}
