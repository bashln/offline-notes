package com.offlinenotes.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.offlinenotes.data.NotesRepository
import com.offlinenotes.data.SettingsRepository
import com.offlinenotes.domain.NoteKind
import com.offlinenotes.domain.NoteMeta
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotesListUiState(
    val rootUri: Uri? = null,
    val isLoading: Boolean = true,
    val query: String = "",
    val notes: List<NoteMeta> = emptyList(),
    val groupedNotes: List<NoteGroupUi> = emptyList(),
    val defaultQuickKind: NoteKind = NoteKind.ORG_NOTE,
    val availableTags: Set<String> = emptySet(),
    val noteTagsByUri: Map<String, String> = emptyMap(),
    val collapsedGroups: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val selectedUris: Set<String> = emptySet()
)

data class NoteGroupUi(
    val key: String,
    val title: String,
    val notes: List<NoteMeta>,
    val isExpanded: Boolean
)

sealed interface NotesListEvent {
    data class OpenEditor(val noteUri: Uri) : NotesListEvent
    data class ShowMessage(val message: String, val allowReselect: Boolean = false) : NotesListEvent
}

class NotesListViewModel(application: Application) : AndroidViewModel(application) {
    private val tag = "NotesListViewModel"
    private val settingsRepository = SettingsRepository(application)
    private val notesRepository = NotesRepository(application)
    private var allNotesCache: List<NoteMeta> = emptyList()
    private var searchJob: Job? = null

    private val _uiState = MutableStateFlow(NotesListUiState())
    val uiState: StateFlow<NotesListUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<NotesListEvent>()
    val events: SharedFlow<NotesListEvent> = _events.asSharedFlow()

    init {
        observeRootFolder()
        observeDefaultFormat()
        observeTags()
    }

    fun onQueryChange(value: String) {
        _uiState.update { it.copy(query = value) }
        scheduleFilter()
    }

    fun onFolderSelected(uri: Uri, returnedFlags: Int) {
        viewModelScope.launch {
            try {
                val rwMask = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                Log.d(tag, "Folder picker result uri=$uri flags=$returnedFlags")
                getApplication<Application>().contentResolver.takePersistableUriPermission(uri, rwMask)
                if (!hasPersistedReadWritePermission(uri)) {
                    throw SecurityException("Sem permissao de escrita")
                }
                notesRepository.checkWritableRoot(uri).getOrThrow()
                settingsRepository.saveRootUri(uri)
                allNotesCache = emptyList()
                _uiState.update { it.copy(rootUri = uri) }
                refreshNotes(forceReload = true)
            } catch (error: Throwable) {
                Log.w(tag, "Folder selection failed", error)
                resetFolderSelection()
                _events.emit(
                    NotesListEvent.ShowMessage(
                        mapStorageError(error, "Nao foi possivel salvar a pasta"),
                        allowReselect = isPermissionError(error)
                    )
                )
            }
        }
    }

    fun createQuickNote(kind: NoteKind? = null) {
        val rootUri = _uiState.value.rootUri ?: return
        val targetKind = kind ?: _uiState.value.defaultQuickKind
        viewModelScope.launch {
            notesRepository.createQuickNote(rootUri, targetKind)
                .onSuccess { createdUri ->
                    Log.d(tag, "createQuickNote success: openEditor uri=$createdUri")
                    if (targetKind == NoteKind.MARKDOWN_TASKS) {
                        val content = "- [ ] Item 1\n- [ ] Item 2\n"
                        notesRepository.writeNote(createdUri, content)
                    }
                    refreshNotes(forceReload = true)
                    _events.emit(NotesListEvent.OpenEditor(createdUri))
                }
                .onFailure {
                    _events.emit(
                        NotesListEvent.ShowMessage(
                            mapStorageError(it, "Falha ao criar nota"),
                            allowReselect = isPermissionError(it)
                        )
                    )
                }
        }
    }

    fun toggleDefaultQuickFormat() {
        viewModelScope.launch {
            val nextKind = if (_uiState.value.defaultQuickKind == NoteKind.ORG_NOTE) {
                NoteKind.MARKDOWN_NOTE
            } else {
                NoteKind.ORG_NOTE
            }
            val value = if (nextKind == NoteKind.ORG_NOTE) "org" else "md"
            settingsRepository.saveDefaultNoteFormat(value)
        }
    }

    fun openNote(noteMeta: NoteMeta) {
        if (_uiState.value.isSelectionMode) {
            toggleSelection(noteMeta)
            return
        }
        viewModelScope.launch {
            _events.emit(NotesListEvent.OpenEditor(noteMeta.uri))
        }
    }

    fun onNoteLongPress(noteMeta: NoteMeta) {
        if (!_uiState.value.isSelectionMode) {
            _uiState.update {
                it.copy(
                    isSelectionMode = true,
                    selectedUris = setOf(noteMeta.uri.toString())
                )
            }
            return
        }
        toggleSelection(noteMeta)
    }

