package com.offlinenotes.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.offlinenotes.data.NoteFileNaming
import com.offlinenotes.data.NotesRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChecklistLine(
    val index: Int,
    val checked: Boolean,
    val text: String
)

data class EditorUiState(
    val uri: Uri,
    val title: String,
    val text: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val checklistLines: List<ChecklistLine> = emptyList()
)

sealed interface EditorEvent {
    data class ShowMessage(val message: String, val allowReselect: Boolean = false) : EditorEvent
}

class EditorViewModel(
    application: Application,
    noteUri: Uri
) : AndroidViewModel(application) {
    private val notesRepository = NotesRepository(application)

    private val _uiState = MutableStateFlow(
        EditorUiState(
            uri = noteUri,
            title = noteUri.lastPathSegment?.substringAfterLast('/') ?: "Nota"
        )
    )
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EditorEvent>()
    val events: SharedFlow<EditorEvent> = _events.asSharedFlow()

    init {
        load()
    }

    fun onTextChanged(value: String) {
        _uiState.update {
            it.copy(
                text = value,
                checklistLines = ChecklistTextTransformer.extractChecklistLines(value)
            )
        }
    }

    fun toggleChecklistLine(lineIndex: Int) {
        onTextChanged(ChecklistTextTransformer.toggleLine(_uiState.value.text, lineIndex))
    }

    fun save(showFeedback: Boolean = true) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            notesRepository.writeNote(_uiState.value.uri, _uiState.value.text)
                .onSuccess {
                    if (showFeedback) {
                        _events.emit(EditorEvent.ShowMessage("Nota salva"))
                    }
                }
                .onFailure {
                    _events.emit(
                        EditorEvent.ShowMessage(
                            it.message ?: "Falha ao salvar",
                            allowReselect = isPermissionMessage(it.message)
                        )
                    )
                }
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    fun saveSilently() {
        save(showFeedback = false)
    }

    fun renameCurrentNote(newName: String) {
        viewModelScope.launch {
            val targetName = runCatching {
                NoteFileNaming.normalizeRename(_uiState.value.title, newName)
            }.getOrElse {
                _events.emit(EditorEvent.ShowMessage(it.message ?: "Nome invalido"))
                return@launch
            }

            notesRepository.renameNote(_uiState.value.uri, targetName)
                .onSuccess {
                    _uiState.update { it.copy(title = targetName) }
                    _events.emit(EditorEvent.ShowMessage("Nome atualizado"))
                }
                .onFailure {
                    _events.emit(
                        EditorEvent.ShowMessage(
                            it.message ?: "Falha ao renomear",
                            allowReselect = isPermissionMessage(it.message)
                        )
                    )
                }
        }
    }

    private fun load() {
        viewModelScope.launch {
            notesRepository.getNoteName(_uiState.value.uri)
                .onSuccess { name ->
                    _uiState.update { it.copy(title = name) }
                }
            notesRepository.readNote(_uiState.value.uri)
                .onSuccess { content ->
                    _uiState.update {
                        it.copy(
                            text = content,
                            isLoading = false,
                            checklistLines = ChecklistTextTransformer.extractChecklistLines(content)
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(
                        EditorEvent.ShowMessage(
                            it.message ?: "Sem permissao para acessar a pasta. Selecione a pasta novamente.",
                            allowReselect = isPermissionMessage(it.message)
                        )
                    )
                }
        }
    }

    private fun isPermissionMessage(message: String?): Boolean {
        return message.orEmpty().lowercase().contains("permissao")
    }

    companion object {
        fun factory(application: Application, noteUri: Uri): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return EditorViewModel(application, noteUri) as T
                }
            }
        }
    }
}
