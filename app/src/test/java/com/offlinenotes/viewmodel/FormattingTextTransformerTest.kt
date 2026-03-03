package com.offlinenotes.viewmodel

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class FormattingTextTransformerTest {

    @Test
    fun `wraps markdown selection in bold`() {
        val value = TextFieldValue(text = "hello", selection = TextRange(0, 5))

        val transformed = FormattingTextTransformer.apply(value, FormattingAction.Bold, isOrg = false)

        assertEquals("**hello**", transformed.text)
        assertEquals(TextRange(2, 7), transformed.selection)
    }

    @Test
    fun `inserts markdown italic markers at cursor`() {
        val value = TextFieldValue(text = "hello", selection = TextRange(5))

        val transformed = FormattingTextTransformer.apply(value, FormattingAction.Italic, isOrg = false)

        assertEquals("hello__", transformed.text)
        assertEquals(TextRange(6), transformed.selection)
    }

    @Test
    fun `prefixes markdown heading on current line`() {
        val value = TextFieldValue(text = "title", selection = TextRange(5))

        val transformed = FormattingTextTransformer.apply(value, FormattingAction.Heading, isOrg = false)

        assertEquals("# title", transformed.text)
        assertEquals(TextRange(7), transformed.selection)
    }

    @Test
    fun `quotes multiple markdown lines`() {
        val value = TextFieldValue(text = "one\ntwo", selection = TextRange(0, 7))

        val transformed = FormattingTextTransformer.apply(value, FormattingAction.Quote, isOrg = false)

        assertEquals("> one\n> two", transformed.text)
        assertEquals(TextRange(2, 11), transformed.selection)
    }

    @Test
    fun `wraps org selection in quote block`() {
        val value = TextFieldValue(text = "line", selection = TextRange(0, 4))

        val transformed = FormattingTextTransformer.apply(value, FormattingAction.Quote, isOrg = true)

        assertEquals("#+begin_quote\nline\n#+end_quote", transformed.text)
        assertEquals(TextRange(14, 18), transformed.selection)
    }

    @Test
    fun `inserts org code markers at cursor`() {
        val value = TextFieldValue(text = "abc", selection = TextRange(1))

        val transformed = FormattingTextTransformer.apply(value, FormattingAction.Code, isOrg = true)

        assertEquals("a==bc", transformed.text)
        assertEquals(TextRange(2), transformed.selection)
    }
}
