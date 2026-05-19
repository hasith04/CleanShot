package com.cleanshot.app.utils

import android.content.Context
import android.net.Uri
import com.cleanshot.app.home.LibraryScanOutcome
import com.cleanshot.app.models.ScreenshotResults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun refreshScreenshotLibrary(
    context: Context,
    monitoredFolders: Set<String>,
    previousUris: Set<Uri> = emptySet()
): Pair<ScreenshotResults, LibraryScanOutcome> = withContext(Dispatchers.IO) {
    val results = fetchScreenshotsData(context, monitoredFolders)
    val currentUris = results.all.toSet()
    val newCount = if (previousUris.isEmpty()) {
        0
    } else {
        (currentUris - previousUris).size
    }
    results to LibraryScanOutcome(
        newScreenshotCount = newCount,
        totalCount = results.totalCount
    )
}