    fun onNoteTap(noteMeta: NoteMeta) {
        if (_uiState.value.isSelectionMode) {
            toggleSelection(noteMeta)
        } else {
            openNote(noteMeta)
        }
    }

    fun clearSelectionMode() {
        _uiState.update { it.copy(isSelectionMode = false, selectedUris = emptySet()) }
    }

    fun toggleSelectAllVisible() {
        val visibleUris = _uiState.value.groupedNotes.flatMap { group -> group.notes }.map { it.uri.toString() }.toSet()
        if (visibleUris.isEmpty()) return
        _uiState.update { state ->
            val allSelected = visibleUris.all { it in state.selectedUris }
            val next = if (allSelected) {
                state.selectedUris - visibleUris
            } else {
                state.selectedUris + visibleUris
            }
            state.copy(
                selectedUris = next,
                isSelectionMode = next.isNotEmpty()
            )
        }
    }

    fun deleteSelectedNotes() {
        val selected = _uiState.value.selectedUris
        if (selected.isEmpty()) return
        viewModelScope.launch {
            val selectedNotes = allNotesCache.filter { it.uri.toString() in selected }
            selectedNotes.forEach { note ->
                notesRepository.deleteNote(note.uri)
                    .onSuccess { settingsRepository.removeNoteTag(note.uri) }
            }
            clearSelectionMode()
            refreshNotes(forceReload = true)
        }
    }

    fun setTagForSelected(tag: String?) {
        val selected = _uiState.value.selectedUris
        if (selected.isEmpty()) return
        viewModelScope.launch {
            settingsRepository.setNoteTagForUris(selected, tag)
            clearSelectionMode()
            applyFilterNow()
        }
    }

    fun setTagForNote(noteMeta: NoteMeta, tag: String?) {
        viewModelScope.launch {
            settingsRepository.setNoteTag(noteMeta.uri, tag)
            applyFilterNow()
        }
    }

    fun saveCustomTag(tag: String) {
        viewModelScope.launch {
            settingsRepository.saveCustomTag(tag)
        }
    }

    fun toggleGroupExpansion(groupKey: String) {
        _uiState.update { state ->
            val next = state.collapsedGroups.toMutableSet()
            if (groupKey in next) {
                next.remove(groupKey)
            } else {
                next.add(groupKey)
            }
            state.copy(collapsedGroups = next)
        }
        applyFilterNow()
    }

    fun renameNote(noteMeta: NoteMeta, newName: String) {
        viewModelScope.launch {
            notesRepository.renameNote(noteMeta.uri, newName.trim())
                .onSuccess { newUri ->
                    settingsRepository.migrateNoteTag(noteMeta.uri, newUri)
                    refreshNotes(forceReload = true)
                }
                .onFailure {
                    _events.emit(
                        NotesListEvent.ShowMessage(
                            mapStorageError(it, "Falha ao renomear"),
                            allowReselect = isPermissionError(it)
                        )
                    )
                }
        }
    }

    fun deleteNote(noteMeta: NoteMeta) {
        viewModelScope.launch {
            notesRepository.deleteNote(noteMeta.uri)
                .onSuccess {
                    settingsRepository.removeNoteTag(noteMeta.uri)
                    refreshNotes(forceReload = true)
                }
                .onFailure {
                    _events.emit(
                        NotesListEvent.ShowMessage(
                            mapStorageError(it, "Falha ao deletar"),
                            allowReselect = isPermissionError(it)
                        )
                    )
                }
        }
    }

