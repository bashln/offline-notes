package com.offlinenotes.ui.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewParserTest {

    @Test
    fun parsePreviewBlocks_markdownListsAndChecklist() {
        val input = """
            - item 1
              - [x] done item
              - [ ] pending item
            + item 2
        """.trimIndent()

        val blocks = parsePreviewBlocks(input, isOrg = false)

        assertEquals(4, blocks.size)
        assertTrue(blocks[0] is PreviewBlock.Bullet)
        assertEquals(0, (blocks[0] as PreviewBlock.Bullet).indentLevel)
        assertTrue(blocks[1] is PreviewBlock.Checklist)
        assertTrue((blocks[1] as PreviewBlock.Checklist).checked)
        assertEquals(1, (blocks[1] as PreviewBlock.Checklist).indentLevel)
        assertTrue(blocks[2] is PreviewBlock.Checklist)
        assertTrue(!(blocks[2] as PreviewBlock.Checklist).checked)
        assertTrue(blocks[3] is PreviewBlock.Bullet)
    }

    @Test
    fun parsePreviewBlocks_markdownFencedCodeBlock() {
        val input = """
            # titulo
            ```kotlin
            val x = 1
            println(x)
            ```
            texto final
        """.trimIndent()

        val blocks = parsePreviewBlocks(input, isOrg = false)

        assertTrue(blocks[0] is PreviewBlock.Heading)
        assertTrue(blocks[1] is PreviewBlock.CodeBlock)
        val code = blocks[1] as PreviewBlock.CodeBlock
        assertEquals("kotlin", code.languageHint)
        assertEquals("val x = 1\nprintln(x)", code.content)
        assertTrue(blocks[2] is PreviewBlock.Paragraph)
    }

    @Test
    fun parsePreviewBlocks_orgSourceCodeBlock() {
        val input = """
            * Titulo
            #+begin_src sh
            echo "oi"
            #+end_src
            - [X] concluido
        """.trimIndent()

        val blocks = parsePreviewBlocks(input, isOrg = true)

        assertTrue(blocks[0] is PreviewBlock.Heading)
        assertTrue(blocks[1] is PreviewBlock.CodeBlock)
        assertEquals("sh", (blocks[1] as PreviewBlock.CodeBlock).languageHint)
        assertTrue(blocks[2] is PreviewBlock.Checklist)
        assertTrue((blocks[2] as PreviewBlock.Checklist).checked)
    }

    @Test
    fun parsePreviewBlocks_fallbackForUnknownLine() {
        val input = "linha sem marcador"

        val blocks = parsePreviewBlocks(input, isOrg = false)

        assertEquals(1, blocks.size)
        assertTrue(blocks.first() is PreviewBlock.Paragraph)
        assertEquals("linha sem marcador", (blocks.first() as PreviewBlock.Paragraph).text)
    }

    @Test
    fun parsePreviewBlocks_blockquote_singleLine() {
        val input = "> texto citado"

        val blocks = parsePreviewBlocks(input, isOrg = false)

        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is PreviewBlock.Blockquote)
        assertEquals("texto citado", (blocks[0] as PreviewBlock.Blockquote).text)
    }

    @Test
    fun parsePreviewBlocks_blockquote_multipleLines() {
        val input = """
            > linha 1
            > linha 2
            > linha 3
        """.trimIndent()

        val blocks = parsePreviewBlocks(input, isOrg = false)

        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is PreviewBlock.Blockquote)
        assertEquals("linha 1\nlinha 2\nlinha 3", (blocks[0] as PreviewBlock.Blockquote).text)
    }

    @Test
    fun parsePreviewBlocks_blockquote_noSpaceAfterMarker() {
        val input = ">sem espaco"

        val blocks = parsePreviewBlocks(input, isOrg = false)

        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is PreviewBlock.Blockquote)
        assertEquals("sem espaco", (blocks[0] as PreviewBlock.Blockquote).text)
    }

    @Test
    fun parsePreviewBlocks_horizontalRule_markdown_dashes() {
        val input = "---"
        val blocks = parsePreviewBlocks(input, isOrg = false)
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is PreviewBlock.HorizontalRule)
    }

    @Test
    fun parsePreviewBlocks_horizontalRule_markdown_asterisks() {
        val input = "***"
        val blocks = parsePreviewBlocks(input, isOrg = false)
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is PreviewBlock.HorizontalRule)
    }

    @Test
    fun parsePreviewBlocks_horizontalRule_markdown_underscores() {
        val input = "___"
        val blocks = parsePreviewBlocks(input, isOrg = false)
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is PreviewBlock.HorizontalRule)
    }

    @Test
    fun parsePreviewBlocks_horizontalRule_org() {
        val input = "-----"
        val blocks = parsePreviewBlocks(input, isOrg = true)
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is PreviewBlock.HorizontalRule)
    }

    @Test
    fun parsePreviewBlocks_horizontalRule_notConfusedWithBullet() {
        val input = "- item"
        val blocks = parsePreviewBlocks(input, isOrg = false)
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is PreviewBlock.Bullet)
    }

    @Test
    fun parsePreviewBlocks_headingLevels_markdown() {
        for (level in 1..6) {
            val prefix = "#".repeat(level)
            val input = "$prefix Heading $level"
            val blocks = parsePreviewBlocks(input, isOrg = false)
            assertEquals(1, blocks.size)
            assertTrue(blocks[0] is PreviewBlock.Heading)
            assertEquals(level, (blocks[0] as PreviewBlock.Heading).level)
            assertEquals("Heading $level", (blocks[0] as PreviewBlock.Heading).text)
        }
    }

    @Test
    fun parsePreviewBlocks_headingLevels_org() {
        for (level in 1..6) {
            val prefix = "*".repeat(level)
            val input = "$prefix Heading $level"
            val blocks = parsePreviewBlocks(input, isOrg = true)
            assertEquals(1, blocks.size)
            assertTrue(blocks[0] is PreviewBlock.Heading)
            assertEquals(level, (blocks[0] as PreviewBlock.Heading).level)
            assertEquals("Heading $level", (blocks[0] as PreviewBlock.Heading).text)
        }
    }

    @Test
    fun parsePreviewBlocks_orderedList_markdown() {
        val input = "1. First item\n2. Second item\n3. Third item"

        val blocks = parsePreviewBlocks(input, isOrg = false)

        assertEquals(3, blocks.size)
        assertTrue(blocks.all { it is PreviewBlock.NumberedBullet })
        assertEquals("1.", (blocks[0] as PreviewBlock.NumberedBullet).number)
        assertEquals("First item", (blocks[0] as PreviewBlock.NumberedBullet).text)
        assertEquals(0, (blocks[0] as PreviewBlock.NumberedBullet).indentLevel)
        assertEquals("3.", (blocks[2] as PreviewBlock.NumberedBullet).number)
    }

    @Test
    fun parsePreviewBlocks_orderedList_org_parenthesis() {
        val input = "1) Item A\n2) Item B"

        val blocks = parsePreviewBlocks(input, isOrg = true)

        assertEquals(2, blocks.size)
        assertTrue(blocks[0] is PreviewBlock.NumberedBullet)
        assertEquals("1)", (blocks[0] as PreviewBlock.NumberedBullet).number)
        assertEquals("Item A", (blocks[0] as PreviewBlock.NumberedBullet).text)
    }

    @Test
    fun parsePreviewBlocks_orderedList_indented() {
        val input = "  1. Indented item"

        val blocks = parsePreviewBlocks(input, isOrg = false)

        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is PreviewBlock.NumberedBullet)
        assertEquals(1, (blocks[0] as PreviewBlock.NumberedBullet).indentLevel)
    }

    @Test
    fun parsePreviewBlocks_consecutiveLines_grouped() {
        val input = "Line one\nLine two\nLine three"

        val blocks = parsePreviewBlocks(input, isOrg = false)

        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is PreviewBlock.Paragraph)
        assertEquals("Line one\nLine two\nLine three", (blocks[0] as PreviewBlock.Paragraph).text)
    }

    @Test
    fun parsePreviewBlocks_blankLine_separatesParagraphs() {
        val input = "First paragraph\n\nSecond paragraph"

        val blocks = parsePreviewBlocks(input, isOrg = false)

        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is PreviewBlock.Paragraph)
        assertEquals("First paragraph", (blocks[0] as PreviewBlock.Paragraph).text)
        assertTrue(blocks[1] is PreviewBlock.Empty)
        assertTrue(blocks[2] is PreviewBlock.Paragraph)
        assertEquals("Second paragraph", (blocks[2] as PreviewBlock.Paragraph).text)
    }

    @Test
    fun parsePreviewBlocks_mixedContent() {
        val input = """
            # Titulo
            > citacao
            ---
            paragrafo normal
        """.trimIndent()

        val blocks = parsePreviewBlocks(input, isOrg = false)

        assertEquals(4, blocks.size)
        assertTrue(blocks[0] is PreviewBlock.Heading)
        assertTrue(blocks[1] is PreviewBlock.Blockquote)
        assertTrue(blocks[2] is PreviewBlock.HorizontalRule)
        assertTrue(blocks[3] is PreviewBlock.Paragraph)
    }
}
