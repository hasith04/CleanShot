package com.cleanshot.app.library

import com.cleanshot.app.models.LibrarySearchQuery
import com.cleanshot.app.models.ScreenshotItem

/**
 * Local text search over screenshot metadata. Replace/extend with OCR and AI pipelines later.
 */
object LibrarySearchFilter {

  fun filterItems(items: List<ScreenshotItem>, query: LibrarySearchQuery): List<ScreenshotItem> {
    if (!query.isActive) return items
    val normalized = query.text.trim().lowercase()
    return items.filter { item ->
      val matchesText =
          normalized.isEmpty() ||
              item.displayName.lowercase().contains(normalized) ||
              item.sourceApp.lowercase().contains(normalized) ||
              item.uri.lastPathSegment.orEmpty().lowercase().contains(normalized)
      val matchesCategory = query.categoryId == null // reserved for smart categories
      matchesText && matchesCategory
    }
  }
}

/** Placeholder hooks for upcoming smart organization features. */
interface LibrarySmartFeatures {
  suspend fun searchOcr(query: String): List<ScreenshotItem> = emptyList()
  suspend fun detectDuplicates(): List<List<ScreenshotItem>> = emptyList()
  suspend fun suggestCategories(): Map<String, List<ScreenshotItem>> = emptyMap()
}
