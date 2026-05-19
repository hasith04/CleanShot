package com.cleanshot.app.models

import android.net.Uri

/** Screenshot with metadata for library grouping, search, and future AI features. */
data class ScreenshotItem(
    val uri: Uri,
    val dateTakenMillis: Long,
    val displayName: String,
    val sizeBytes: Long = 0L,
    val relativePath: String = "",
    val sourceApp: String = "Other"
)

data class LibrarySectionGroup(
    val title: String,
    val items: List<ScreenshotItem>
)

/**
 * Filter state for library search. Extend for OCR, categories, and duplicate detection.
 */
data class LibrarySearchQuery(
    val text: String = "",
    val categoryId: String? = null
) {
    val isActive: Boolean get() = text.isNotBlank() || categoryId != null
}
