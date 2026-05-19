package com.cleanshot.app.screens

import android.net.Uri

enum class ScreenshotReviewState {
    Unreviewed,
    Kept,
    Queued
}

/**
 * Stable review record — deck membership is derived from [ScreenshotReviewState], not list index.
 */
data class CleanupReviewItem(
    val id: String,
    val uri: Uri,
    val reviewState: ScreenshotReviewState
)

data class CleanupUndoEntry(
    val itemId: String,
    val previousState: ScreenshotReviewState
)

fun List<Uri>.toCleanupReviewItems(): List<CleanupReviewItem> = map { uri ->
    CleanupReviewItem(
        id = uri.toString(),
        uri = uri,
        reviewState = ScreenshotReviewState.Unreviewed
    )
}

fun List<CleanupReviewItem>.unreviewedDeck(): List<CleanupReviewItem> =
    filter { it.reviewState == ScreenshotReviewState.Unreviewed }

fun List<CleanupReviewItem>.queuedUris(): List<Uri> =
    filter { it.reviewState == ScreenshotReviewState.Queued }.map { it.uri }

fun List<CleanupReviewItem>.keepCount(): Int =
    count { it.reviewState == ScreenshotReviewState.Kept }

fun List<CleanupReviewItem>.queuedCount(): Int =
    count { it.reviewState == ScreenshotReviewState.Queued }

fun List<CleanupReviewItem>.reviewedCount(): Int =
    count { it.reviewState != ScreenshotReviewState.Unreviewed }
