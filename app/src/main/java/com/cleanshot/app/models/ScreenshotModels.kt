package com.cleanshot.app.models



import android.net.Uri



data class ScreenshotResults(

    val recent: List<Uri>,

    val all: List<Uri>,

    val items: List<ScreenshotItem>,

    val totalCount: Int

)



/** Screenshot storage footprint relative to total device capacity. */

data class StorageInfo(
    val screenshotBytesUsed: Long,
    val screenshotCount: Int,
    val deviceTotalBytes: Long,
    val deviceFreeBytes: Long
) {

    val progressFraction: Float

        get() = if (deviceTotalBytes > 0) {

            (screenshotBytesUsed.toFloat() / deviceTotalBytes.toFloat()).coerceIn(0f, 1f)

        } else {

            0f

        }

}


