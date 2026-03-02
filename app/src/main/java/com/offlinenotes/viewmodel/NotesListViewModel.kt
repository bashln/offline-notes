package com.offlinenotes.viewmodel

import android.app.Application
import android.net.Uri
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
    val defaultQuickKind: NoteKind = NoteKind.ORG_NOTE
)

sealed interface NotesListEvent {
    data class OpenEditor(val noteUri: Uri) : NotesListEvent
    data class ShowMessage(val message: String, val allowReselect: Boolean = false) : NotesListEvent
}

class NotesListViewModel(application: Application) : AndroidViewModel(application) {
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
    }

    fun onQueryChange(value: String) {
        _uiState.update { it.copy(query = value) }
        scheduleFilter()
    }

    fun onFolderSelected(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val flags =
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                getApplication<Application>().contentResolver.takePersistableUriPermission(uri, flags)
                settingsRepository.saveRootUri(uri)
            }.onFailure { error ->
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
        viewModelScope.launch {
            _events.emit(NotesListEvent.OpenEditor(noteMeta.uri))
        }
    }

    fun renameNote(noteMeta: NoteMeta, newName: String) {
        viewModelScope.launch {
            notesRepository.renameNote(noteMeta.uri, newName.trim())
                .onSuccess {
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
        val filtered = if (query.isBlank()) {
            allNotesCache
        } else {
            allNotesCache.filter { it.name.lowercase().contains(query) }
        }
        _uiState.update {
            it.copy(
                notes = filtered,
                isLoading = false
            )
        }
    }

    private fun mapStorageError(error: Throwable, fallback: String): String {
        val message = error.message.orEmpty().lowercase()
        return when {
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
        return message.contains("permission") || error is SecurityException
    }

    private fun clearFolderSelectionWithMessage(message: String) {
        viewModelScope.launch {
            allNotesCache = emptyList()
            settingsRepository.clearRootUri()
            _uiState.update { it.copy(rootUri = null, isLoading = false, notes = emptyList(), query = "") }
            _events.emit(NotesListEvent.ShowMessage(message, allowReselect = true))
        }
    }

    private fun validateExistingFolderAccess(uri: Uri): Boolean {
        return runCatching {
            getApplication<Application>().contentResolver.persistedUriPermissions.any {
                it.uri == uri && (it.isReadPermission || it.isWritePermission)
            }
        }.getOrDefault(false)
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

    private fun observeRootFolder() {
        viewModelScope.launch {
            settingsRepository.rootUriFlow.collectLatest { uri ->
                if (uri != null && !validateExistingFolderAccess(uri)) {
                    onFolderAccessInvalid()
                    return@collectLatest
                }

                _uiState.update { it.copy(rootUri = uri) }
                if (uri != null) {
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