    fun refreshNotes(forceReload: Boolean = false) {
        val rootUri = _uiState.value.rootUri ?: return
        viewModelScope.launch {
            if (forceReload || allNotesCache.isEmpty()) {
                _uiState.update { it.copy(isLoading = true) }
                runCatching {
                    notesRepository.listNotes(rootUri)
                }.onSuccess { list ->
                    allNotesCache = list
                    applyFilterNow()
                }.onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, notes = emptyList()) }
                    if (isPermissionError(error)) {
                        clearFolderSelectionWithMessage("Permissao da pasta expirada. Escolha novamente.")
                    } else {
                        _events.emit(
                            NotesListEvent.ShowMessage(
                                mapStorageError(error, "Erro ao acessar pasta")
                            )
                        )
                    }
                }
            } else {
                applyFilterNow()
            }
        }
    }

    private fun scheduleFilter() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(250)
            applyFilterNow()
        }
    }

    private fun applyFilterNow() {
        val query = _uiState.value.query.trim().lowercase()
        val noteTags = _uiState.value.noteTagsByUri
        val filtered = if (query.isBlank()) {
            allNotesCache
        } else {
            allNotesCache.filter { it.name.lowercase().contains(query) }
        }

        val grouped = buildGroups(
            notes = filtered,
            noteTagsByUri = noteTags,
            collapsedGroups = _uiState.value.collapsedGroups
        )

        _uiState.update {
            it.copy(
                notes = filtered,
                groupedNotes = grouped,
                isLoading = false
            )
        }
    }

    private fun buildGroups(
        notes: List<NoteMeta>,
        noteTagsByUri: Map<String, String>,
        collapsedGroups: Set<String>
    ): List<NoteGroupUi> {
        val groupedByTag = notes.groupBy { noteTagsByUri[it.uri.toString()]?.takeIf { tag -> tag.isNotBlank() } }
        val withTag = groupedByTag.filterKeys { it != null }
            .toList()
            .sortedBy { it.first!!.lowercase() }
            .map { (tag, groupNotes) ->
                val key = "tag:${tag!!}"
                NoteGroupUi(
                    key = key,
                    title = tag,
                    notes = groupNotes.sortedWith(compareByDescending<NoteMeta> { it.lastModified ?: Long.MIN_VALUE }.thenBy { it.name.lowercase() }),
                    isExpanded = key !in collapsedGroups
                )
            }

        val untagged = groupedByTag[null].orEmpty()
        val groups = withTag.toMutableList()
        if (untagged.isNotEmpty()) {
            val key = "untagged"
            groups.add(
                NoteGroupUi(
                    key = key,
                    title = "Sem tag",
                    notes = untagged.sortedWith(compareByDescending<NoteMeta> { it.lastModified ?: Long.MIN_VALUE }.thenBy { it.name.lowercase() }),
                    isExpanded = key !in collapsedGroups
                )
            )
        }

        return groups
    }

    private fun toggleSelection(noteMeta: NoteMeta) {
        val key = noteMeta.uri.toString()
        _uiState.update { state ->
            val next = state.selectedUris.toMutableSet()
            if (!next.add(key)) {
                next.remove(key)
            }
            state.copy(
                selectedUris = next,
                isSelectionMode = next.isNotEmpty()
            )
        }
    }

    private fun mapStorageError(error: Throwable, fallback: String): String {
        val message = error.message.orEmpty().lowercase()
        return when {
            message.contains("escrita") || message.contains("write") -> {
                "Sem permissao de escrita nesta pasta. Selecione outra pasta."
            }

            isPermissionError(error) -> {
                "Sem permissao para acessar esta pasta. Escolha novamente."
            }

            message.contains("no such file") || message.contains("not found") -> {
                "Pasta ou arquivo nao encontrado."
            }

            else -> error.message ?: fallback
        }
    }

    private fun isPermissionError(error: Throwable): Boolean {
        val message = error.message.orEmpty().lowercase()
        return message.contains("permission") || message.contains("permissao") || error is SecurityException
    }

    private fun clearFolderSelectionWithMessage(message: String) {
        viewModelScope.launch {
            resetFolderSelection()
            _events.emit(NotesListEvent.ShowMessage(message, allowReselect = true))
        }
    }

    private suspend fun resetFolderSelection() {
        allNotesCache = emptyList()
        settingsRepository.clearRootUri()
        _uiState.update { it.copy(rootUri = null, isLoading = false, notes = emptyList(), query = "") }
    }

    private fun validateExistingFolderAccess(uri: Uri): Boolean {
        return runCatching {
            hasPersistedReadWritePermission(uri)
        }.getOrDefault(false)
    }

    private fun hasPersistedReadWritePermission(uri: Uri): Boolean {
        return getApplication<Application>().contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isReadPermission && it.isWritePermission
        }
    }

    private fun onFolderAccessInvalid() {
        clearFolderSelectionWithMessage("Permissao da pasta expirada. Escolha novamente.")
    }

    private fun observeDefaultFormat() {
        viewModelScope.launch {
            settingsRepository.defaultNoteFormatFlow.collectLatest { value ->
                val parsed = if (value == "md") NoteKind.MARKDOWN_NOTE else NoteKind.ORG_NOTE
                _uiState.update { it.copy(defaultQuickKind = parsed) }
            }
        }
    }

    private fun observeTags() {
        viewModelScope.launch {
            settingsRepository.customTagsFlow.collectLatest { tags ->
                _uiState.update { it.copy(availableTags = tags) }
            }
        }

        viewModelScope.launch {
            settingsRepository.noteTagsFlow.collectLatest { map ->
                _uiState.update { it.copy(noteTagsByUri = map) }
                applyFilterNow()
            }
        }
    }

    private fun observeRootFolder() {
        viewModelScope.launch {
            settingsRepository.rootUriFlow.collectLatest { uri ->
                if (uri != null && !validateExistingFolderAccess(uri)) {
                    onFolderAccessInvalid()
                    return@collectLatest
                }

                _uiState.update { it.copy(rootUri = uri) }
                if (uri != null) {
                    val writable = notesRepository.checkWritableRoot(uri).isSuccess
                    if (!writable) {
                        clearFolderSelectionWithMessage("Sem permissao de escrita nesta pasta. Selecione outra pasta.")
                        return@collectLatest
                    }
                    refreshNotes(forceReload = true)
                } else {
                    allNotesCache = emptyList()
                    _uiState.update { it.copy(isLoading = false, notes = emptyList()) }
                }
            }
        }
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return NotesListViewModel(application) as T
                }
            }
        }
    }
}
