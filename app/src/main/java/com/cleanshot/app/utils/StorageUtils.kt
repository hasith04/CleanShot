package com.cleanshot.app.utils

import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import com.cleanshot.app.models.ScreenshotItem
import com.cleanshot.app.models.StorageInfo

data class DeviceStorageCapacity(
    val totalBytes: Long,
    val freeBytes: Long
)

fun getDeviceStorageCapacity(context: Context): DeviceStorageCapacity {
    val stat = StatFs(Environment.getDataDirectory().path)
    val totalBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        try {
            val statsManager =
                context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
            statsManager.getTotalBytes(StorageManager.UUID_DEFAULT)
        } catch (_: Exception) {
            stat.totalBytes
        }
    } else {
        stat.totalBytes
    }
    return DeviceStorageCapacity(
        totalBytes = totalBytes,
        freeBytes = stat.availableBytes.coerceAtLeast(0L)
    )
}

fun getScreenshotStorageInfo(
    items: List<ScreenshotItem>,
    deviceCapacity: DeviceStorageCapacity
): StorageInfo {
    val screenshotBytes = items.sumOf { it.sizeBytes.coerceAtLeast(0L) }
    return StorageInfo(
        screenshotBytesUsed = screenshotBytes,
        screenshotCount = items.size,
        deviceTotalBytes = deviceCapacity.totalBytes,
        deviceFreeBytes = deviceCapacity.freeBytes
    )
}

fun getScreenshotStorageInfo(context: Context, items: List<ScreenshotItem>): StorageInfo =
    getScreenshotStorageInfo(items, getDeviceStorageCapacity(context))
