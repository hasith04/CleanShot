package com.cleanshot.app.utils

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.format.Formatter
import androidx.activity.result.IntentSenderRequest
import com.cleanshot.app.models.ScreenshotItem
import com.cleanshot.app.models.ScreenshotResults
import java.util.ArrayList
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun fetchScreenshotsData(context: Context, monitoredFolders: Set<String> = emptySet()): ScreenshotResults {
    val items = mutableListOf<ScreenshotItem>()
    
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.RELATIVE_PATH,
        MediaStore.Images.Media.DATE_TAKEN,
        MediaStore.Images.Media.SIZE
    )
    
    val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"
    
    // Build selection based on monitored folders
    var selection: String? = null
    var selectionArgs: Array<String>? = null
    
    if (monitoredFolders.isNotEmpty()) {
        val paths = monitoredFolders.mapNotNull { uriString ->
            extractRelativePath(uriString)
        }
        
        if (paths.isNotEmpty()) {
            selection = paths.joinToString(" OR ") { "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?" }
            selectionArgs = paths.map { "$it%" }.toTypedArray()
        }
    }

    // If no specific folders monitored, fallback to searching for "Screenshot" in name
    if (selection == null) {
        selection = "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
        selectionArgs = arrayOf("%Screenshot%")
    }

    context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        sortOrder
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
        val pathColumn = cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
        val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
        val sizeColumn = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            val name = cursor.getString(nameColumn).orEmpty()
            val relativePath = if (pathColumn >= 0) {
                cursor.getString(pathColumn).orEmpty()
            } else {
                ""
            }
            val dateTaken = cursor.getLong(dateColumn)
            val sizeBytes = if (sizeColumn >= 0) {
                cursor.getLong(sizeColumn).coerceAtLeast(0L)
            } else {
                0L
            }
            items.add(
                ScreenshotItem(
                    uri = contentUri,
                    dateTakenMillis = dateTaken,
                    displayName = name,
                    sizeBytes = sizeBytes,
                    relativePath = relativePath,
                    sourceApp = ScreenshotSourceDetector.detect(relativePath, name)
                )
            )
        }
    }

    val uris = items.map { it.uri }
    return ScreenshotResults(
        recent = uris.take(10),
        all = uris,
        items = items,
        totalCount = items.size
    )
}

/**
 * Extracts relative path from a SAF Tree URI if possible.
 * Example: content://com.android.externalstorage.documents/tree/primary%3APictures%2FScreenshots
 * becomes Pictures/Screenshots/
 */
private fun extractRelativePath(uriString: String): String? {
    return try {
        val decodedUri = Uri.decode(uriString)
        val treeId = decodedUri.substringAfter("/tree/", "")
        if (treeId.startsWith("primary:")) {
            val path = treeId.substringAfter("primary:")
            if (path.isEmpty()) null else "$path/"
        } else if (treeId.contains(":")) {
            // Handle SD cards etc if needed, but primary is most common
            val path = treeId.substringAfter(":")
            if (path.isEmpty()) null else "$path/"
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

/** Sums [MediaStore.Images.Media.SIZE] for the given content URIs (IO-bound). */
suspend fun estimateUrisTotalBytes(context: Context, uris: List<Uri>): Long =
    withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext 0L
        val projection = arrayOf(MediaStore.Images.Media.SIZE)
        var total = 0L
        val resolver = context.contentResolver
        for (uri in uris) {
            resolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
                    if (sizeIndex >= 0) {
                        total += cursor.getLong(sizeIndex).coerceAtLeast(0L)
                    }
                }
            }
        }
        total
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
            val name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)) ?: "Unknown"
            val size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE))
            val date = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED))
            val width = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH))
            val height = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT))
            val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)) ?: "Unknown"

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
