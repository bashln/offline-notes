package com.offlinenotes.data

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val rootUriKey: Preferences.Key<String> = stringPreferencesKey("root_uri")
    private val defaultNoteFormatKey: Preferences.Key<String> = stringPreferencesKey("default_note_format")

    val rootUriFlow: Flow<Uri?> = context.dataStore.data.map { prefs ->
        prefs[rootUriKey]?.let(Uri::parse)
    }

    val defaultNoteFormatFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[defaultNoteFormatKey] ?: "org"
    }

    suspend fun saveRootUri(uri: Uri) {
        context.dataStore.edit { prefs ->
            prefs[rootUriKey] = uri.toString()
        }
    }

    suspend fun clearRootUri() {
        context.dataStore.edit { prefs ->
            prefs.remove(rootUriKey)
        }
    }

    suspend fun saveDefaultNoteFormat(value: String) {
        context.dataStore.edit { prefs ->
            prefs[defaultNoteFormatKey] = value
        }
    }
}
