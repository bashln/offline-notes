package com.offlinenotes.ui.editor

internal sealed interface PreviewBlock {
    data class Heading(val level: Int, val text: String) : PreviewBlock
    data class Checklist(
        val checked: Boolean,
        val text: String,
        val indentLevel: Int
    ) : PreviewBlock
    data class Bullet(
        val text: String,
        val indentLevel: Int
    ) : PreviewBlock
    data class NumberedBullet(
        val number: String,
        val text: String,
        val indentLevel: Int
    ) : PreviewBlock
    data class CodeBlock(
        val languageHint: String?,
        val content: String
    ) : PreviewBlock
    data class Blockquote(val text: String) : PreviewBlock
    data object HorizontalRule : PreviewBlock
    data class Paragraph(val text: String) : PreviewBlock
    data object Empty : PreviewBlock
}

internal fun parsePreviewBlocks(text: String, isOrg: Boolean): List<PreviewBlock> {
    if (text.isBlank()) {
        return listOf(PreviewBlock.Paragraph("Nota vazia"))
    }

    val headingPattern = if (isOrg) Regex("^(\\*{1,6})\\s+(.+)$") else Regex("^(#{1,6})\\s+(.+)$")
    val blockquotePattern = Regex("^>\\s?(.*)")
    val horizontalRulePatternMd = Regex("^\\s*[-*_]{3,}\\s*$")
    val horizontalRulePatternOrg = Regex("^\\s*-{5,}\\s*$")
    val checklistPattern = Regex("^(\\s*)[-+*]\\s+\\[([ xX])]\\s+(.+)$")
    val orderedListPattern = Regex("^(\\s*)(\\d+[.)])\\s+(.+)$")
    val bulletPattern = Regex("^(\\s*)[-+*]\\s+(.+)$")
    val orgCodeStart = Regex("^\\s*#\\+begin_src(?:\\s+(\\S+))?.*$", RegexOption.IGNORE_CASE)
    val orgCodeEnd = Regex("^\\s*#\\+end_src\\s*$", RegexOption.IGNORE_CASE)

    val lines = text.lines()
    val blocks = mutableListOf<PreviewBlock>()
    val paragraphAccum = mutableListOf<String>()
    var index = 0

    fun flushParagraph() {
        if (paragraphAccum.isNotEmpty()) {
            blocks += PreviewBlock.Paragraph(paragraphAccum.joinToString("\n"))
            paragraphAccum.clear()
        }
    }

    while (index < lines.size) {
        val line = lines[index].trimEnd()

        // Fenced code blocks (consume multiple lines)
        val mdCodeStart = if (!isOrg) line.trimStart().takeIf { it.startsWith("```") } else null
        if (mdCodeStart != null) {
            flushParagraph()
            val language = mdCodeStart.removePrefix("```").trim().ifBlank { null }
            val contentLines = mutableListOf<String>()
            index++
            while (index < lines.size && !lines[index].trimStart().startsWith("```")) {
                contentLines += lines[index]
                index++
            }
            if (index < lines.size) index++
            blocks += PreviewBlock.CodeBlock(languageHint = language, content = contentLines.joinToString("\n"))
            continue
        }

        val orgCodeMatch = if (isOrg) orgCodeStart.find(line) else null
        if (orgCodeMatch != null) {
            flushParagraph()
            val language = orgCodeMatch.groupValues.getOrNull(1)?.ifBlank { null }
            val contentLines = mutableListOf<String>()
            index++
            while (index < lines.size && !orgCodeEnd.matches(lines[index].trim())) {
                contentLines += lines[index]
                index++
            }
            if (index < lines.size) index++
            blocks += PreviewBlock.CodeBlock(languageHint = language, content = contentLines.joinToString("\n"))
            continue
        }

        when {
            line.isBlank() -> {
                flushParagraph()
                blocks += PreviewBlock.Empty
                index++
            }

            headingPattern.matches(line) -> {
                flushParagraph()
                val m = headingPattern.find(line)!!
                blocks += PreviewBlock.Heading(level = m.groupValues[1].length, text = m.groupValues[2].trim())
                index++
            }

            (!isOrg && horizontalRulePatternMd.matches(line)) ||
                    (isOrg && horizontalRulePatternOrg.matches(line)) -> {
                flushParagraph()
                blocks += PreviewBlock.HorizontalRule
                index++
            }

            blockquotePattern.matches(line) -> {
                flushParagraph()
                val quoteLines = mutableListOf<String>()
                while (index < lines.size) {
                    val m = blockquotePattern.find(lines[index].trimEnd())
                    if (m != null) { quoteLines += m.groupValues[1]; index++ } else break
                }
                blocks += PreviewBlock.Blockquote(text = quoteLines.joinToString("\n"))
                continue
            }

            checklistPattern.matches(line) -> {
                flushParagraph()
                val m = checklistPattern.find(line)!!
                blocks += PreviewBlock.Checklist(
                    checked = m.groupValues[2].equals("x", ignoreCase = true),
                    text = m.groupValues[3].trim(),
                    indentLevel = toIndentLevel(m.groupValues[1])
                )
                index++
            }

            orderedListPattern.matches(line) -> {
                flushParagraph()
                val m = orderedListPattern.find(line)!!
                blocks += PreviewBlock.NumberedBullet(
                    number = m.groupValues[2],
                    text = m.groupValues[3].trim(),
                    indentLevel = toIndentLevel(m.groupValues[1])
                )
                index++
            }

            bulletPattern.matches(line) -> {
                flushParagraph()
                val m = bulletPattern.find(line)!!
                blocks += PreviewBlock.Bullet(
                    text = m.groupValues[2].trim(),
                    indentLevel = toIndentLevel(m.groupValues[1])
                )
                index++
            }

            else -> {
                paragraphAccum += line
                index++
            }
        }
    }

    flushParagraph()
    return blocks
}

private fun toIndentLevel(leadingWhitespace: String): Int {
    val count = leadingWhitespace.fold(0) { acc, char ->
        acc + if (char == '\t') 2 else 1
    }
    return count / 2
}
