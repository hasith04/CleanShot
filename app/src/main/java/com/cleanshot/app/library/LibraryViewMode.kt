package com.cleanshot.app.library

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class LibraryViewMode(val label: String) {
    Comfortable("Comfortable"),
    Compact("Compact"),
    Large("Large preview");

    companion object {
        fun fromStored(value: String?): LibraryViewMode =
            entries.find { it.name == value } ?: Comfortable
    }
}

enum class LibrarySortOrder(val label: String) {
    NewestFirst("Newest first"),
    OldestFirst("Oldest first"),
    LargestSize("Largest size"),
    SourceApp("Source app");

    companion object {
        fun fromStored(value: String?): LibrarySortOrder =
            entries.find { it.name == value } ?: NewestFirst
    }
}

data class LibraryDisplaySettings(
    val viewMode: LibraryViewMode,
    val sortOrder: LibrarySortOrder
) {
    companion object {
        val DEFAULT = LibraryDisplaySettings(
            viewMode = LibraryViewMode.Comfortable,
            sortOrder = LibrarySortOrder.NewestFirst
        )
    }
}

data class LibraryGridSpec(
    val columns: Int,
    val spacing: Dp,
    val aspectRatio: Float,
    val cornerRadius: Dp,
    val elevation: Dp
)

fun LibraryViewMode.toGridSpec(): LibraryGridSpec = when (this) {
    LibraryViewMode.Comfortable -> LibraryGridSpec(
        columns = 3,
        spacing = 10.dp,
        aspectRatio = 0.62f,
        cornerRadius = 14.dp,
        elevation = 3.dp
    )
    LibraryViewMode.Compact -> LibraryGridSpec(
        columns = 4,
        spacing = 6.dp,
        aspectRatio = 0.58f,
        cornerRadius = 10.dp,
        elevation = 2.dp
    )
    LibraryViewMode.Large -> LibraryGridSpec(
        columns = 2,
        spacing = 14.dp,
        aspectRatio = 0.72f,
        cornerRadius = 18.dp,
        elevation = 4.dp
    )
}
