package com.example.hybridai.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hybridai.ui.theme.*

/**
 * Lightweight inline markdown renderer — no external dependency.
 * Handles: **bold**, *italic*, `inline code`, ``` code blocks ```, - bullets.
 * Renders each "segment" as either a styled AnnotatedString or a code block Box.
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier
) {
    val segments = parseMarkdown(text)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        segments.forEach { segment ->
            when (segment) {
                is MarkdownSegment.Paragraph -> {
                    Text(
                        text = segment.annotated,
                        color = PrimaryAccent,
                        fontSize = 14.sp,
                        lineHeight = 21.sp
                    )
                }
                is MarkdownSegment.CodeBlock -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CodeBackground, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = segment.code,
                            color = CodeText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )
                    }
                }
            }
        }
    }
}

// ── Segment types ─────────────────────────────────────────────────────────

private sealed class MarkdownSegment {
    data class Paragraph(val annotated: androidx.compose.ui.text.AnnotatedString) : MarkdownSegment()
    data class CodeBlock(val code: String) : MarkdownSegment()
}

// ── Parser ────────────────────────────────────────────────────────────────

private fun parseMarkdown(raw: String): List<MarkdownSegment> {
    val segments = mutableListOf<MarkdownSegment>()
    // Split on fenced code blocks (``` ... ```)
    val parts = raw.split(Regex("```[a-zA-Z]*\\n?"))

    parts.forEachIndexed { index, part ->
        val trimmed = part.trimEnd('\n')
        if (trimmed.isBlank()) return@forEachIndexed
        if (index % 2 == 1) {
            // Odd index = inside code fence
            segments += MarkdownSegment.CodeBlock(trimmed)
        } else {
            // Even index = normal text — render line by line for bullets
            val lines = trimmed.lines()
            val paragraphBuilder = StringBuilder()
            lines.forEach { line ->
                val bullet = Regex("^[\\-\\*] (.+)").find(line)
                if (bullet != null) {
                    // Flush accumulated paragraph first
                    if (paragraphBuilder.isNotBlank()) {
                        segments += MarkdownSegment.Paragraph(renderInline(paragraphBuilder.toString().trim()))
                        paragraphBuilder.clear()
                    }
                    segments += MarkdownSegment.Paragraph(renderInline("  •  " + bullet.groupValues[1]))
                } else {
                    paragraphBuilder.appendLine(line)
                }
            }
            if (paragraphBuilder.isNotBlank()) {
                segments += MarkdownSegment.Paragraph(renderInline(paragraphBuilder.toString().trim()))
            }
        }
    }
    return segments
}

/** Converts **bold**, *italic*, `inline code` to AnnotatedString spans */
private fun renderInline(text: String) = buildAnnotatedString {
    val bold    = Regex("\\*\\*(.+?)\\*\\*")
    val italic  = Regex("\\*(.+?)\\*")
    val code    = Regex("`(.+?)`")

    // Merge all patterns with positions
    val spans = (bold.findAll(text).map { Triple(it.range, it.groupValues[1], "bold") }
            + italic.findAll(text).map { Triple(it.range, it.groupValues[1], "italic") }
            + code.findAll(text).map { Triple(it.range, it.groupValues[1], "code") })
        .sortedBy { it.first.first }
        .toMutableList()

    var cursor = 0
    spans.forEach { (range, content, type) ->
        if (range.first >= cursor) {
            append(text.substring(cursor, range.first))
            when (type) {
                "bold"   -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(content) }
                "italic" -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(content) }
                "code"   -> withStyle(SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = CodeBackground,
                    color = CodeText
                )) { append(content) }
            }
            cursor = range.last + 1
        }
    }
    if (cursor < text.length) append(text.substring(cursor))
}
