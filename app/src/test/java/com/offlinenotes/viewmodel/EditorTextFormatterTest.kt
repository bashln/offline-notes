package com.offlinenotes.viewmodel

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class EditorTextFormatterTest {

    @Test
    fun `continues unordered list item on enter`() {
        val previous = TextFieldValue(text = "- item", selection = TextRange(6))
        val next = TextFieldValue(text = "- item\n", selection = TextRange(7))

        val transformed = EditorTextFormatter.applyAutoListContinuation(previous, next)

        assertEquals("- item\n- ", transformed.text)
        assertEquals(9, transformed.selection.start)
    }

    @Test
    fun `increments ordered list marker on enter`() {
        val previous = TextFieldValue(text = "1. item", selection = TextRange(7))
        val next = TextFieldValue(text = "1. item\n", selection = TextRange(8))

        val transformed = EditorTextFormatter.applyAutoListContinuation(previous, next)

        assertEquals("1. item\n2. ", transformed.text)
        assertEquals(11, transformed.selection.start)
    }

    @Test
    fun `preserves indentation for nested bullets`() {
        val previous = TextFieldValue(text = "  * nested", selection = TextRange(10))
        val next = TextFieldValue(text = "  * nested\n", selection = TextRange(11))

        val transformed = EditorTextFormatter.applyAutoListContinuation(previous, next)

        assertEquals("  * nested\n  * ", transformed.text)
        assertEquals(15, transformed.selection.start)
    }

    @Test
    fun `removes empty list marker on enter`() {
        val previous = TextFieldValue(text = "- ", selection = TextRange(2))
        val next = TextFieldValue(text = "- \n", selection = TextRange(3))

        val transformed = EditorTextFormatter.applyAutoListContinuation(previous, next)

        assertEquals("\n", transformed.text)
        assertEquals(0, transformed.selection.start)
    }

    @Test
    fun `ignores edits that are not a single enter insertion`() {
        val previous = TextFieldValue(text = "- item", selection = TextRange(6))
        val next = TextFieldValue(text = "- item\nx", selection = TextRange(8))

        val transformed = EditorTextFormatter.applyAutoListContinuation(previous, next)

        assertEquals(next, transformed)
    }
}
