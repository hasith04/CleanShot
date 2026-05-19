package com.cleanshot.app.library

import com.cleanshot.app.models.LibrarySectionGroup
import com.cleanshot.app.models.ScreenshotItem

sealed class LibraryGridEntry(val key: String) {
    data class Header(val title: String) : LibraryGridEntry("header:$title")
    data class Shot(val item: ScreenshotItem, val staggerIndex: Int) :
        LibraryGridEntry("shot:${item.uri}")
}

fun List<LibrarySectionGroup>.toGridEntries(): List<LibraryGridEntry> {
    if (isEmpty()) return emptyList()
    var stagger = 0
    return flatMap { section ->
        buildList {
            add(LibraryGridEntry.Header(section.title))
            section.items.forEach { item ->
                add(LibraryGridEntry.Shot(item, staggerIndex = stagger))
                stagger++
            }
        }
    }
}
