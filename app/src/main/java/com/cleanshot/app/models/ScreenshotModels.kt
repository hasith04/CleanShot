package com.cleanshot.app.models

import android.net.Uri

data class ScreenshotResults(
    val recent: List<Uri>,
    val all: List<Uri>,
    val totalCount: Int
)

data class StorageInfo(
    val totalSpace: String,
    val usedSpace: String,
    val freeSpace: String,
    val progress: Float
)
