package com.offlinenotes.ui.editor

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.offlinenotes.ui.theme.ThemePalette
import com.offlinenotes.viewmodel.EditorEvent
import com.offlinenotes.viewmodel.EditorViewModel
import com.offlinenotes.viewmodel.NotesListViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EditorScreen(
    paddingValues: PaddingValues,
    noteUri: Uri,
    palette: ThemePalette,
    notesViewModel: NotesListViewModel,
    onFolderSelected: (Uri, Int) -> Unit,
    onBack: () -> Unit,
    onNavigateToNote: (Uri) -> Unit
) {
    val maxEditorWidth = 800.dp
    val actionSlotSize = 48.dp
    val app = LocalContext.current.applicationContext as Application
    val viewModel: EditorViewModel = viewModel(
        key = noteUri.toString(),
        factory = EditorViewModel.factory(app, noteUri)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showRenameDialog by remember { mutableStateOf(false) }
    var skipDisposeAutoSave by remember { mutableStateOf(false) }
    var isPreviewMode by rememberSaveable(noteUri.toString()) { mutableStateOf(false) }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    val isOrgNote = uiState.title.endsWith(".org")
    val currentExtension = if (isOrgNote) ".org" else ".md"
    val colorScheme = androidx.compose.material3.MaterialTheme.colorScheme
    val syntaxHighlighting = remember(
        isOrgNote,
        colorScheme.primary,
        colorScheme.onSurface,
        colorScheme.onSurfaceVariant
    ) {
        SyntaxHighlightingTransformation(
            isOrg = isOrgNote,
            listMarkerColor = colorScheme.primary,
            codeTextColor = colorScheme.onSurface,
            codeDelimiterColor = colorScheme.onSurfaceVariant
        )
    }
    var renameValue by remember(uiState.title) {
        mutableStateOf(uiState.title.removeSuffix(".md").removeSuffix(".org"))
    }

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

    DisposableEffect(viewModel) {
        onDispose {
            if (!skipDisposeAutoSave) {
                viewModel.saveSilently()
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event is EditorEvent.ShowMessage) {
                val result = snackbarHostState.showSnackbar(
                    message = event.message,
                    actionLabel = if (event.allowReselect) "Re-selecionar" else null
                )
                if (event.allowReselect && result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                    launchFolderPicker()
                }
            }
        }
    }

    LaunchedEffect(uiState.editorValue.selection, isPreviewMode) {
        if (isPreviewMode) {
            return@LaunchedEffect
        }
        bringIntoViewRequester.bringIntoView()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                ExplorerPanel(
                    viewModel = notesViewModel,
                    onNoteClick = { uri ->
                        scope.launch {
                            drawerState.close()
                            viewModel.saveSilently {
                                skipDisposeAutoSave = true
                                onNavigateToNote(uri)
                            }
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = uiState.title,
                            modifier = Modifier.clickable { showRenameDialog = true }
                        )
                    },
                    navigationIcon = {
                        Row {
                            IconButton(onClick = {
                                viewModel.saveSilently {
                                    skipDisposeAutoSave = true
                                    onBack()
                                }
                            }) {
                                Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Voltar")
                            }
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Folder, contentDescription = "Explorer")
                            }
                        }
                    },
                    actions = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { isPreviewMode = false }) {
                                Text(
                                    text = "Edit",
                                    color = if (!isPreviewMode) {
                                        androidx.compose.material3.MaterialTheme.colorScheme.primary
                                    } else {
                                        androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                            TextButton(onClick = { isPreviewMode = true }) {
                                Text(
                                    text = "View",
                                    color = if (isPreviewMode) {
                                        androidx.compose.material3.MaterialTheme.colorScheme.primary
                                    } else {
                                        androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                        Box(modifier = Modifier.size(actionSlotSize), contentAlignment = Alignment.Center) {
                            if (!isPreviewMode) {
                                IconButton(onClick = { viewModel.save() }) {
                                    Icon(
                                        imageVector = Icons.Default.Save,
                                        contentDescription = "Salvar",
                                        tint = androidx.compose.material3.MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
                        titleContentColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { scaffoldPadding ->
            if (uiState.isLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = paddingValues.calculateBottomPadding())
                        .padding(scaffoldPadding),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator()
                }
                return@Scaffold
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = paddingValues.calculateBottomPadding())
                    .padding(scaffoldPadding)
                    .imePadding()
                    .padding(16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .fillMaxWidth()
                        .widthIn(max = maxEditorWidth),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!isPreviewMode) {
                        FormattingToolbar(
                            onAction = { action ->
                                viewModel.applyFormatting(action = action, isOrg = isOrgNote)
                            }
                        )
                        TextField(
                            value = uiState.editorValue,
                            onValueChange = viewModel::onEditorValueChange,
                            modifier = Modifier
                                .fillMaxSize()
                                .bringIntoViewRequester(bringIntoViewRequester),
                            textStyle = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                            placeholder = { Text("Escreva sua nota...") },
                            visualTransformation = syntaxHighlighting,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                                disabledContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                                focusedIndicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.outline,
                                cursorColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
                            ),
                            shape = androidx.compose.material3.MaterialTheme.shapes.medium
                        )
                    } else {
                        NotePreviewContent(
                            text = uiState.editorValue.text,
                            isOrg = isOrgNote,
                            palette = palette,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                        )
                    }
                }
            }
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Renomear arquivo") },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    singleLine = true,
                    placeholder = { Text("nome-do-arquivo") },
                    supportingText = { Text("Extensao fixa: $currentExtension") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showRenameDialog = false
                    viewModel.renameCurrentNote(renameValue)
                }) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
