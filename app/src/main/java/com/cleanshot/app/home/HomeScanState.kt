package com.cleanshot.app.home

sealed interface ScanButtonUiState {
    data object Idle : ScanButtonUiState
    data object Scanning : ScanButtonUiState
    data class Result(val message: String) : ScanButtonUiState
}

data class LibraryScanOutcome(
    val newScreenshotCount: Int,
    val totalCount: Int
)

fun formatLastScannedLabel(lastScanEpochMs: Long): String? {
    if (lastScanEpochMs <= 0L) return null
    val elapsedMs = (System.currentTimeMillis() - lastScanEpochMs).coerceAtLeast(0L)
    return when {
        elapsedMs < 60_000L -> "Last scanned just now"
        elapsedMs < 3_600_000L -> "Last scanned ${elapsedMs / 60_000L}m ago"
        elapsedMs < 86_400_000L -> "Last scanned ${elapsedMs / 3_600_000L}h ago"
        else -> "Last scanned ${elapsedMs / 86_400_000L}d ago"
    }
}

fun resultMessageForScan(outcome: LibraryScanOutcome): String =
    when {
        outcome.newScreenshotCount > 0 -> {
            val count = outcome.newScreenshotCount
            if (count == 1) "1 new screenshot found" else "$count new screenshots found"
        }
        else -> "No new screenshots found"
    }
