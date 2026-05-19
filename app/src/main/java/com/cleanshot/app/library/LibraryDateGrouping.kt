package com.cleanshot.app.library

import com.cleanshot.app.models.LibrarySectionGroup
import com.cleanshot.app.models.ScreenshotItem
import java.util.Calendar

object LibraryDateGrouping {

  fun groupByDate(
      items: List<ScreenshotItem>,
      nowMillis: Long = System.currentTimeMillis()
  ): List<LibrarySectionGroup> {
    if (items.isEmpty()) return emptyList()

    val todayStart = startOfDay(nowMillis)
    val yesterdayStart = todayStart - DAY_MS
    val weekStart = todayStart - 7 * DAY_MS

    val today = mutableListOf<ScreenshotItem>()
    val yesterday = mutableListOf<ScreenshotItem>()
    val thisWeek = mutableListOf<ScreenshotItem>()
    val older = mutableListOf<ScreenshotItem>()

    for (item in items) {
      val timestamp = item.dateTakenMillis.takeIf { it > 0L } ?: continue
      when {
        timestamp >= todayStart -> today.add(item)
        timestamp >= yesterdayStart -> yesterday.add(item)
        timestamp >= weekStart -> thisWeek.add(item)
        else -> older.add(item)
      }
    }

    val undated = items.filter { it.dateTakenMillis <= 0L }
    if (undated.isNotEmpty()) {
      older.addAll(undated)
    }

    return buildList {
      if (today.isNotEmpty()) add(LibrarySectionGroup("Today", today))
      if (yesterday.isNotEmpty()) add(LibrarySectionGroup("Yesterday", yesterday))
      if (thisWeek.isNotEmpty()) add(LibrarySectionGroup("This Week", thisWeek))
      if (older.isNotEmpty()) add(LibrarySectionGroup("Older", older))
    }
  }

  /** Human-readable freshness label for the collection subtitle (e.g. "Updated today"). */
  fun collectionUpdatedSuffix(
      items: List<ScreenshotItem>,
      nowMillis: Long = System.currentTimeMillis()
  ): String? {
    val newest = items.mapNotNull { item ->
      item.dateTakenMillis.takeIf { it > 0L }
    }.maxOrNull() ?: return null

    val todayStart = startOfDay(nowMillis)
    val yesterdayStart = todayStart - DAY_MS
    return when {
      newest >= todayStart -> "Updated today"
      newest >= yesterdayStart -> "Updated yesterday"
      else -> null
    }
  }

  private fun startOfDay(timeMillis: Long): Long {
    val calendar = Calendar.getInstance().apply { timeInMillis = timeMillis }
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
  }

  private const val DAY_MS = 86_400_000L
}
