package com.offlinenotes.ui.editor

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

private sealed interface PreviewBlock {
    data class Heading(val level: Int, val text: String) : PreviewBlock
    data class Checklist(val checked: Boolean, val text: String, val indentLevel: Int) : PreviewBlock
    data class Bullet(val text: String, val indentLevel: Int) : PreviewBlock
    data class Paragraph(val text: String) : PreviewBlock
    data object Empty : PreviewBlock
}

@Composable
fun NotePreviewContent(
    text: String,
    isOrg: Boolean,
    modifier: Modifier = Modifier
) {
    val blocks = parsePreviewBlocks(text, isOrg)
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp)
    ) {
        itemsIndexed(blocks) { _, block ->
            when (block) {
                is PreviewBlock.Heading -> {
                    val headingStyle = when (block.level) {
                        1 -> MaterialTheme.typography.titleLarge
                        2 -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.titleSmall
                    }
                    PreviewText(
                        text = buildInlineStyledText(
                            text = block.text,
                            isOrg = isOrg,
                            linkColor = MaterialTheme.colorScheme.primary,
                            codeBackground = MaterialTheme.colorScheme.surfaceVariant,
                            codeTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        style = headingStyle,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)
                    )
                }

                is PreviewBlock.Checklist -> {
                    androidx.compose.foundation.layout.Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.padding(
                            start = (block.indentLevel * 12).dp,
                            top = 2.dp,
                            bottom = 2.dp
                        )
                    ) {
                        Icon(
                            imageVector = if (block.checked) {
                                Icons.Default.CheckBox
                            } else {
                                Icons.Default.CheckBoxOutlineBlank
                            },
                            contentDescription = null,
                            tint = if (block.checked) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(top = 1.dp)
                        )
                        PreviewText(
                            text = buildInlineStyledText(
                                text = block.text,
                                isOrg = isOrg,
                                linkColor = MaterialTheme.colorScheme.primary,
                                codeBackground = MaterialTheme.colorScheme.surfaceVariant,
                                codeTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                is PreviewBlock.Bullet -> {
                    PreviewText(
                        text = buildInlineStyledText(
                            text = "- ${block.text}",
                            isOrg = isOrg,
                            linkColor = MaterialTheme.colorScheme.primary,
                            codeBackground = MaterialTheme.colorScheme.surfaceVariant,
                            codeTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(
                            start = (block.indentLevel * 12).dp,
                            top = 2.dp,
                            bottom = 2.dp
                        )
                    )
                }

                is PreviewBlock.Paragraph -> {
                    PreviewText(
                        text = buildInlineStyledText(
                            text = block.text,
                            isOrg = isOrg,
                            linkColor = MaterialTheme.colorScheme.primary,
                            codeBackground = MaterialTheme.colorScheme.surfaceVariant,
                            codeTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                PreviewBlock.Empty -> {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun PreviewText(
    text: AnnotatedString,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = style,
        color = color,
        modifier = modifier
    )
}

private fun parsePreviewBlocks(text: String, isOrg: Boolean): List<PreviewBlock> {
    if (text.isBlank()) {
        return listOf(PreviewBlock.Paragraph("Nota vazia"))
    }

    val headingPattern = if (isOrg) {
        Regex("^(\\*{1,6})\\s+(.+)$")
    } else {
        Regex("^(#{1,6})\\s+(.+)$")
    }
    val checklistPattern = Regex("^(\\s*)-\\s+\\[([ xX])\\]\\s+(.+)$")
    val bulletPattern = Regex("^(\\s*)-\\s+(.+)$")

    return text.lines().map { rawLine ->
        val line = rawLine.trimEnd()
        when {
            line.isBlank() -> PreviewBlock.Empty
            headingPattern.matches(line) -> {
                val match = headingPattern.find(line)!!
                val level = match.groupValues[1].length
                val content = match.groupValues[2].trim()
                PreviewBlock.Heading(level = level, text = content)
            }

            checklistPattern.matches(line) -> {
                val match = checklistPattern.find(line)!!
                val indent = match.groupValues[1].length / 2
                val checked = match.groupValues[2].equals("x", ignoreCase = true)
                val content = match.groupValues[3].trim()
                PreviewBlock.Checklist(checked = checked, text = content, indentLevel = indent)
            }

            bulletPattern.matches(line) -> {
                val match = bulletPattern.find(line)!!
                val indent = match.groupValues[1].length / 2
                PreviewBlock.Bullet(text = match.groupValues[2].trim(), indentLevel = indent)
            }

            else -> PreviewBlock.Paragraph(text = line)
        }
    }
}

private fun buildInlineStyledText(
    text: String,
    isOrg: Boolean,
    linkColor: Color,
    codeBackground: Color,
    codeTextColor: Color
): AnnotatedString {
    val builder = AnnotatedString.Builder()
    var index = 0

    while (index < text.length) {
        val nextLink = findNextLinkToken(text, index, isOrg)
        if (nextLink == null) {
            appendStyledSegment(builder, text.substring(index), isOrg, codeBackground, codeTextColor)
            break
        }

        if (nextLink.start > index) {
            appendStyledSegment(
                builder,
                text.substring(index, nextLink.start),
                isOrg,
                codeBackground,
                codeTextColor
            )
        }

        builder.pushLink(
            LinkAnnotation.Url(
                nextLink.url,
                styles = TextLinkStyles(
                    style = SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline
                    )
                )
            )
        )
        builder.append(nextLink.label)
        builder.pop()

        index = nextLink.endExclusive
    }

    return builder.toAnnotatedString()
}

private fun appendStyledSegment(
    builder: AnnotatedString.Builder,
    segment: String,
    isOrg: Boolean,
    codeBackground: Color,
    codeTextColor: Color
) {
    val boldDelimiter = if (isOrg) "*" else "**"
    val italicDelimiter = if (isOrg) "/" else "_"
    val markdownCodeDelimiter = if (isOrg) null else "`"
    val orgCodeDelimiterA = if (isOrg) "=" else null
    val orgCodeDelimiterB = if (isOrg) "~" else null
    var index = 0

    while (index < segment.length) {
        val boldRange = findDelimitedRange(segment, boldDelimiter, index, isOrg)
        val italicRange = findDelimitedRange(segment, italicDelimiter, index, isOrg)
        val markdownCodeRange = markdownCodeDelimiter?.let {
            findDelimitedRange(segment, it, index, isOrg)
        }
        val orgCodeRangeA = orgCodeDelimiterA?.let {
            findDelimitedRange(segment, it, index, isOrg)
        }
        val orgCodeRangeB = orgCodeDelimiterB?.let {
            findDelimitedRange(segment, it, index, isOrg)
        }

        val allRanges = listOfNotNull(
            boldRange,
            italicRange,
            markdownCodeRange,
            orgCodeRangeA,
            orgCodeRangeB
        )
        val next = allRanges.minByOrNull { it.first }

        if (next == null) {
            builder.append(segment.substring(index))
            break
        }

        if (next.first > index) {
            builder.append(segment.substring(index, next.first))
        }

        val inner = segment.substring(next.first + next.third.length, next.second)
        val spanStyle = when (next.third) {
            boldDelimiter -> SpanStyle(fontWeight = FontWeight.SemiBold)
            italicDelimiter -> SpanStyle(fontStyle = FontStyle.Italic)
            else -> SpanStyle(
                fontFamily = FontFamily.Monospace,
                background = codeBackground,
                color = codeTextColor
            )
        }
        builder.withStyle(spanStyle) { append(inner) }
        index = next.second + next.third.length
    }
}

private fun findDelimitedRange(
    text: String,
    delimiter: String,
    startIndex: Int,
    isOrg: Boolean
): Triple<Int, Int, String>? {
    if (delimiter.isBlank()) return null

    var open = text.indexOf(delimiter, startIndex)
    while (open >= 0) {
        if (!isOpeningDelimiterValid(text, delimiter, open, isOrg)) {
            open = text.indexOf(delimiter, open + delimiter.length)
            continue
        }

        var close = text.indexOf(delimiter, open + delimiter.length)
        while (close >= 0) {
            if (close == open + delimiter.length) {
                close = text.indexOf(delimiter, close + delimiter.length)
                continue
            }

            if (isClosingDelimiterValid(text, delimiter, close, isOrg)) {
                return Triple(open, close, delimiter)
            }
            close = text.indexOf(delimiter, close + delimiter.length)
        }

        open = text.indexOf(delimiter, open + delimiter.length)
    }

    return null
}

private fun isOpeningDelimiterValid(text: String, delimiter: String, index: Int, isOrg: Boolean): Boolean {
    val afterIndex = index + delimiter.length
    val before = text.getOrNull(index - 1)
    val after = text.getOrNull(afterIndex) ?: return false

    if (after.isWhitespace()) return false

    return when {
        !isOrg && delimiter == "_" -> {
            (before == null || !before.isLetterOrDigit()) && after.isLetterOrDigit()
        }

        isOrg -> {
            before == null || before.isWhitespace() || isDelimiterBoundary(before)
        }

        else -> true
    }
}

private fun isClosingDelimiterValid(text: String, delimiter: String, index: Int, isOrg: Boolean): Boolean {
    val before = text.getOrNull(index - 1) ?: return false
    val after = text.getOrNull(index + delimiter.length)

    if (before.isWhitespace()) return false

    return when {
        !isOrg && delimiter == "_" -> {
            before.isLetterOrDigit() && (after == null || !after.isLetterOrDigit())
        }

        isOrg -> {
            after == null || after.isWhitespace() || isDelimiterBoundary(after)
        }

        else -> true
    }
}

private fun isDelimiterBoundary(char: Char): Boolean {
    return !char.isLetterOrDigit()
}

private data class LinkToken(
    val start: Int,
    val endExclusive: Int,
    val label: String,
    val url: String
)

private fun findNextLinkToken(text: String, startIndex: Int, isOrg: Boolean): LinkToken? {
    val markdownMatch = MARKDOWN_LINK.find(text, startIndex)
    val orgMatch = if (isOrg) ORG_LINK.find(text, startIndex) else null
    val bareMatch = BARE_URL.find(text, startIndex)

    val matches = listOfNotNull(
        markdownMatch?.let {
            LinkToken(
                start = it.range.first,
                endExclusive = it.range.last + 1,
                label = it.groupValues[1],
                url = it.groupValues[2]
            )
        },
        orgMatch?.let {
            LinkToken(
                start = it.range.first,
                endExclusive = it.range.last + 1,
                label = it.groupValues[2],
                url = it.groupValues[1]
            )
        },
        bareMatch?.let {
            val raw = it.value.trimEnd('.', ',', ';', ':', ')', ']', '}')
            val url = if (raw.startsWith("www.", ignoreCase = true)) {
                "https://$raw"
            } else {
                raw
            }
            val end = it.range.first + raw.length
            LinkToken(
                start = it.range.first,
                endExclusive = end,
                label = raw,
                url = url
            )
        }
    )

    return matches.minByOrNull { it.start }
}

private val MARKDOWN_LINK = Regex("\\[([^\\]]+)]\\(((?:https?://|mailto:|tel:)[^)\\s]+)\\)")
private val ORG_LINK = Regex("\\[\\[((?:https?://|mailto:|tel:)[^]\\s]+)]\\[([^\\]]+)]]")
private val BARE_URL = Regex("(?:https?://|mailto:|tel:|www\\.)[^\\s]+")
