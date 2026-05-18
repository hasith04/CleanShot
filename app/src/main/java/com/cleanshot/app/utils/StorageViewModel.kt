package com.cleanshot.app.utils

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StorageViewModel(application: Application) : AndroidViewModel(application) {
    private val storagePreferences = StoragePreferences(application)
    private val contentResolver = application.contentResolver

    val storageSettings: StateFlow<StorageSettings> = storagePreferences.storageSettingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StorageSettings(null, emptySet())
        )

    fun setMainFolder(uri: Uri) {
        viewModelScope.launch {
            // Take persistable permission
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            storagePreferences.saveMainFolder(uri)
        }
    }

    fun addMonitoredFolder(uri: Uri) {
        viewModelScope.launch {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            storagePreferences.addMonitoredFolder(uri)
        }
    }

    fun removeMonitoredFolder(uriString: String) {
        viewModelScope.launch {
            storagePreferences.removeMonitoredFolder(uriString)
            // Note: We don't necessarily need to release permissions here as other apps might use it
            // but SAF will manage it.
        }
    }
}
