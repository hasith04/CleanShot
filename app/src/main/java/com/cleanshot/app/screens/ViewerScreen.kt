package com.cleanshot.app.screens

import android.app.Activity
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import com.cleanshot.app.utils.getScreenshotDetails
import com.cleanshot.app.utils.shareScreenshots
import kotlinx.coroutines.launch
import java.util.Date

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
    var isZoomed by remember { mutableStateOf(false) }

    // Reset zoom state when page changes
    LaunchedEffect(pagerState.currentPage) {
        isZoomed = false
    }

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
    var currentScale by remember {
        mutableFloatStateOf(1f)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Horizontal Pager for swiping between images
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            pageSpacing = 16.dp,
            userScrollEnabled = currentScale <= 1f,
        ) { page ->
            ZoomableImage(
                uri = screenshots[page],
                onTap = { uiVisible = !uiVisible },
                onZoomChanged = { isZoomed = it }
            )
        }

        // Immersive Top Bar Overlay
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

        // Immersive Bottom Bar Overlay
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

@Composable
fun ZoomableImage(uri: Uri, onTap: () -> Unit, onZoomChanged: (Boolean) -> Unit) {
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        val newScale = (scale.value * zoomChange).coerceIn(1f, 5f)
        onZoomChanged(newScale > 1f)
        scope.launch {
            scale.snapTo(newScale)
        }
        if (newScale > 1f) {
            offset += offsetChange
        } else {
            offset = Offset.Zero
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(scale.value) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        scope.launch {
                            if (scale.value > 1f) {
                                offset = Offset.Zero
                                onZoomChanged(false)
                                scale.animateTo(1f)
                            } else {
                                onZoomChanged(true)
                                scale.animateTo(2.5f)
                            }
                        }
                    }
                )
            }
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                translationX = offset.x * scale.value
                translationY = offset.y * scale.value
            }
            .then(
                if (scale.value > 1f) {
                    Modifier.transformable(state = state)
                } else {
                    Modifier
                }
            )
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            alignment = Alignment.Center
        )
    }
}

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
