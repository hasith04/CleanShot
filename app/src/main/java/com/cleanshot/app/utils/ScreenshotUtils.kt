package com.cleanshot.app.utils

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.format.Formatter
import androidx.activity.result.IntentSenderRequest
import com.cleanshot.app.models.ScreenshotResults
import java.util.ArrayList
import java.util.Date

fun fetchScreenshotsData(context: Context): ScreenshotResults {
    val screenshotsOnly = mutableListOf<Uri>()
    val allImages = mutableListOf<Uri>()
    val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATE_TAKEN)
    val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"
    val query = context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, sortOrder)
    query?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val name = cursor.getString(nameColumn) ?: ""
            val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            if (name.contains("Screenshot", ignoreCase = true)) screenshotsOnly.add(contentUri)
            allImages.add(contentUri)
        }
    }
    val hasScreenshots = screenshotsOnly.isNotEmpty()
    val finalAllList = if (hasScreenshots) screenshotsOnly else allImages
    return ScreenshotResults(recent = finalAllList.take(10), all = finalAllList, totalCount = if (hasScreenshots) screenshotsOnly.size else 0)
}

fun getScreenshotDetails(context: Context, uri: Uri): Map<String, String> {
    val details = mutableMapOf<String, String>()
    val projection = arrayOf(
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.SIZE,
        MediaStore.Images.Media.DATE_ADDED,
        MediaStore.Images.Media.WIDTH,
        MediaStore.Images.Media.HEIGHT,
        MediaStore.Images.Media.DATA
    )

    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
            val size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE))
            val date = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED))
            val width = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH))
            val height = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT))
            val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))

            details["Name"] = name
            details["Size"] = Formatter.formatShortFileSize(context, size)
            details["Date"] = Date(date * 1000).toString()
            details["Resolution"] = "${width}x${height}"
            details["Path"] = path
        }
    }
    return details
}

fun initiateDeletion(
    context: Context,
    uris: List<Uri>,
    onIntentSenderReady: (IntentSenderRequest) -> Unit
) {
    if (uris.isEmpty()) return
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, uris)
        onIntentSenderReady(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
    } else {
        uris.forEach { context.contentResolver.delete(it, null, null) }
    }
}

fun shareScreenshots(context: Context, uris: List<Uri>) {
    if (uris.isEmpty()) return
    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uris[0])
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        }
    }
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(intent, "Share Screenshots"))
}
