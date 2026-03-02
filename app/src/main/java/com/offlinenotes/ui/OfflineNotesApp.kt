package com.offlinenotes.ui

import android.app.Application
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.offlinenotes.ui.editor.EditorScreen
import com.offlinenotes.ui.notes.CreateNoteDialog
import com.offlinenotes.ui.notes.NotesListScreen
import com.offlinenotes.ui.sync.SyncScreen
import com.offlinenotes.viewmodel.NotesListEvent
import com.offlinenotes.viewmodel.NotesListViewModel

private object Routes {
    const val NOTES = "notes"
    const val SYNC = "sync"
    const val EDITOR = "editor"
    const val EDITOR_ARG = "noteUri"
    const val EDITOR_ROUTE = "$EDITOR/{$EDITOR_ARG}"
}

@Composable
fun OfflineNotesApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val app = context.applicationContext as Application

    val notesViewModel: NotesListViewModel = viewModel(
        factory = NotesListViewModel.factory(app)
    )
    val notesState by notesViewModel.uiState.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }

    ObserveNotesEvents(notesViewModel) { event ->
        when (event) {
            is NotesListEvent.OpenEditor -> {
                navController.navigate("${Routes.EDITOR}/${Uri.encode(event.noteUri.toString())}")
            }

            is NotesListEvent.ShowMessage -> Unit
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val showFab = currentRoute == Routes.NOTES && notesState.rootUri != null

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                NavigationBarItem(
                    selected = currentDestination?.hierarchy?.any { it.route == Routes.NOTES } == true,
                    onClick = {
                        navController.navigate(Routes.NOTES) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.Description, contentDescription = "Notas") },
                    label = { Text("Notas") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                        unselectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = currentDestination?.hierarchy?.any { it.route == Routes.SYNC } == true,
                    onClick = {
                        navController.navigate(Routes.SYNC) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.Cloud, contentDescription = "Sync") },
                    label = { Text("Sync") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                        unselectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        },
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Criar nota")
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
                    viewModel = notesViewModel
                )
            }
            composable(Routes.SYNC) {
                SyncScreen(paddingValues = padding)
            }
            composable(
                route = Routes.EDITOR_ROUTE,
                arguments = listOf(navArgument(Routes.EDITOR_ARG) { type = NavType.StringType })
            ) { backStackEntry ->
                val encoded = backStackEntry.arguments?.getString(Routes.EDITOR_ARG).orEmpty()
                val uri = Uri.decode(encoded).toUri()
                EditorScreen(
                    paddingValues = padding,
                    noteUri = uri,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }

    if (showCreateDialog) {
        CreateNoteDialog(
            initialName = notesViewModel.suggestedName(),
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, kind ->
                showCreateDialog = false
                notesViewModel.createNote(name, kind)
            }
        )
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
