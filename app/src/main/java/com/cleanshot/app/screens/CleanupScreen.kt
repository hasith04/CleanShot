package com.cleanshot.app.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.cleanshot.app.ui.theme.CleanShotTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanupScreen(
    screenshots: List<Uri>,
    onBack: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var keptCount by remember { mutableIntStateOf(0) }
    var deletedCount by remember { mutableIntStateOf(0) }

    // Swipe & Animation states
    val offsetX = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val swipeThreshold = with(density) { 100.dp.toPx() }
    val translationYPx = with(density) { 24.dp.toPx() }

    val currentScreenshot = screenshots.getOrNull(currentIndex)

    BackHandler { onBack() }

    fun next(isDelete: Boolean) {
        scope.launch {
            val target = if (isDelete) 2000f else -2000f
            offsetX.animateTo(target, tween(400, easing = FastOutSlowInEasing))
            if (isDelete) deletedCount++ else keptCount++
            currentIndex++
            offsetX.snapTo(0f)
            rotation.snapTo(0f)
        }
    }

    val isDark = isSystemInDarkTheme()
    val backgroundColor = if (isDark)
        Color.Black
    else
        Color(0xFFEFF4F4)
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            /*
             * Back Button
             */
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 44.dp, start = 14.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = if (isDark) Color.White else Color(0xFF111111)
                )
            }

            /*
             * Main Layout
             */
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 25.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                 // Header section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(98.dp))
                    Text(
                        text = "Cleanup",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color.White else Color(0xFF111111)

                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${currentIndex + 1} / ${screenshots.size} reviewed",
                        fontSize = 15.sp,
                        color = if (isDark)
                            Color.White.copy(alpha = 0.55f)
                        else
                            Color(0xFF707070)
                    )
                }

                // Refinement 1: Vertical spacing before card (40dp)
                Spacer(modifier = Modifier.height(5.dp))

                /*
                 * Screenshot Card Area
                 */
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentIndex < screenshots.size) {
                        // Next Card (Stacked Preview Effect)
                        val nextScreenshot = screenshots.getOrNull(currentIndex + 1)
                        if (nextScreenshot != null) {
                            val dragProgress = (abs(offsetX.value) / swipeThreshold).coerceIn(0f, 1f)
                            val nextScale = 0.94f + (dragProgress * 0.06f)
                            val nextAlpha = 0.7f + (dragProgress * 0.3f)
                            val nextTranslationY = translationYPx * (1f - dragProgress)

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.72f) // Refinement 2: Width 82%
                                    .aspectRatio(9f / 18.5f)
                                    .graphicsLayer {
                                        scaleX = nextScale
                                        scaleY = nextScale
                                        alpha = nextAlpha
                                        translationY = nextTranslationY
                                    }
                                    .shadow(
                                        elevation = 12.dp,
                                        shape = RoundedCornerShape(24.dp),
                                        ambientColor = Color.Black.copy(alpha = 0.1f),
                                        spotColor = Color.Black.copy(alpha = 0.15f)
                                    )
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Color.Black)
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(nextScreenshot),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }

                        if (currentScreenshot != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.72f) // Refinement 2: Width 82%
                                    .aspectRatio(9f / 19.5f)
                                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                                    .graphicsLayer {
                                        rotationZ = rotation.value
                                        // Premium Shadow vertical offset
                                        translationY = -abs(offsetX.value / 40f)
                                    }
                                    .shadow(
                                        elevation = 18.dp, // Refinement 4: Softer larger shadow
                                        shape = RoundedCornerShape(28.dp),
                                        ambientColor = Color.Black.copy(alpha = 0.12f),
                                        spotColor = Color.Black.copy(alpha = 0.2f)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = Color.Black.copy(alpha = 0.05f),
                                        shape = RoundedCornerShape(28.dp)
                                    )
                                    .clip(RoundedCornerShape(28.dp))
                                    .background(Color.Black)
                                    .pointerInput(currentIndex) {
                                        detectDragGestures(
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                scope.launch {
                                                    offsetX.snapTo(offsetX.value + dragAmount.x)
                                                    rotation.snapTo(offsetX.value / 30f)
                                                }
                                            },
                                            onDragEnd = {
                                                if (offsetX.value > swipeThreshold) {
                                                    next(isDelete = true)
                                                } else if (offsetX.value < -swipeThreshold) {
                                                    next(isDelete = false)
                                                } else {
                                                    scope.launch {
                                                        launch { offsetX.animateTo(0f, spring()) }
                                                        launch { rotation.animateTo(0f, spring()) }
                                                    }
                                                }
                                            }
                                        )
                                    }
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(currentScreenshot),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                
                                // Swipe Hints
                                val hintAlpha = (abs(offsetX.value) / swipeThreshold).coerceIn(0f, 1f)
                                if (offsetX.value > 0) {
                                    SwipeActionHint(text = "DELETE", color = Color.Red, alpha = hintAlpha, isRight = true)
                                } else if (offsetX.value < 0) {
                                    SwipeActionHint(text = "KEEP", color = Color.Green, alpha = hintAlpha, isRight = false)
                                }
                            }
                        }
                    } else {
                        CleanupCompleteView(keptCount, deletedCount, onBack)
                    }
                }

                if (currentIndex < screenshots.size) {
                    // Refinement 5: Spacing between card and buttons (32dp)
                    Spacer(modifier = Modifier.height(32.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.padding(bottom = 35.dp),
                        horizontalArrangement = Arrangement.spacedBy(48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // KEEP button
                        CircularActionButton(
                            icon = Icons.Default.Check,
                            color = Color(0xFF2E7D32),
                            backgroundColor = Color(0xFFEAF5EA),
                            onClick = { next(false) }
                        )

                        // DELETE button
                        CircularActionButton(
                            icon = Icons.Default.Delete,
                            color = Color(0xFFD32F2F),
                            backgroundColor = Color(0xFFFFECEC),
                            onClick = { next(true) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SwipeActionHint(text: String, color: Color, alpha: Float, isRight: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        contentAlignment = if (isRight) Alignment.TopEnd else Alignment.TopStart
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.7f * alpha),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(2.dp, color.copy(alpha = alpha)),
            modifier = Modifier.graphicsLayer { rotationZ = if (isRight) 15f else -15f }
        ) {
            Text(
                text = text,
                color = color.copy(alpha = alpha),
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
fun CircularActionButton(
    icon: ImageVector,
    color: Color,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier
            .size(width = 110.dp, height = 72.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(28.dp)
            )        ,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = backgroundColor,
            contentColor = color
        )
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(32.dp))
    }
}

@Composable
fun CleanupCompleteView(kept: Int, deleted: Int, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(100.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(28.dp))
        Text(
            text = "Cleanup Complete ✨",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "You kept $kept and deleted $deleted images.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(45.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text("Done", fontWeight = FontWeight.Bold)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CleanupScreenPreview() {
    CleanShotTheme {
        CleanupScreen(
            screenshots = listOf(Uri.EMPTY, Uri.EMPTY, Uri.EMPTY),
            onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CleanupCompleteViewPreview() {
    CleanShotTheme {
        CleanupCompleteView(
            kept = 12,
            deleted = 5,
            onBack = {}
        )
    }
}
