package com.cleanshot.app.library

import com.cleanshot.app.models.ScreenshotItem

object LibraryItemSorter {

    fun sort(items: List<ScreenshotItem>, order: LibrarySortOrder): List<ScreenshotItem> =
        when (order) {
            LibrarySortOrder.NewestFirst ->
                items.sortedByDescending { effectiveDate(it) }

            LibrarySortOrder.OldestFirst ->
                items.sortedBy { effectiveDate(it) }

            LibrarySortOrder.LargestSize ->
                items.sortedByDescending { it.sizeBytes }

            LibrarySortOrder.SourceApp ->
                items.sortedWith(
                    compareBy<ScreenshotItem> { it.sourceApp.lowercase() }
                        .thenByDescending { effectiveDate(it) }
                )
        }

    private fun effectiveDate(item: ScreenshotItem): Long =
        item.dateTakenMillis.takeIf { it > 0L } ?: Long.MIN_VALUE
}
