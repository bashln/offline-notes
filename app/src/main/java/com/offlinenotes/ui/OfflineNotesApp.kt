package com.offlinenotes.ui

import android.app.Application
import android.util.Base64
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.offlinenotes.domain.NoteKind
import com.offlinenotes.ui.editor.EditorScreen
import com.offlinenotes.ui.notes.NotesListScreen
import com.offlinenotes.ui.privacy.PrivacyScreen
import com.offlinenotes.ui.settings.SettingsScreen
import com.offlinenotes.ui.sync.SyncScreen
import com.offlinenotes.ui.theme.OfflineNotesTheme
import com.offlinenotes.viewmodel.NotesListEvent
import com.offlinenotes.viewmodel.NotesListViewModel
import com.offlinenotes.viewmodel.ThemeViewModel

private object Routes {
    const val NOTES = "notes"
    const val SYNC = "sync"
    const val EDITOR = "editor"
    const val SETTINGS = "settings"
    const val PRIVACY = "privacy"
    const val EDITOR_ARG = "noteUri"
    const val EDITOR_ROUTE = "$EDITOR/{$EDITOR_ARG}"
}

@Composable
fun OfflineNotesApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val themeViewModel: ThemeViewModel = viewModel(
        factory = ThemeViewModel.factory(app)
    )
    val themeState by themeViewModel.uiState.collectAsStateWithLifecycle()
    val notesViewModel: NotesListViewModel = viewModel(
        factory = NotesListViewModel.factory(app)
    )
    val notesState by notesViewModel.uiState.collectAsStateWithLifecycle()
    var showFabMenu by remember { mutableStateOf(false) }

    ObserveNotesEvents(notesViewModel) { event ->
        when (event) {
            is NotesListEvent.OpenEditor -> {
                val encoded = Base64.encodeToString(
                    event.noteUri.toString().toByteArray(Charsets.UTF_8),
                    Base64.URL_SAFE or Base64.NO_WRAP
                )
                navController.navigate("${Routes.EDITOR}/$encoded")
            }

            is NotesListEvent.ShowMessage -> Unit
        }
    }

    OfflineNotesTheme(
        palette = themeState.palette,
        mode = themeState.mode
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val showFab = currentRoute == Routes.NOTES && notesState.rootUri != null

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            floatingActionButton = {
                if (showFab) {
                    androidx.compose.foundation.layout.Box {
                        FloatingActionButton(
                            onClick = { notesViewModel.createQuickNote() },
                            modifier = Modifier.pointerInput(Unit) {
                                detectTapGestures(onLongPress = { showFabMenu = true })
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Criar nota")
                        }
                        DropdownMenu(
                            expanded = showFabMenu,
                            onDismissRequest = { showFabMenu = false },
                            modifier = Modifier.wrapContentSize()
                        ) {
                            DropdownMenuItem(
                                text = { Text("Nota (.md)") },
                                onClick = {
                                    showFabMenu = false
                                    notesViewModel.createQuickNote(NoteKind.MARKDOWN_NOTE)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Checklist (.md)") },
                                onClick = {
                                    showFabMenu = false
                                    notesViewModel.createQuickNote(NoteKind.MARKDOWN_TASKS)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Org (.org)") },
                                onClick = {
                                    showFabMenu = false
                                    notesViewModel.createQuickNote(NoteKind.ORG_NOTE)
                                }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Routes.NOTES,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Routes.NOTES) {
                    NotesListScreen(
                        paddingValues = padding,
                        viewModel = notesViewModel,
                        onFolderSelected = notesViewModel::onFolderSelected,
                        onOpenSettings = {
                            navController.navigate(Routes.SETTINGS) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        paddingValues = padding,
                        isOrgDefault = notesState.defaultQuickKind == NoteKind.ORG_NOTE,
                        currentThemePalette = themeState.palette,
                        currentThemeMode = themeState.mode,
                        onBack = { navController.popBackStack() },
                        onFolderSelected = notesViewModel::onFolderSelected,
                        onToggleDefaultFormat = notesViewModel::toggleDefaultQuickFormat,
                        onThemePaletteSelected = themeViewModel::setThemePalette,
                        onThemeModeSelected = themeViewModel::setThemeMode,
                        onOpenSyncHelp = {
                            navController.navigate(Routes.SYNC) {
                                launchSingleTop = true
                            }
                        },
                        onOpenPrivacy = {
                            navController.navigate(Routes.PRIVACY) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable(Routes.SYNC) {
                    SyncScreen(paddingValues = padding)
                }
                composable(Routes.PRIVACY) {
                    PrivacyScreen(
                        paddingValues = padding,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = Routes.EDITOR_ROUTE,
                    arguments = listOf(navArgument(Routes.EDITOR_ARG) { type = NavType.StringType })
                ) { backStackEntry ->
                    val encoded = backStackEntry.arguments?.getString(Routes.EDITOR_ARG).orEmpty()
                    val decoded = String(
                        Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_WRAP),
                        Charsets.UTF_8
                    )
                    val uri = decoded.toUri()
                    EditorScreen(
                        paddingValues = padding,
                        noteUri = uri,
                        onFolderSelected = notesViewModel::onFolderSelected,
                        onBack = {
                            navController.popBackStack()
                            notesViewModel.refreshNotes(forceReload = true)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ObserveNotesEvents(
    viewModel: NotesListViewModel,
    onEvent: (NotesListEvent) -> Unit
) {
    androidx.compose.runtime.LaunchedEffect(viewModel) {
        viewModel.events.collect(onEvent)
    }
}
