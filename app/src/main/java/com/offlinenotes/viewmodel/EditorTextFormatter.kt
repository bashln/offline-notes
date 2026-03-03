package com.offlinenotes.viewmodel

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

object EditorTextFormatter {
    private val listPattern = Regex("^(\\s*)([-*]|(\\d+)\\.)\\s(.*)$")

    fun applyAutoListContinuation(
        previousValue: TextFieldValue,
        nextValue: TextFieldValue
    ): TextFieldValue {
        if (!previousValue.selection.collapsed || !nextValue.selection.collapsed) {
            return nextValue
        }

        val insertedNewlineIndex = nextValue.selection.start - 1
        if (insertedNewlineIndex < 0 || insertedNewlineIndex >= nextValue.text.length) {
            return nextValue
        }
        if (nextValue.text[insertedNewlineIndex] != '\n') {
            return nextValue
        }
        if (nextValue.text.length != previousValue.text.length + 1) {
            return nextValue
        }

        val originalCursor = previousValue.selection.start
        if (originalCursor != insertedNewlineIndex) {
            return nextValue
        }

        val removedInsertedNewline = nextValue.text.removeRange(insertedNewlineIndex, insertedNewlineIndex + 1)
        if (removedInsertedNewline != previousValue.text) {
            return nextValue
        }

        val lineStart = previousValue.text.lastIndexOf('\n', startIndex = originalCursor - 1) + 1
        val lineEnd = previousValue.text.indexOf('\n', startIndex = originalCursor)
            .takeIf { it >= 0 } ?: previousValue.text.length
        val line = previousValue.text.substring(lineStart, lineEnd)
        val match = listPattern.matchEntire(line) ?: return nextValue

        val indentation = match.groupValues[1]
        val marker = match.groupValues[2]
        val content = match.groupValues[4]

        return if (content.isBlank()) {
            val replacement = "$indentation\n"
            val transformed = previousValue.text.replaceRange(lineStart, lineEnd, replacement)
            val selection = TextRange(lineStart + indentation.length)
            nextValue.copy(text = transformed, selection = selection, composition = null)
        } else {
            val continuationMarker = buildContinuationMarker(marker)
            val insertion = "\n$indentation$continuationMarker "
            val transformed = previousValue.text.replaceRange(originalCursor, originalCursor, insertion)
            val cursor = originalCursor + insertion.length
            nextValue.copy(text = transformed, selection = TextRange(cursor), composition = null)
        }
    }

    private fun buildContinuationMarker(marker: String): String {
        val numeric = marker.removeSuffix(".").toIntOrNull()
        return if (numeric != null) {
            "${numeric + 1}."
        } else {
            marker
        }
    }
}
