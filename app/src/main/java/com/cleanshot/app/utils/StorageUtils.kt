package com.cleanshot.app.utils

import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import android.text.format.Formatter
import com.cleanshot.app.models.StorageInfo

fun getStorageInfo(context: Context): StorageInfo {
    val stat = StatFs(Environment.getDataDirectory().path)
    var totalBytes: Long = 0
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        try {
            val statsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
            totalBytes = statsManager.getTotalBytes(StorageManager.UUID_DEFAULT)
        } catch (e: Exception) {
            totalBytes = stat.totalBytes
        }
    } else {
        totalBytes = stat.totalBytes
    }
    val freeBytes = stat.availableBytes
    val usedBytes = totalBytes - freeBytes
    val totalSpaceStr = Formatter.formatShortFileSize(context, totalBytes)
    val usedSpaceStr = Formatter.formatShortFileSize(context, usedBytes)
    val freeSpaceStr = Formatter.formatShortFileSize(context, freeBytes)
    val progress = if (totalBytes > 0) usedBytes.toFloat() / totalBytes.toFloat() else 0f
    return StorageInfo(totalSpaceStr, usedSpaceStr, freeSpaceStr, progress)
}
