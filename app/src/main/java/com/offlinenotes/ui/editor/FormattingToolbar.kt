package com.offlinenotes.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.offlinenotes.viewmodel.FormattingAction

private data class ToolbarItem(
    val action: FormattingAction,
    val label: String
)

@Composable
fun FormattingToolbar(
    modifier: Modifier = Modifier,
    onAction: (FormattingAction) -> Unit
) {
    val items = listOf(
        ToolbarItem(FormattingAction.Bold, "Bold"),
        ToolbarItem(FormattingAction.Italic, "Italic"),
        ToolbarItem(FormattingAction.Heading, "Heading"),
        ToolbarItem(FormattingAction.Code, "Code"),
        ToolbarItem(FormattingAction.Quote, "Quote")
    )

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        state = rememberLazyListState(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items) { item ->
            AssistChip(
                onClick = { onAction(item.action) },
                label = { Text(item.label) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}
