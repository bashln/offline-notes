package com.offlinenotes.viewmodel

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

enum class FormattingAction {
    Bold,
    Italic,
    Heading,
    Code,
    Quote
}

object FormattingTextTransformer {
    fun apply(
        value: TextFieldValue,
        action: FormattingAction,
        isOrg: Boolean
    ): TextFieldValue {
        return when (action) {
            FormattingAction.Bold -> wrapSelectionOrInsert(value, if (isOrg) "*" else "**")
            FormattingAction.Italic -> wrapSelectionOrInsert(value, if (isOrg) "/" else "_")
            FormattingAction.Code -> wrapSelectionOrInsert(value, if (isOrg) "=" else "`")
            FormattingAction.Heading -> prefixCurrentLine(value, if (isOrg) "* " else "# ")
            FormattingAction.Quote -> {
                if (isOrg) {
                    wrapWithOrgQuoteBlock(value)
                } else {
                    prefixQuotedLines(value)
                }
            }
        }
    }

    private fun wrapSelectionOrInsert(
        value: TextFieldValue,
        delimiter: String
    ): TextFieldValue {
        val start = value.selection.min
        val end = value.selection.max
        val selectedText = value.text.substring(start, end)
        val replacement = buildString {
            append(delimiter)
            append(selectedText)
            append(delimiter)
        }
        val transformed = value.text.replaceRange(start, end, replacement)
        val selection = if (value.selection.collapsed) {
            TextRange(start + delimiter.length)
        } else {
            TextRange(start + delimiter.length, start + delimiter.length + selectedText.length)
        }
        return value.copy(text = transformed, selection = selection, composition = null)
    }

    private fun prefixCurrentLine(
        value: TextFieldValue,
        prefix: String
    ): TextFieldValue {
        val lineStart = findLineStart(value.text, value.selection.min)
        val transformed = value.text.replaceRange(lineStart, lineStart, prefix)
        val selection = TextRange(
            start = value.selection.min + prefix.length,
            end = value.selection.max + prefix.length
        )
        return value.copy(text = transformed, selection = selection, composition = null)
    }

    private fun prefixQuotedLines(value: TextFieldValue): TextFieldValue {
        if (value.selection.collapsed) {
            return prefixCurrentLine(value, "> ")
        }

        val start = findLineStart(value.text, value.selection.min)
        val end = findSelectionLineEnd(value.text, value.selection.max)
        val selectedBlock = value.text.substring(start, end)
        val quotedBlock = selectedBlock.lineSequence()
            .joinToString("\n") { line -> "> $line" }
        val transformed = value.text.replaceRange(start, end, quotedBlock)
        val addedLength = quotedBlock.length - selectedBlock.length
        val selection = TextRange(
            start = value.selection.min + 2,
            end = value.selection.max + addedLength
        )
        return value.copy(text = transformed, selection = selection, composition = null)
    }

    private fun wrapWithOrgQuoteBlock(value: TextFieldValue): TextFieldValue {
        val start = value.selection.min
        val end = value.selection.max
        val blockOpen = "#+begin_quote\n"
        val blockClose = "\n#+end_quote"

        return if (value.selection.collapsed) {
            val block = "$blockOpen$blockClose"
            val transformed = value.text.replaceRange(start, end, block)
            val cursor = start + blockOpen.length
            value.copy(text = transformed, selection = TextRange(cursor), composition = null)
        } else {
            val selectedText = value.text.substring(start, end)
            val replacement = "$blockOpen$selectedText$blockClose"
            val transformed = value.text.replaceRange(start, end, replacement)
            val selection = TextRange(start + blockOpen.length, start + blockOpen.length + selectedText.length)
            value.copy(text = transformed, selection = selection, composition = null)
        }
    }

    private fun findLineStart(text: String, index: Int): Int {
        if (text.isEmpty()) {
            return 0
        }
        val safeIndex = index.coerceIn(0, text.length)
        return text.lastIndexOf('\n', startIndex = safeIndex - 1) + 1
    }

    private fun findSelectionLineEnd(text: String, index: Int): Int {
        if (text.isEmpty()) {
            return 0
        }
        val safeIndex = index.coerceIn(0, text.length)
        val adjustedIndex = if (safeIndex > 0 && safeIndex == text.length) safeIndex - 1 else safeIndex
        val newlineIndex = text.indexOf('\n', startIndex = adjustedIndex)
        return if (newlineIndex >= 0) newlineIndex else text.length
    }
}
