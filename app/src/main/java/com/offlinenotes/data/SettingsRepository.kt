package com.offlinenotes.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.offlinenotes.ui.theme.ThemeMode
import com.offlinenotes.ui.theme.ThemePalette
import com.offlinenotes.ui.theme.ThemeSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val rootUriKey: Preferences.Key<String> = stringPreferencesKey("root_uri")
    private val defaultNoteFormatKey: Preferences.Key<String> = stringPreferencesKey("default_note_format")
    private val customTagsKey: Preferences.Key<Set<String>> = stringSetPreferencesKey("custom_tags")
    private val noteTagsMapKey: Preferences.Key<Set<String>> = stringSetPreferencesKey("note_tags_map")
    private val themePaletteKey: Preferences.Key<String> = stringPreferencesKey("theme_palette")
    private val themeModeKey: Preferences.Key<String> = stringPreferencesKey("theme_mode")

    val rootUriFlow: Flow<Uri?> = context.dataStore.data.map { prefs ->
        prefs[rootUriKey]?.let(Uri::parse)
    }

    val defaultNoteFormatFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[defaultNoteFormatKey] ?: "org"
    }

    val customTagsFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[customTagsKey] ?: emptySet()
    }

    val noteTagsFlow: Flow<Map<String, String>> = context.dataStore.data.map { prefs ->
        decodeNoteTagsMap(prefs[noteTagsMapKey] ?: emptySet())
    }

    val themePaletteFlow: Flow<ThemePalette> = context.dataStore.data.map { prefs ->
        ThemePalette.fromStorageValue(prefs[themePaletteKey])
    }

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        ThemeMode.fromStorageValue(prefs[themeModeKey])
    }

    val themeSettingsFlow: Flow<ThemeSettings> = combine(themePaletteFlow, themeModeFlow) { palette, mode ->
        ThemeSettings(palette = palette, mode = mode)
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

    suspend fun saveThemePalette(value: ThemePalette) {
        context.dataStore.edit { prefs ->
            prefs[themePaletteKey] = value.storageValue
        }
    }

    suspend fun saveThemeMode(value: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[themeModeKey] = value.storageValue
        }
    }

    suspend fun saveCustomTag(tag: String) {
        val normalized = tag.trim()
        if (normalized.isBlank()) return
        context.dataStore.edit { prefs ->
            val existing = prefs[customTagsKey]?.toMutableSet() ?: mutableSetOf()
            existing.add(normalized)
            prefs[customTagsKey] = existing
        }
    }

    suspend fun setNoteTag(uri: Uri, tag: String?) {
        setNoteTagByUriString(uri.toString(), tag)
    }

    suspend fun setNoteTagByUriString(uriString: String, tag: String?) {
        context.dataStore.edit { prefs ->
            val map = decodeNoteTagsMap(prefs[noteTagsMapKey] ?: emptySet()).toMutableMap()
            val normalized = tag?.trim().orEmpty()
            if (normalized.isBlank()) {
                map.remove(uriString)
            } else {
                map[uriString] = normalized
                val tags = prefs[customTagsKey]?.toMutableSet() ?: mutableSetOf()
                tags.add(normalized)
                prefs[customTagsKey] = tags
            }
            prefs[noteTagsMapKey] = encodeNoteTagsMap(map)
        }
    }

    suspend fun setNoteTagForUris(uriStrings: Set<String>, tag: String?) {
        context.dataStore.edit { prefs ->
            val map = decodeNoteTagsMap(prefs[noteTagsMapKey] ?: emptySet()).toMutableMap()
            val normalized = tag?.trim().orEmpty()
            if (normalized.isBlank()) {
                uriStrings.forEach { map.remove(it) }
            } else {
                uriStrings.forEach { map[it] = normalized }
                val tags = prefs[customTagsKey]?.toMutableSet() ?: mutableSetOf()
                tags.add(normalized)
                prefs[customTagsKey] = tags
            }
            prefs[noteTagsMapKey] = encodeNoteTagsMap(map)
        }
    }

    suspend fun removeNoteTag(uri: Uri) {
        setNoteTag(uri, null)
    }

    suspend fun migrateNoteTag(oldUri: Uri, newUri: Uri) {
        context.dataStore.edit { prefs ->
            val map = decodeNoteTagsMap(prefs[noteTagsMapKey] ?: emptySet()).toMutableMap()
            val existing = map.remove(oldUri.toString())
            if (existing != null) {
                map[newUri.toString()] = existing
                prefs[noteTagsMapKey] = encodeNoteTagsMap(map)
            }
        }
    }

    private fun encodeNoteTagsMap(map: Map<String, String>): Set<String> {
        return map.map { (uri, tag) ->
            val encodedUri = Base64.encodeToString(uri.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            val encodedTag = Base64.encodeToString(tag.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            "$encodedUri|$encodedTag"
        }.toSet()
    }

    private fun decodeNoteTagsMap(values: Set<String>): Map<String, String> {
        if (values.isEmpty()) return emptyMap()
        val map = mutableMapOf<String, String>()
        values.forEach { item ->
            val split = item.indexOf('|')
            if (split <= 0 || split == item.lastIndex) return@forEach
            val encodedUri = item.substring(0, split)
            val encodedTag = item.substring(split + 1)
            val uri = runCatching {
                String(Base64.decode(encodedUri, Base64.NO_WRAP), Charsets.UTF_8)
            }.getOrNull()
            val tag = runCatching {
                String(Base64.decode(encodedTag, Base64.NO_WRAP), Charsets.UTF_8)
            }.getOrNull()
            if (!uri.isNullOrBlank() && !tag.isNullOrBlank()) {
                map[uri] = tag
            }
        }
        return map
    }
}
