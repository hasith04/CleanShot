package com.cleanshot.app.library

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.libraryDisplayDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "library_display_prefs"
)

private val VIEW_MODE_KEY = stringPreferencesKey("view_mode")
private val SORT_ORDER_KEY = stringPreferencesKey("sort_order")

class LibraryDisplayPreferences(private val context: Context) {

    val displaySettingsFlow: Flow<LibraryDisplaySettings> =
        context.libraryDisplayDataStore.data.map { prefs ->
            LibraryDisplaySettings(
                viewMode = LibraryViewMode.fromStored(prefs[VIEW_MODE_KEY]),
                sortOrder = LibrarySortOrder.fromStored(prefs[SORT_ORDER_KEY])
            )
        }

    suspend fun saveViewMode(mode: LibraryViewMode) {
        context.libraryDisplayDataStore.edit { prefs ->
            prefs[VIEW_MODE_KEY] = mode.name
        }
    }

    suspend fun saveSortOrder(order: LibrarySortOrder) {
        context.libraryDisplayDataStore.edit { prefs ->
            prefs[SORT_ORDER_KEY] = order.name
        }
    }
}
