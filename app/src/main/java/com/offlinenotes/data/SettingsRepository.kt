package com.offlinenotes.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.JsonReader
import android.util.JsonWriter
import android.util.Log
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.documentfile.provider.DocumentFile
import com.offlinenotes.domain.FileTypeFilter
import com.offlinenotes.domain.GroupingMode
import com.offlinenotes.ui.theme.ThemeMode
import com.offlinenotes.ui.theme.ThemePalette
import com.offlinenotes.ui.theme.ThemeSettings
import java.io.IOException
import java.io.StringReader
import java.io.StringWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val tag = "SettingsRepository"

    private val rootUriKey: Preferences.Key<String> = stringPreferencesKey("root_uri")
    private val defaultNoteFormatKey: Preferences.Key<String> = stringPreferencesKey("default_note_format")
    private val customTagsKey: Preferences.Key<Set<String>> = stringSetPreferencesKey("custom_tags")
    private val noteTagsMapKey: Preferences.Key<Set<String>> = stringSetPreferencesKey("note_tags_map")
    private val themePaletteKey: Preferences.Key<String> = stringPreferencesKey("theme_palette")
    private val themeModeKey: Preferences.Key<String> = stringPreferencesKey("theme_mode")
    private val groupingModeKey: Preferences.Key<String> = stringPreferencesKey("grouping_mode")
    private val typeFilterKey: Preferences.Key<String> = stringPreferencesKey("type_filter")
    private val collapsedGroupsKey: Preferences.Key<Set<String>> = stringSetPreferencesKey("collapsed_groups")

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

    val groupingModeFlow: Flow<GroupingMode> = context.dataStore.data.map { prefs ->
        GroupingMode.fromStorageValue(prefs[groupingModeKey])
    }

    val typeFilterFlow: Flow<FileTypeFilter> = context.dataStore.data.map { prefs ->
        FileTypeFilter.fromStorageValue(prefs[typeFilterKey])
    }

    val collapsedGroupsFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[collapsedGroupsKey]?.filterTo(mutableSetOf()) { it.isNotBlank() } ?: emptySet()
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

    suspend fun saveGroupingMode(value: GroupingMode) {
        context.dataStore.edit { prefs ->
            prefs[groupingModeKey] = value.storageValue
        }
    }

    suspend fun saveTypeFilter(value: FileTypeFilter) {
        context.dataStore.edit { prefs ->
            prefs[typeFilterKey] = value.storageValue
        }
    }

    suspend fun saveCollapsedGroups(value: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[collapsedGroupsKey] = value.filterTo(mutableSetOf()) { it.isNotBlank() }
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

    suspend fun backupTagsToFile(rootUri: Uri, tagsMap: Map<String, String>) {
        withContext(Dispatchers.IO) {
            runCatching {
                val root = DocumentFile.fromTreeUri(context, rootUri)
                    ?: throw IOException("Pasta raiz invalida")
                val backupDir = ensureBackupDirectory(root)
                val backupFile = ensureBackupFile(backupDir)
                val payload = serializeTagsBackup(tagsMap)
                context.contentResolver.openOutputStream(backupFile.uri, "wt")
                    ?.bufferedWriter()
                    ?.use { writer ->
                        writer.write(payload)
                    }
                    ?: throw IOException("Falha ao abrir arquivo de backup para escrita")
            }.onFailure { error ->
                Log.w(tag, "Falha ao criar backup de tags", error)
            }
        }
    }

    suspend fun restoreTagsFromFile(rootUri: Uri): Boolean = withContext(Dispatchers.IO) {
        val existing = decodeNoteTagsMap(context.dataStore.data.first()[noteTagsMapKey] ?: emptySet())
        if (existing.isNotEmpty()) {
            return@withContext false
        }

        return@withContext runCatching {
            val restored = readTagsBackup(rootUri)
            if (restored.isEmpty()) {
                return@runCatching false
            }
            context.dataStore.edit { prefs ->
                prefs[noteTagsMapKey] = encodeNoteTagsMap(restored)
                val customTags = prefs[customTagsKey]?.toMutableSet() ?: mutableSetOf()
                customTags.addAll(restored.values.filter { it.isNotBlank() })
                prefs[customTagsKey] = customTags
            }
            true
        }.onFailure { error ->
            Log.w(tag, "Falha ao restaurar backup de tags", error)
        }.getOrDefault(false)
    }

    private fun ensureBackupDirectory(root: DocumentFile): DocumentFile {
        val existing = root.findFile(BACKUP_DIR_NAME)
        if (existing != null) {
            if (!existing.isDirectory) {
                throw IOException("Caminho de backup invalido")
            }
            return existing
        }

        return root.createDirectory(BACKUP_DIR_NAME)
            ?: throw IOException("Falha ao criar pasta de backup")
    }

    private fun ensureBackupFile(backupDir: DocumentFile): DocumentFile {
        val existing = backupDir.findFile(BACKUP_FILE_NAME)
        if (existing != null) {
            if (!existing.isFile) {
                throw IOException("Arquivo de backup invalido")
            }
            return existing
        }

        return backupDir.createFile("application/json", BACKUP_FILE_NAME)
            ?: throw IOException("Falha ao criar arquivo de backup")
    }

    private fun readTagsBackup(rootUri: Uri): Map<String, String> {
        val root = DocumentFile.fromTreeUri(context, rootUri)
            ?: throw IOException("Pasta raiz invalida")
        val backupDir = root.findFile(BACKUP_DIR_NAME)
        if (backupDir == null || !backupDir.isDirectory) {
            return emptyMap()
        }
        val backupFile = backupDir.findFile(BACKUP_FILE_NAME)
        if (backupFile == null || !backupFile.isFile) {
            return emptyMap()
        }

        val content = context.contentResolver.openInputStream(backupFile.uri)
            ?.bufferedReader()
            ?.use { it.readText() }
            .orEmpty()
        if (content.isBlank()) {
            return emptyMap()
        }

        return parseTagsBackup(content)
    }

    private fun serializeTagsBackup(tagsMap: Map<String, String>): String {
        val writer = StringWriter()
        JsonWriter(writer).use { jsonWriter ->
            jsonWriter.beginObject()
            jsonWriter.name("version").value(1)
            jsonWriter.name("tags")
            jsonWriter.beginObject()
            tagsMap.entries
                .sortedBy { it.key }
                .forEach { (uri, tagValue) ->
                    jsonWriter.name(uri)
                    jsonWriter.value(tagValue)
                }
            jsonWriter.endObject()
            jsonWriter.endObject()
        }
        return writer.toString()
    }

    private fun parseTagsBackup(content: String): Map<String, String> {
        val restored = mutableMapOf<String, String>()
        JsonReader(StringReader(content)).use { reader ->
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "tags" -> {
                        reader.beginObject()
                        while (reader.hasNext()) {
                            val uri = reader.nextName()
                            val tagValue = reader.nextString().trim()
                            if (uri.isNotBlank() && tagValue.isNotBlank()) {
                                restored[uri] = tagValue
                            }
                        }
                        reader.endObject()
                    }

                    else -> reader.skipValue()
                }
            }
            reader.endObject()
        }
        return restored
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

    companion object {
        private const val BACKUP_DIR_NAME = ".offlinenotes"
        private const val BACKUP_FILE_NAME = "tags_backup.json"
    }
}
