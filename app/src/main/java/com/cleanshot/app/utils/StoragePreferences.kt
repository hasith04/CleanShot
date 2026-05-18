package com.cleanshot.app.utils

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.storageDataStore: DataStore<Preferences> by preferencesDataStore(name = "storage_prefs")

data class StorageSettings(
    val mainScreenshotFolder: String?,
    val monitoredFolders: Set<String>
)

class StoragePreferences(private val context: Context) {
    companion object {
        private val MAIN_FOLDER_KEY = stringPreferencesKey("main_screenshot_folder")
        private val MONITORED_FOLDERS_KEY = stringSetPreferencesKey("monitored_folders")
    }

    val storageSettingsFlow: Flow<StorageSettings> = context.storageDataStore.data.map { prefs ->
        StorageSettings(
            mainScreenshotFolder = prefs[MAIN_FOLDER_KEY],
            monitoredFolders = prefs[MONITORED_FOLDERS_KEY] ?: emptySet()
        )
    }

    suspend fun saveMainFolder(uri: Uri) {
        context.storageDataStore.edit { prefs ->
            prefs[MAIN_FOLDER_KEY] = uri.toString()
            // Also add to monitored folders if not present
            val current = prefs[MONITORED_FOLDERS_KEY] ?: emptySet()
            prefs[MONITORED_FOLDERS_KEY] = current + uri.toString()
        }
    }

    suspend fun addMonitoredFolder(uri: Uri) {
        context.storageDataStore.edit { prefs ->
            val current = prefs[MONITORED_FOLDERS_KEY] ?: emptySet()
            prefs[MONITORED_FOLDERS_KEY] = current + uri.toString()
        }
    }

    suspend fun removeMonitoredFolder(uriString: String) {
        context.storageDataStore.edit { prefs ->
            val current = prefs[MONITORED_FOLDERS_KEY] ?: emptySet()
            prefs[MONITORED_FOLDERS_KEY] = current - uriString
            if (prefs[MAIN_FOLDER_KEY] == uriString) {
                prefs.remove(MAIN_FOLDER_KEY)
            }
        }
    }
}
