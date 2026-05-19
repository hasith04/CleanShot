package com.cleanshot.app.components

import android.text.format.Formatter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cleanshot.app.models.StorageInfo

@Composable
fun StorageCard(info: StorageInfo) {
    val context = LocalContext.current

    val animatedBytes by animateFloatAsState(
        targetValue = info.screenshotBytesUsed.toFloat(),
        animationSpec = tween(durationMillis = 450),
        label = "screenshot_bytes"
    )
    val animatedCount by animateIntAsState(
        targetValue = info.screenshotCount,
        animationSpec = tween(durationMillis = 400),
        label = "screenshot_count"
    )
    val animatedProgress by animateFloatAsState(
        targetValue = info.progressFraction,
        animationSpec = tween(durationMillis = 500),
        label = "screenshot_storage_progress"
    )
    val animatedDeviceFree by animateFloatAsState(
        targetValue = info.deviceFreeBytes.toFloat(),
        animationSpec = tween(durationMillis = 450),
        label = "device_free_bytes"
    )
    val animatedDeviceTotal by animateFloatAsState(
        targetValue = info.deviceTotalBytes.toFloat(),
        animationSpec = tween(durationMillis = 450),
        label = "device_total_bytes"
    )

    val usedLabel = remember(animatedBytes) {
        val bytes = animatedBytes.toLong().coerceAtLeast(0L)
        "${Formatter.formatShortFileSize(context, bytes)} used"
    }
    val countLabel = remember(animatedCount) {
        if (animatedCount == 1) {
            "1 screenshot stored"
        } else {
            "$animatedCount screenshots stored"
        }
    }
    val deviceStorageLabel = remember(animatedDeviceTotal, animatedDeviceFree) {
        val total = Formatter.formatShortFileSize(
            context,
            animatedDeviceTotal.toLong().coerceAtLeast(0L)
        )
        val available = Formatter.formatShortFileSize(
            context,
            animatedDeviceFree.toLong().coerceAtLeast(0L)
        )
        "$total total · $available available"
    }

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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Screenshot Storage",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = usedLabel,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = countLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = deviceStorageLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f)
            )
        }
    }
}
