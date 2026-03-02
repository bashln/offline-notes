package com.offlinenotes.ui.editor

import android.app.Application
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.offlinenotes.data.SettingsRepository
import com.offlinenotes.viewmodel.EditorEvent
import com.offlinenotes.viewmodel.EditorViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    paddingValues: PaddingValues,
    noteUri: Uri,
    onBack: () -> Unit
) {
    val app = LocalContext.current.applicationContext as Application
    val settingsRepository = remember(app) { SettingsRepository(app) }
    val scope = rememberCoroutineScope()
    val viewModel: EditorViewModel = viewModel(
        key = noteUri.toString(),
        factory = EditorViewModel.factory(app, noteUri)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showRenameDialog by remember { mutableStateOf(false) }
    val currentExtension = if (uiState.title.endsWith(".org")) ".org" else ".md"
    var renameValue by remember(uiState.title) {
        mutableStateOf(uiState.title.removeSuffix(".md").removeSuffix(".org"))
    }

    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val flags =
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching {
                app.contentResolver.takePersistableUriPermission(uri, flags)
            }
            scope.launch {
                settingsRepository.saveRootUri(uri)
                snackbarHostState.showSnackbar("Pasta atualizada")
            }
        }
    }

    DisposableEffect(viewModel) {
        onDispose {
            viewModel.saveSilently()
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
                    folderLauncher.launch(null)
                }
            }
        }
    }

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
                    IconButton(onClick = {
                        viewModel.saveSilently()
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.save() }) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Salvar",
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.primary
                        )
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
                    .padding(paddingValues)
                    .padding(scaffoldPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(scaffoldPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = uiState.text,
                onValueChange = viewModel::onTextChanged,
                modifier = Modifier.fillMaxSize(),
                textStyle = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                placeholder = { Text("Escreva sua nota...") },
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
