package com.cleanshot.app.screens

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cleanshot.app.components.ComingSoonSheet
import com.cleanshot.app.components.StorageCard
import com.cleanshot.app.home.ScanButtonUiState
import com.cleanshot.app.models.StorageInfo

private val QuickActionButtonHeight = 88.dp
private val QuickActionIconBoxSize = 24.dp
private val QuickActionIconSize = 22.dp
private val QuickActionCornerRadius = 20.dp
private val QuickActionHorizontalPadding = 8.dp
private val QuickActionVerticalPadding = 14.dp

@Composable
fun HomeScreen(
    storageInfo: StorageInfo,
    recentScreenshots: List<Uri>,
    scanButtonState: ScanButtonUiState,
    lastScannedLabel: String?,
    isScanEnabled: Boolean,
    onScanClick: () -> Unit,
    onOrganizeClick: () -> Unit,
    onCleanupClick: () -> Unit,
    showOrganizeComingSoon: Boolean,
    onDismissOrganizeComingSoon: () -> Unit,
    onViewAllClick: () -> Unit,
    onScreenshotClick: (Uri) -> Unit
) {
    if (showOrganizeComingSoon) {
        ComingSoonSheet(
            title = "Organize is coming soon ✨",
            message = "Smart screenshot organization is currently in development.",
            onDismiss = onDismissOrganizeComingSoon
        )
    }

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
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        item {
            StorageCard(storageInfo)
        }

        item {
            QuickActionsRow(
                scanButtonState = scanButtonState,
                lastScannedLabel = lastScannedLabel,
                isScanEnabled = isScanEnabled,
                onScanClick = onScanClick,
                onOrganizeClick = onOrganizeClick,
                onCleanupClick = onCleanupClick
            )
        }

        item {
            RecentScreenshotsSection(
                screenshots = recentScreenshots,
                onViewAllClick = onViewAllClick,
                onScreenshotClick = onScreenshotClick
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun QuickActionsRow(
    scanButtonState: ScanButtonUiState,
    lastScannedLabel: String?,
    isScanEnabled: Boolean,
    onScanClick: () -> Unit,
    onOrganizeClick: () -> Unit,
    onCleanupClick: () -> Unit
) {
    val isScanning = scanButtonState is ScanButtonUiState.Scanning
    val scanLabel = when (scanButtonState) {
        ScanButtonUiState.Idle -> "Scan"
        ScanButtonUiState.Scanning -> "Scanning..."
        is ScanButtonUiState.Result -> "Scan"
    }
    val rowMetadata = when (scanButtonState) {
        is ScanButtonUiState.Result -> scanButtonState.message
        else -> lastScannedLabel
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            ScanQuickActionButton(
                label = scanLabel,
                enabled = isScanEnabled,
                isScanning = isScanning,
                onClick = onScanClick,
                modifier = Modifier.weight(1f)
            )

            HomeQuickActionButton(
                label = "Organize",
                icon = Icons.Default.AutoAwesome,
                iconScale = 0.9f,
                onClick = onOrganizeClick,
                modifier = Modifier.weight(1f)
            )

            HomeQuickActionButton(
                label = "Cleanup",
                icon = Icons.Default.DeleteSweep,
                onClick = onCleanupClick,
                modifier = Modifier.weight(1f)
            )
        }

        if (rowMetadata != null) {
            Spacer(modifier = Modifier.height(8.dp))
            AnimatedContent(
                targetState = rowMetadata,
                transitionSpec = {
                    fadeIn(tween(180)) togetherWith fadeOut(tween(140))
                },
                label = "scan_row_metadata"
            ) { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ScanQuickActionButton(
    label: String,
    enabled: Boolean,
    isScanning: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pulseTransition = rememberInfiniteTransition(label = "scan_pulse")
    val animatedPulse by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_pulse_scale"
    )
    val pulseScale = if (isScanning) animatedPulse else 1f

    HomeQuickActionButton(
        label = label,
        icon = Icons.Default.Search,
        modifier = modifier.scale(pulseScale),
        enabled = enabled && !isScanning,
        isLoading = isScanning,
        animateLabel = true,
        onClick = onClick
    )
}

@Composable
private fun HomeQuickActionButton(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    iconScale: Float = 1f,
    animateLabel: Boolean = false,
    onClick: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val iconTint = MaterialTheme.colorScheme.onSecondaryContainer

    Surface(
        modifier = modifier
            .height(QuickActionButtonHeight)
            .fillMaxWidth(),
        shape = RoundedCornerShape(QuickActionCornerRadius),
        color = MaterialTheme.colorScheme.secondaryContainer,
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        enabled = enabled && !isLoading
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = QuickActionHorizontalPadding,
                    vertical = QuickActionVerticalPadding
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(QuickActionIconBoxSize),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(QuickActionIconSize),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = iconTint,
                        modifier = Modifier
                            .size(QuickActionIconSize)
                            .scale(iconScale)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (animateLabel) {
                AnimatedContent(
                    targetState = label,
                    transitionSpec = {
                        fadeIn(tween(180)) togetherWith fadeOut(tween(140))
                    },
                    label = "quick_action_label"
                ) { animatedLabel ->
                    QuickActionLabel(text = animatedLabel)
                }
            } else {
                QuickActionLabel(text = label)
            }
        }
    }
}

@Composable
private fun QuickActionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentScreenshotsSection(
    screenshots: List<Uri>,
    onViewAllClick: () -> Unit,
    onScreenshotClick: (Uri) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Screenshots",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            TextButton(onClick = onViewAllClick) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("View All")
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(start = 4.dp)
                    )
                }
            }
        }

        if (screenshots.isEmpty()) {
            Text(
                text = "No images found.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(screenshots, key = { it.toString() }) { uri ->
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
