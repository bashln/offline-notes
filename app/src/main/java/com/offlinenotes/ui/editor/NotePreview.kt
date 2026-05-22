package com.offlinenotes.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.offlinenotes.ui.theme.ObsidianiteBackground
import com.offlinenotes.ui.theme.ObsidianiteBorder
import com.offlinenotes.ui.theme.ObsidianiteInteractiveAccent
import com.offlinenotes.ui.theme.ObsidianiteSurface
import com.offlinenotes.ui.theme.ObsidianiteTextAccent
import com.offlinenotes.ui.theme.ObsidianiteTextDim
import com.offlinenotes.ui.theme.ObsidianiteTextFaint
import com.offlinenotes.ui.theme.ObsidianiteTextLink
import com.offlinenotes.ui.theme.ObsidianiteTextNormal
import com.offlinenotes.ui.theme.ObsidianiteTextSubAccent
import com.offlinenotes.ui.theme.ThemePalette

@Composable
fun NotePreviewContent(
    text: String,
    isOrg: Boolean,
    palette: ThemePalette = ThemePalette.TokyoNight,
    modifier: Modifier = Modifier
) {
    val blocks = parsePreviewBlocks(text, isOrg)
    val isObsidianite = palette == ThemePalette.Obsidianite && isOrg

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp)
    ) {
        itemsIndexed(blocks) { _, block ->
            when (block) {
                is PreviewBlock.Heading -> {
                    if (isObsidianite) {
                        ObsidianiteHeading(text = block.text, level = block.level, isOrg = isOrg, palette = palette)
                    } else {
                        StandardHeading(text = block.text, level = block.level, isOrg = isOrg, palette = palette)
                    }
                }

                is PreviewBlock.Checklist -> {
                    if (isObsidianite) {
                        ObsidianiteChecklist(block = block, isOrg = isOrg, palette = palette)
                    } else {
                        StandardChecklist(block = block, isOrg = isOrg, palette = palette)
                    }
                }

                is PreviewBlock.Bullet -> {
                    if (isObsidianite) {
                        ObsidianiteBullet(block = block, isOrg = isOrg, palette = palette)
                    } else {
                        StandardBullet(block = block, isOrg = isOrg, palette = palette)
                    }
                }

                is PreviewBlock.NumberedBullet -> {
                    if (isObsidianite) {
                        ObsidianiteNumberedBullet(block = block, isOrg = isOrg, palette = palette)
                    } else {
                        StandardNumberedBullet(block = block, isOrg = isOrg, palette = palette)
                    }
                }

                is PreviewBlock.Blockquote -> {
                    if (isObsidianite) {
                        ObsidianiteBlockquote(block = block, isOrg = isOrg, palette = palette)
                    } else {
                        StandardBlockquote(block = block, isOrg = isOrg, palette = palette)
                    }
                }

                PreviewBlock.HorizontalRule -> {
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        color = if (isObsidianite) ObsidianiteBorder else MaterialTheme.colorScheme.outlineVariant
                    )
                }

                is PreviewBlock.CodeBlock -> {
                    if (isObsidianite) {
                        ObsidianiteCodeBlock(block = block)
                    } else {
                        StandardCodeBlock(block = block)
                    }
                }

                is PreviewBlock.Paragraph -> {
                    if (isObsidianite) {
                        ObsidianiteParagraph(text = block.text, isOrg = isOrg, palette = palette)
                    } else {
                        StandardParagraph(text = block.text, isOrg = isOrg, palette = palette)
                    }
                }

                PreviewBlock.Empty -> {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun StandardHeading(text: String, level: Int, isOrg: Boolean, palette: ThemePalette) {
    val headingStyle = when (level) {
        1 -> MaterialTheme.typography.titleLarge
        2 -> MaterialTheme.typography.titleMedium
        else -> MaterialTheme.typography.titleSmall
    }
    PreviewText(
        text = buildInlineStyledText(
            text = text,
            isOrg = isOrg,
            palette = palette,
            linkColor = MaterialTheme.colorScheme.primary,
            codeBackground = MaterialTheme.colorScheme.surfaceVariant,
            codeTextColor = MaterialTheme.colorScheme.onSurface
        ),
        style = headingStyle,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)
    )
}

@Composable
private fun ObsidianiteHeading(text: String, level: Int, isOrg: Boolean, palette: ThemePalette) {
    val headingStyle = when (level) {
        1 -> MaterialTheme.typography.titleLarge
        2 -> MaterialTheme.typography.titleMedium
        else -> MaterialTheme.typography.titleSmall
    }
    val headingColor = when (level) {
        1 -> ObsidianiteTextAccent
        else -> ObsidianiteTextFaint
    }

    if (level == 1) {
        Row(
            modifier = Modifier
                .padding(top = 10.dp, bottom = 6.dp)
                .height(30.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(3.dp)
                    .background(ObsidianiteTextAccent)
            )
            PreviewText(
                text = buildInlineStyledText(
                    text = text,
                    isOrg = isOrg,
                    palette = palette,
                    linkColor = ObsidianiteTextLink,
                    codeBackground = ObsidianiteSurface,
                    codeTextColor = ObsidianiteTextNormal
                ),
                style = headingStyle,
                color = headingColor,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    } else {
        PreviewText(
            text = buildInlineStyledText(
                text = text,
                isOrg = isOrg,
                palette = palette,
                linkColor = ObsidianiteTextLink,
                codeBackground = ObsidianiteSurface,
                codeTextColor = ObsidianiteTextNormal
            ),
            style = headingStyle,
            color = headingColor,
            modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)
        )
    }
}

@Composable
private fun StandardChecklist(block: PreviewBlock.Checklist, isOrg: Boolean, palette: ThemePalette) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        modifier = Modifier.padding(
            start = (block.indentLevel * 12).dp,
            top = 2.dp,
            bottom = 2.dp
        )
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
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
                    palette = palette,
                    linkColor = MaterialTheme.colorScheme.primary,
                    codeBackground = MaterialTheme.colorScheme.surfaceVariant,
                    codeTextColor = MaterialTheme.colorScheme.onSurface
                ),
                style = MaterialTheme.typography.bodyLarge.copy(
                    textDecoration = if (block.checked) {
                        TextDecoration.LineThrough
                    } else {
                        TextDecoration.None
                    }
                ),
                color = if (block.checked) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun ObsidianiteChecklist(block: PreviewBlock.Checklist, isOrg: Boolean, palette: ThemePalette) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .padding(start = (block.indentLevel * 12).dp, top = 4.dp, bottom = 4.dp)
            .background(ObsidianiteBackground)
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = if (block.checked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
            contentDescription = null,
            tint = if (block.checked) ObsidianiteTextAccent else ObsidianiteInteractiveAccent,
            modifier = Modifier
                .padding(top = 1.dp)
                .border(
                    border = BorderStroke(
                        width = if (block.checked) 0.dp else 1.dp,
                        color = ObsidianiteBorder.copy(alpha = 0.7f)
                    )
                )
        )
        PreviewText(
            text = buildInlineStyledText(
                text = block.text,
                isOrg = isOrg,
                palette = palette,
                linkColor = ObsidianiteTextLink,
                codeBackground = ObsidianiteSurface,
                codeTextColor = ObsidianiteTextNormal
            ),
            style = MaterialTheme.typography.bodyLarge.copy(
                textDecoration = if (block.checked) TextDecoration.LineThrough else TextDecoration.None
            ),
            color = if (block.checked) ObsidianiteTextFaint else ObsidianiteTextNormal,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun StandardBullet(block: PreviewBlock.Bullet, isOrg: Boolean, palette: ThemePalette) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        modifier = Modifier.padding(
            start = (block.indentLevel * 12).dp,
            top = 2.dp,
            bottom = 2.dp
        )
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text(
                text = "•",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
            PreviewText(
                text = buildInlineStyledText(
                    text = block.text,
                    isOrg = isOrg,
                    palette = palette,
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
}

@Composable
private fun StandardNumberedBullet(block: PreviewBlock.NumberedBullet, isOrg: Boolean, palette: ThemePalette) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.padding(
            start = (block.indentLevel * 12).dp,
            top = 3.dp,
            bottom = 3.dp
        )
    ) {
        Text(
            text = block.number,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(28.dp)
        )
        PreviewText(
            text = buildInlineStyledText(
                text = block.text,
                isOrg = isOrg,
                palette = palette,
                linkColor = MaterialTheme.colorScheme.primary,
                codeBackground = MaterialTheme.colorScheme.surfaceVariant,
                codeTextColor = MaterialTheme.colorScheme.onSurface
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ObsidianiteNumberedBullet(block: PreviewBlock.NumberedBullet, isOrg: Boolean, palette: ThemePalette) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.padding(start = (block.indentLevel * 12).dp, top = 4.dp, bottom = 4.dp)
    ) {
        Text(
            text = block.number,
            style = MaterialTheme.typography.bodyLarge,
            color = ObsidianiteTextDim,
            modifier = Modifier.width(28.dp)
        )
        PreviewText(
            text = buildInlineStyledText(
                text = block.text,
                isOrg = isOrg,
                palette = palette,
                linkColor = ObsidianiteTextLink,
                codeBackground = ObsidianiteSurface,
                codeTextColor = ObsidianiteTextNormal
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = ObsidianiteTextNormal
        )
    }
}

@Composable
private fun ObsidianiteBullet(block: PreviewBlock.Bullet, isOrg: Boolean, palette: ThemePalette) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.padding(start = (block.indentLevel * 12).dp, top = 4.dp, bottom = 4.dp)
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyLarge,
            color = ObsidianiteTextDim
        )
        PreviewText(
            text = buildInlineStyledText(
                text = block.text,
                isOrg = isOrg,
                palette = palette,
                linkColor = ObsidianiteTextLink,
                codeBackground = ObsidianiteSurface,
                codeTextColor = ObsidianiteTextNormal
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = ObsidianiteTextNormal,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun StandardBlockquote(block: PreviewBlock.Blockquote, isOrg: Boolean, palette: ThemePalette) {
    Row(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
        )
        PreviewText(
            text = buildInlineStyledText(
                text = block.text,
                isOrg = isOrg,
                palette = palette,
                linkColor = MaterialTheme.colorScheme.primary,
                codeBackground = MaterialTheme.colorScheme.surfaceVariant,
                codeTextColor = MaterialTheme.colorScheme.onSurface
            ),
            style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
        )
    }
}

@Composable
private fun ObsidianiteBlockquote(block: PreviewBlock.Blockquote, isOrg: Boolean, palette: ThemePalette) {
    Row(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(ObsidianiteSurface.copy(alpha = 0.5f))
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(ObsidianiteTextAccent)
        )
        PreviewText(
            text = buildInlineStyledText(
                text = block.text,
                isOrg = isOrg,
                palette = palette,
                linkColor = ObsidianiteTextLink,
                codeBackground = ObsidianiteSurface,
                codeTextColor = ObsidianiteTextNormal
            ),
            style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
            color = ObsidianiteTextDim,
            modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
        )
    }
}

@Composable
private fun StandardCodeBlock(block: PreviewBlock.CodeBlock) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        if (!block.languageHint.isNullOrBlank()) {
            Text(
                text = block.languageHint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 10.dp, top = 8.dp, end = 10.dp)
            )
        }
        Text(
            text = block.content.ifBlank { " " },
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun ObsidianiteCodeBlock(block: PreviewBlock.CodeBlock) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ObsidianiteSurface),
        border = BorderStroke(width = 1.dp, color = ObsidianiteBorder),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        if (!block.languageHint.isNullOrBlank()) {
            Text(
                text = block.languageHint,
                style = MaterialTheme.typography.labelSmall,
                color = ObsidianiteTextSubAccent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 10.dp, top = 8.dp, end = 10.dp)
            )
        }
        Text(
            text = block.content.ifBlank { " " },
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            color = ObsidianiteTextNormal,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun StandardParagraph(text: String, isOrg: Boolean, palette: ThemePalette) {
    PreviewText(
        text = buildInlineStyledText(
            text = text,
            isOrg = isOrg,
            palette = palette,
            linkColor = MaterialTheme.colorScheme.primary,
            codeBackground = MaterialTheme.colorScheme.surfaceVariant,
            codeTextColor = MaterialTheme.colorScheme.onSurface
        ),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

@Composable
private fun ObsidianiteParagraph(text: String, isOrg: Boolean, palette: ThemePalette) {
    PreviewText(
        text = buildInlineStyledText(
            text = text,
            isOrg = isOrg,
            palette = palette,
            linkColor = ObsidianiteTextLink,
            codeBackground = ObsidianiteSurface,
            codeTextColor = ObsidianiteTextNormal
        ),
        style = MaterialTheme.typography.bodyLarge,
        color = ObsidianiteTextNormal,
        modifier = Modifier.padding(vertical = 2.dp)
    )
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

private fun buildInlineStyledText(
    text: String,
    isOrg: Boolean,
    palette: ThemePalette,
    linkColor: Color,
    codeBackground: Color,
    codeTextColor: Color
): AnnotatedString {
    val useObsidianitePalette = palette == ThemePalette.Obsidianite && isOrg
    val resolvedLinkColor = if (useObsidianitePalette) ObsidianiteTextLink else linkColor
    val resolvedCodeBackground = if (useObsidianitePalette) ObsidianiteSurface else codeBackground
    val resolvedCodeTextColor = if (useObsidianitePalette) ObsidianiteTextNormal else codeTextColor
    val builder = AnnotatedString.Builder()
    var index = 0

    while (index < text.length) {
        val nextLink = findNextLinkToken(text, index, isOrg)
        if (nextLink == null) {
            appendStyledSegment(
                builder,
                text.substring(index),
                isOrg,
                resolvedCodeBackground,
                resolvedCodeTextColor
            )
            break
        }

        if (nextLink.start > index) {
            appendStyledSegment(
                builder,
                text.substring(index, nextLink.start),
                isOrg,
                resolvedCodeBackground,
                resolvedCodeTextColor
            )
        }

        builder.pushLink(
            LinkAnnotation.Url(
                nextLink.url,
                styles = TextLinkStyles(
                    style = SpanStyle(
                        color = resolvedLinkColor,
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
