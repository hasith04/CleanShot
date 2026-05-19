package com.cleanshot.app.library

import com.cleanshot.app.models.ScreenshotItem

object LibrarySectionBuilder {

    fun buildGridEntries(
        items: List<ScreenshotItem>,
        sortOrder: LibrarySortOrder
    ): List<LibraryGridEntry> {
        val sorted = LibraryItemSorter.sort(items, sortOrder)
        return when (sortOrder) {
            LibrarySortOrder.NewestFirst,
            LibrarySortOrder.OldestFirst -> {
                LibraryDateGrouping.groupByDate(sorted).toGridEntries()
            }

            LibrarySortOrder.LargestSize -> {
                sorted.mapIndexed { index, item ->
                    LibraryGridEntry.Shot(item, staggerIndex = index)
                }
            }

            LibrarySortOrder.SourceApp -> {
                buildSourceAppEntries(sorted)
            }
        }
    }

    private fun buildSourceAppEntries(items: List<ScreenshotItem>): List<LibraryGridEntry> {
        if (items.isEmpty()) return emptyList()

        val grouped = items.groupBy { it.sourceApp }
        val sectionOrder = grouped.keys.sortedWith(
            compareByDescending<String> { grouped[it]?.size ?: 0 }
                .thenBy { it.lowercase() }
        )

        val entries = mutableListOf<LibraryGridEntry>()
        var stagger = 0
        for (app in sectionOrder) {
            val sectionItems = grouped[app].orEmpty()
            if (sectionItems.isEmpty()) continue
            entries.add(LibraryGridEntry.Header(app))
            for (item in sectionItems) {
                entries.add(LibraryGridEntry.Shot(item, staggerIndex = stagger++))
            }
        }
        return entries
    }
}
