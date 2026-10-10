package com.os11.app.ui.components.note

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.sp
import com.os11.app.service.note.NoteSanitizer
import com.os11.app.ui.theme.OS11Theme

/**
 * Lightweight native note editor per ADR-A11 and Spike S-2.
 *
 * Provides inline formatting shortcuts (Bold, Italic, Strikethrough, Code, Lists, Checklists)
 * and sanitizes HTML output via Jsoup upon save.
 */
@Composable
fun NoteEditor(
    initialText: String,
    onNotesChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Add notes...",
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val radius = OS11Theme.radius

    var textFieldValue by remember(initialText) {
        val plain = NoteSanitizer.toPlainText(initialText).ifBlank { initialText }
        mutableStateOf(TextFieldValue(plain, selection = TextRange(plain.length)))
    }

    fun applyWrapFormatting(prefix: String, suffix: String) {
        val text = textFieldValue.text
        val selection = textFieldValue.selection
        val selectedText = if (selection.min < selection.max) text.substring(selection.min, selection.max) else ""

        val newText = if (selectedText.isNotEmpty()) {
            text.replaceRange(selection.min, selection.max, "$prefix$selectedText$suffix")
        } else {
            text.replaceRange(selection.start, selection.end, "$prefix$suffix")
        }
        val newCursor = if (selectedText.isNotEmpty()) {
            selection.max + prefix.length + suffix.length
        } else {
            selection.start + prefix.length
        }

        textFieldValue = TextFieldValue(newText, selection = TextRange(newCursor))
        onNotesChanged(NoteSanitizer.fromPlainText(newText))
    }

    fun applyPrefixFormatting(prefix: String) {
        val text = textFieldValue.text
        val selection = textFieldValue.selection
        val lineStart = text.lastIndexOf('\n', selection.start - 1).let { if (it == -1) 0 else it + 1 }

        val newText = text.replaceRange(lineStart, lineStart, prefix)
        val newCursor = selection.start + prefix.length

        textFieldValue = TextFieldValue(newText, selection = TextRange(newCursor))
        onNotesChanged(NoteSanitizer.fromPlainText(newText))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radius.lg))
            .background(colors.surfaceRaised)
            .border(spacing.px, colors.borderSubtle, RoundedCornerShape(radius.lg)),
    ) {
        // Formatting Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surfaceSidebar)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = spacing.s2, vertical = spacing.s1),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.s1),
        ) {
            TextToolbarButton(
                label = "B",
                fontWeight = FontWeight.Bold,
                description = "Bold",
                onClick = { applyWrapFormatting("**", "**") },
            )
            TextToolbarButton(
                label = "I",
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                description = "Italic",
                onClick = { applyWrapFormatting("*", "*") },
            )
            TextToolbarButton(
                label = "S",
                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                description = "Strikethrough",
                onClick = { applyWrapFormatting("~~", "~~") },
            )
            TextToolbarButton(
                label = "</>",
                fontFamily = FontFamily.Monospace,
                description = "Code",
                onClick = { applyWrapFormatting("`", "`") },
            )
            TextToolbarButton(
                label = "• List",
                description = "Bullet List",
                onClick = { applyPrefixFormatting("• ") },
            )
            TextToolbarButton(
                label = "[ ] Todo",
                description = "Task Item",
                onClick = { applyPrefixFormatting("[ ] ") },
            )
        }

        Spacer(modifier = Modifier.height(spacing.s2))

        // Editor input area
        BasicTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
                onNotesChanged(NoteSanitizer.fromPlainText(newValue.text))
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s4, vertical = spacing.s2),
            textStyle = type.body.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.accent),
            decorationBox = { innerTextField ->
                if (textFieldValue.text.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = type.body.copy(color = colors.textTertiary),
                    )
                }
                innerTextField()
            },
        )

        Spacer(modifier = Modifier.height(spacing.s2))
    }
}

@Composable
private fun TextToolbarButton(
    label: String,
    description: String,
    onClick: () -> Unit,
    fontWeight: FontWeight = FontWeight.Normal,
    fontStyle: androidx.compose.ui.text.font.FontStyle = androidx.compose.ui.text.font.FontStyle.Normal,
    textDecoration: androidx.compose.ui.text.style.TextDecoration =
        androidx.compose.ui.text.style.TextDecoration.None,
    fontFamily: FontFamily = FontFamily.Default,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val radius = OS11Theme.radius
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(radius.xs))
            .clickable(
                role = Role.Button,
                onClickLabel = description,
                interactionSource = interactionSource,
                indication = ripple(color = colors.surfaceHover),
                onClick = onClick,
            )
            .padding(horizontal = spacing.s2, vertical = spacing.s1),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            textDecoration = textDecoration,
            fontFamily = fontFamily,
            color = colors.textSecondary,
        )
    }
}
