package com.offlinenotes.ui.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class SyntaxHighlightingTransformation(
    private val isOrg: Boolean,
    private val listMarkerColor: Color,
    private val codeTextColor: Color,
    private val codeDelimiterColor: Color
) : VisualTransformation {

    // List prefixes
    private val markdownListPrefixRegex =
        Regex("^\\s*(?:[-*+]\\s(?:\\[[ xX]\\]\\s)?|\\d+\\.\\s)", RegexOption.MULTILINE)
    private val orgListPrefixRegex =
        Regex("^\\s*(?:[-+]\\s|\\d+[.)]\\s)", RegexOption.MULTILINE)

    // Code block delimiters
    private val markdownCodeDelimiterRegex = Regex("^\\s*```.*$", RegexOption.MULTILINE)
    private val orgCodeDelimiterRegex = Regex(
        "^\\s*#\\+(begin_src|end_src)\\b.*$",
        setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE)
    )

    // Headings — group 1 = marker chars, rest = title text
    private val markdownHeadingRegex = Regex("^(#{1,6})([ \\t]+.+)$", RegexOption.MULTILINE)
    private val orgHeadingRegex = Regex("^(\\*{1,6})([ \\t]+.+)$", RegexOption.MULTILINE)

    // Blockquotes
    private val blockquoteRegex = Regex("^>.*$", RegexOption.MULTILINE)

    // Inline code
    private val markdownInlineCodeRegex = Regex("`[^`\\n]+`")
    private val orgInlineCodeRegex = Regex("(?:=[^=\\n]{1,80}=|~[^~\\n]{1,80}~)")

    // Bold — Markdown `**text**` only (conservative; avoids Org heading conflicts)
    private val markdownBoldRegex = Regex("\\*\\*[^*\\n]+\\*\\*")

    override fun filter(text: AnnotatedString): TransformedText {
        val highlighted = buildAnnotatedString {
            append(text)
            applySyntaxStyles(text.text)
        }
        return TransformedText(highlighted, OffsetMapping.Identity)
    }

    private fun AnnotatedString.Builder.applySyntaxStyles(content: String) {
        if (content.isEmpty()) return

        val blocks = if (isOrg) collectOrgCodeBlocks(content) else collectMarkdownCodeBlocks(content)
        val exclusions = mergeRanges(blocks.delimiterRanges + blocks.contentRanges)

        // Code delimiters and content (applied first; everything else excludes these ranges)
        blocks.delimiterRanges.forEach { r -> addStyle(codeDelimiterStyle(), r.start, r.endExclusive) }
        blocks.contentRanges.forEach { r -> addStyle(codeStyle(), r.start, r.endExclusive) }

        // Headings
        val headingRegex = if (isOrg) orgHeadingRegex else markdownHeadingRegex
        headingRegex.findAll(content).forEach { match ->
            val s = match.range.first
            val e = match.range.last + 1
            if (!isExcluded(s, e, exclusions)) {
                val markerLen = match.groupValues[1].length
                // Dim the marker chars
                addStyle(SpanStyle(color = codeDelimiterColor), s, s + markerLen)
                // Primary color + weight for the title
                val textStart = (s + markerLen).coerceAtMost(e)
                if (textStart < e) addStyle(headingStyle(markerLen), textStart, e)
            }
        }

        // Blockquotes — dim the `>`, soften the quoted text
        blockquoteRegex.findAll(content).forEach { match ->
            val s = match.range.first
            val e = match.range.last + 1
            if (!isExcluded(s, e, exclusions)) {
                addStyle(SpanStyle(color = codeDelimiterColor), s, minOf(s + 1, e))
                val textStart = minOf(s + 1, e)
                if (textStart < e) addStyle(SpanStyle(color = codeTextColor), textStart, e)
            }
        }

        // Inline code
        val inlineCodeR = if (isOrg) orgInlineCodeRegex else markdownInlineCodeRegex
        inlineCodeR.findAll(content).forEach { match ->
            val s = match.range.first
            val e = match.range.last + 1
            if (!isExcluded(s, e, exclusions)) addStyle(codeStyle(), s, e)
        }

        // Markdown bold
        if (!isOrg) {
            markdownBoldRegex.findAll(content).forEach { match ->
                val s = match.range.first
                val e = match.range.last + 1
                if (!isExcluded(s, e, exclusions)) {
                    addStyle(SpanStyle(fontWeight = FontWeight.Bold), s, e)
                }
            }
        }

        // List markers
        val listRegex = if (isOrg) orgListPrefixRegex else markdownListPrefixRegex
        listRegex.findAll(content).forEach { match ->
            val s = match.range.first
            val e = match.range.last + 1
            if (!isExcluded(s, e, exclusions)) {
                addStyle(SpanStyle(color = listMarkerColor), s, e)
            }
        }
    }

    private fun isExcluded(start: Int, end: Int, exclusions: List<TextRange>): Boolean =
        exclusions.any { it.start < end && start < it.endExclusive }

    private fun headingStyle(level: Int): SpanStyle {
        val weight = when (level) {
            1 -> FontWeight.Bold
            2 -> FontWeight.SemiBold
            else -> FontWeight.Medium
        }
        return SpanStyle(color = listMarkerColor, fontWeight = weight)
    }

    private fun collectMarkdownCodeBlocks(content: String): CodeBlockRanges {
        val delimiters = markdownCodeDelimiterRegex.findAll(content)
            .map { match -> TextRange(match.range.first, match.range.last + 1) }
            .toList()

        val contentRanges = mutableListOf<TextRange>()
        var openDelimiter: TextRange? = null
        delimiters.forEach { delimiter ->
            val currentOpen = openDelimiter
            if (currentOpen == null) {
                openDelimiter = delimiter
                return@forEach
            }
            val start = rangeAfterLineBreak(content, currentOpen.endExclusive)
            val end = rangeBeforeLineBreak(content, delimiter.start)
            if (start < end) contentRanges.add(TextRange(start, end))
            openDelimiter = null
        }

        return CodeBlockRanges(delimiterRanges = delimiters, contentRanges = contentRanges)
    }

    private fun collectOrgCodeBlocks(content: String): CodeBlockRanges {
        val delimiters = mutableListOf<TextRange>()
        val contentRanges = mutableListOf<TextRange>()
        var openDelimiter: TextRange? = null

        orgCodeDelimiterRegex.findAll(content).forEach { match ->
            val delimiterRange = TextRange(match.range.first, match.range.last + 1)
            delimiters.add(delimiterRange)

            val type = match.groupValues[1].lowercase()
            if (type == "begin_src") {
                if (openDelimiter == null) openDelimiter = delimiterRange
                return@forEach
            }

            val currentOpen = openDelimiter
            if (currentOpen != null) {
                val start = rangeAfterLineBreak(content, currentOpen.endExclusive)
                val end = rangeBeforeLineBreak(content, delimiterRange.start)
                if (start < end) contentRanges.add(TextRange(start, end))
                openDelimiter = null
            }
        }

        return CodeBlockRanges(delimiterRanges = delimiters, contentRanges = contentRanges)
    }

    private fun mergeRanges(ranges: List<TextRange>): List<TextRange> {
        if (ranges.isEmpty()) return emptyList()
        val sorted = ranges.sortedBy { it.start }
        val merged = mutableListOf<TextRange>()
        var current = sorted.first()
        for (i in 1 until sorted.size) {
            val next = sorted[i]
            current = if (next.start <= current.endExclusive) {
                TextRange(current.start, maxOf(current.endExclusive, next.endExclusive))
            } else {
                merged.add(current); next
            }
        }
        merged.add(current)
        return merged
    }

    private fun rangeAfterLineBreak(content: String, index: Int): Int =
        if (index < content.length && content[index] == '\n') index + 1 else index

    private fun rangeBeforeLineBreak(content: String, index: Int): Int {
        if (index <= 0) return index
        return if (content[index - 1] == '\n') index - 1 else index
    }

    private fun codeStyle() = SpanStyle(color = codeTextColor, fontFamily = FontFamily.Monospace)
    private fun codeDelimiterStyle() = SpanStyle(color = codeDelimiterColor, fontFamily = FontFamily.Monospace)

    private data class CodeBlockRanges(
        val delimiterRanges: List<TextRange>,
        val contentRanges: List<TextRange>
    )

    private data class TextRange(val start: Int, val endExclusive: Int)
}
