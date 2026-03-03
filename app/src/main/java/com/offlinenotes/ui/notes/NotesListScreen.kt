package com.offlinenotes.ui.notes

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.offlinenotes.domain.NoteMeta
import com.offlinenotes.viewmodel.NoteGroupUi
import com.offlinenotes.viewmodel.NotesListEvent
import com.offlinenotes.viewmodel.NotesListViewModel

private sealed interface TagTarget {
    data class Single(val note: NoteMeta) : TagTarget
    data object Bulk : TagTarget
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    paddingValues: PaddingValues,
    viewModel: NotesListViewModel,
    onFolderSelected: (Uri, Int) -> Unit,
    onOpenSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var renameTarget by remember { mutableStateOf<NoteMeta?>(null) }
    var deleteTarget by remember { mutableStateOf<NoteMeta?>(null) }
    var tagTarget by remember { mutableStateOf<TagTarget?>(null) }
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }

    val folderLauncher = rememberLauncherForActivityResult(
        contract = StartActivityForResult()
    ) { result ->
        val data = result.data
        val uri: Uri? = data?.data
        if (uri != null) {
            onFolderSelected(uri, data.flags)
        }
    }

    fun launchFolderPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            )
        }
        folderLauncher.launch(intent)
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event is NotesListEvent.ShowMessage) {
                val result = snackbarHostState.showSnackbar(
                    message = event.message,
                    actionLabel = if (event.allowReselect) "Selecionar pasta" else null
                )
                if (event.allowReselect && result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                    launchFolderPicker()
                }
            }
        }
    }

    Scaffold(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
        topBar = {
            if (uiState.isSelectionMode) {
                TopAppBar(
                    title = { Text("${uiState.selectedUris.size} selecionada(s)") },
                    navigationIcon = {
                        IconButton(onClick = viewModel::clearSelectionMode) {
                            Icon(Icons.Default.Close, contentDescription = "Cancelar selecao")
                        }
                    },
                    actions = {
                        IconButton(onClick = viewModel::toggleSelectAllVisible) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Selecionar tudo")
                        }
                        IconButton(onClick = { tagTarget = TagTarget.Bulk }) {
                            Icon(Icons.Default.Label, contentDescription = "Definir tag")
                        }
                        IconButton(onClick = { showBulkDeleteConfirm = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Deletar selecionadas",
                                tint = androidx.compose.material3.MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
                        titleContentColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
                    )
                )
            } else {
                TopAppBar(
                    title = { Text(text = "OfflineNotes") },
                    actions = {
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Configuracoes",
                                tint = androidx.compose.material3.MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
                        titleContentColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { scaffoldPadding ->
        if (uiState.rootUri == null) {
            EmptyFolderState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = paddingValues.calculateBottomPadding())
                    .padding(scaffoldPadding),
                onPickFolder = { launchFolderPicker() }
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
                .padding(scaffoldPadding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("Buscar por nome") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                    disabledContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                    focusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    cursorColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
                )
            )

            Spacer(Modifier.height(8.dp))

            val rootLabel = uiState.rootUri?.lastPathSegment?.substringAfterLast(':').orEmpty()
            Text(
                text = "Pasta ativa: ${if (rootLabel.isBlank()) "(desconhecida)" else rootLabel}",
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(16.dp))

            when {
                uiState.isLoading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.groupedNotes.isEmpty() -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
                        ),
                        shape = androidx.compose.material3.MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = "Nenhuma nota encontrada.",
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.groupedNotes.forEach { group ->
                            item(key = "group-${group.key}") {
                                GroupHeader(
                                    group = group,
                                    onToggle = { viewModel.toggleGroupExpansion(group.key) }
                                )
                            }
                            if (group.isExpanded) {
                                items(group.notes, key = { it.uri.toString() }) { note ->
                                    val noteKey = note.uri.toString()
                                    val selected = noteKey in uiState.selectedUris
                                    NoteCard(
                                        note = note,
                                        tag = uiState.noteTagsByUri[noteKey],
                                        isSelectionMode = uiState.isSelectionMode,
                                        isSelected = selected,
                                        onTap = { viewModel.onNoteTap(note) },
                                        onLongPress = { viewModel.onNoteLongPress(note) },
                                        onRename = { renameTarget = note },
                                        onDelete = { deleteTarget = note },
                                        onSetTag = { tagTarget = TagTarget.Single(note) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (renameTarget != null) {
        RenameDialog(
            note = renameTarget!!,
            onDismiss = { renameTarget = null },
            onConfirm = { newName ->
                viewModel.renameNote(renameTarget!!, newName)
                renameTarget = null
            }
        )
    }

    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Deletar nota") },
            text = { Text("Tem certeza que deseja deletar ${deleteTarget!!.name}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteNote(deleteTarget!!)
                        deleteTarget = null
                    }
                ) {
                    Text("Deletar")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showBulkDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirm = false },
            title = { Text("Deletar notas selecionadas") },
            text = { Text("Tem certeza que deseja deletar ${uiState.selectedUris.size} nota(s)?") },
            confirmButton = {
                TextButton(onClick = {
                    showBulkDeleteConfirm = false
                    viewModel.deleteSelectedNotes()
                }) {
                    Text("Deletar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (tagTarget != null) {
        TagDialog(
            tags = uiState.availableTags,
            currentTag = when (val target = tagTarget) {
                is TagTarget.Single -> uiState.noteTagsByUri[target.note.uri.toString()]
                else -> null
            },
            onDismiss = { tagTarget = null },
            onApply = { tag ->
                when (val target = tagTarget) {
                    is TagTarget.Single -> viewModel.setTagForNote(target.note, tag)
                    is TagTarget.Bulk -> viewModel.setTagForSelected(tag)
                    null -> Unit
                }
                if (!tag.isNullOrBlank()) {
                    viewModel.saveCustomTag(tag)
                }
                tagTarget = null
            }
        )
    }
}

@Composable
private fun GroupHeader(
    group: NoteGroupUi,
    onToggle: () -> Unit
) {
    Card(
        onClick = onToggle,
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = androidx.compose.material3.MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${group.title} (${group.notes.size})",
                style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                imageVector = if (group.isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyFolderState(
    modifier: Modifier,
    onPickFolder: () -> Unit
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Selecione uma pasta para comecar")
        Spacer(Modifier.height(16.dp))
        FilledTonalButton(
            onClick = onPickFolder,
            shape = androidx.compose.material3.MaterialTheme.shapes.medium
        ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Escolher pasta")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteCard(
    note: NoteMeta,
    tag: String?,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onSetTag: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
            } else {
                androidx.compose.material3.MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = androidx.compose.material3.MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onTap,
                onLongClick = onLongPress
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Description,
                contentDescription = null,
                tint = if (isSelected) {
                    androidx.compose.material3.MaterialTheme.colorScheme.primary
                } else {
                    androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = note.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!tag.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    AssistChip(
                        onClick = onSetTag,
                        label = { Text(tag) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Label,
                                contentDescription = null,
                                modifier = Modifier.width(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
            if (!isSelectionMode) {
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu",
                        tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                shape = androidx.compose.material3.MaterialTheme.shapes.medium,
                tonalElevation = 2.dp
            ) {
                DropdownMenuItem(
                    text = { Text("Renomear") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        expanded = false
                        onRename()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Definir tag") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Label,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        expanded = false
                        onSetTag()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Deletar", color = androidx.compose.material3.MaterialTheme.colorScheme.error) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = {
                        expanded = false
                        onDelete()
                    }
                )
            }
        }
    }
}

@Composable
private fun TagDialog(
    tags: Set<String>,
    currentTag: String?,
    onDismiss: () -> Unit,
    onApply: (String?) -> Unit
) {
    var value by remember(currentTag) { mutableStateOf(currentTag.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        title = { Text("Definir tag") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    singleLine = true,
                    label = { Text("Tag") },
                    placeholder = { Text("Ex.: Trabalho") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                        disabledContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                        focusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.outline,
                        cursorColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
                    )
                )
                if (tags.isNotEmpty()) {
                    Text(
                        text = "Tags existentes",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    tags.sortedBy { it.lowercase() }.forEach { tag ->
                        TextButton(onClick = { value = tag }) {
                            Text(tag)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(value.trim().ifBlank { null }) }) {
                Text("Aplicar")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onApply(null) }) {
                    Text("Sem tag")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        }
    )
}

@Composable
private fun RenameDialog(
    note: NoteMeta,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember(note.uri) { mutableStateOf(note.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        title = { Text("Renomear nota") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                placeholder = { Text("novo-nome.md") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                    disabledContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                    focusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.outline,
                    cursorColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim()) }) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
