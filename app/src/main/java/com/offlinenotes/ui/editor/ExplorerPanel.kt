package com.offlinenotes.ui.editor

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.offlinenotes.domain.FileTypeFilter
import com.offlinenotes.viewmodel.ExplorerNode
import com.offlinenotes.viewmodel.NotesListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerPanel(
    viewModel: NotesListViewModel,
    onNoteClick: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Explorer",
            style = MaterialTheme.typography.titleLarge
        )
        
        Spacer(Modifier.height(16.dp))
        
        // Type filter chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = uiState.typeFilter == FileTypeFilter.ALL,
                onClick = { viewModel.onTypeFilterSelected(FileTypeFilter.ALL) },
                label = { Text("Tudo") }
            )
            FilterChip(
                selected = uiState.typeFilter == FileTypeFilter.ORG,
                onClick = { viewModel.onTypeFilterSelected(FileTypeFilter.ORG) },
                label = { Text("Org") }
            )
            FilterChip(
                selected = uiState.typeFilter == FileTypeFilter.MARKDOWN,
                onClick = { viewModel.onTypeFilterSelected(FileTypeFilter.MARKDOWN) },
                label = { Text("MD") }
            )
        }
        
        Spacer(Modifier.height(8.dp))
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(uiState.explorerTree) { node ->
                ExplorerNodeItem(
                    node = node,
                    onNoteClick = onNoteClick,
                    onToggleExpand = { viewModel.toggleGroupExpansion("folder:${it.path}") }
                )
            }
        }
    }
}

@Composable
fun ExplorerNodeItem(
    node: ExplorerNode,
    onNoteClick: (Uri) -> Unit,
    onToggleExpand: (ExplorerNode) -> Unit,
    level: Int = 0
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (node.isFolder) {
                        onToggleExpand(node)
                    } else {
                        node.uri?.let(onNoteClick)
                    }
                }
                .padding(vertical = 8.dp)
                .padding(start = (level * 16).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when {
                    node.isFolder && node.isExpanded -> Icons.Default.ExpandMore
                    node.isFolder -> Icons.AutoMirrored.Filled.KeyboardArrowRight
                    else -> Icons.Default.Description
                },
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (node.isFolder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(Modifier.width(8.dp))
            
            Text(
                text = node.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (node.isFolder) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (node.isFolder && node.isExpanded) {
            node.children.forEach { child ->
                ExplorerNodeItem(
                    node = child,
                    onNoteClick = onNoteClick,
                    onToggleExpand = onToggleExpand,
                    level = level + 1
                )
            }
        }
    }
}
