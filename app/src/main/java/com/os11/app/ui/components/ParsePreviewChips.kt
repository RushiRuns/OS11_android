package com.os11.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.os11.app.ui.theme.OS11Theme
import com.os11.domain.nlp.NlpParseResult
import com.os11.domain.nlp.NlpTokenMatch
import com.os11.domain.nlp.NlpTokenType

/**
 * Preview chips displaying structured attributes parsed by NlpParser from Quick Add text.
 */
@Composable
fun ParsePreviewChips(
    parseResult: NlpParseResult,
    modifier: Modifier = Modifier,
    onRemoveToken: ((NlpTokenMatch) -> Unit)? = null,
) {
    val spacing = OS11Theme.spacing

    if (!hasAnyTokens(parseResult)) return

    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(vertical = spacing.s1),
        horizontalArrangement = Arrangement.spacedBy(spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DateAndTimeChips(parseResult, onRemoveToken)
        PriorityAndHierarchyChips(parseResult, onRemoveToken)
        TagsAndExtraChips(parseResult, onRemoveToken)
    }
}

@Composable
private fun DateAndTimeChips(parseResult: NlpParseResult, onRemoveToken: ((NlpTokenMatch) -> Unit)?) {
    val colors = OS11Theme.colors
    parseResult.dueDate?.let { date ->
        PreviewChip(
            label = date,
            icon = Icons.Default.DateRange,
            tint = colors.accent,
            onRemove = findToken(parseResult, NlpTokenType.DATE)?.let { token ->
                { onRemoveToken?.invoke(token) }
            },
        )
    }

    parseResult.dueTime?.let { time ->
        PreviewChip(
            label = time,
            icon = Icons.Default.Notifications,
            tint = colors.accent,
            onRemove = findToken(parseResult, NlpTokenType.TIME)?.let { token ->
                { onRemoveToken?.invoke(token) }
            },
        )
    }
}

@Composable
private fun PriorityAndHierarchyChips(parseResult: NlpParseResult, onRemoveToken: ((NlpTokenMatch) -> Unit)?) {
    val colors = OS11Theme.colors
    if (parseResult.priority > 0) {
        val (label, tint) = priorityMeta(parseResult.priority)
        PreviewChip(
            label = label,
            icon = Icons.Default.Star,
            tint = tint,
            onRemove = findToken(parseResult, NlpTokenType.PRIORITY)?.let { token ->
                { onRemoveToken?.invoke(token) }
            },
        )
    }

    parseResult.areaName?.let { area ->
        PreviewChip(
            label = "@$area",
            icon = Icons.Default.Place,
            tint = colors.accent,
            onRemove = findToken(parseResult, NlpTokenType.AREA)?.let { token ->
                { onRemoveToken?.invoke(token) }
            },
        )
    }

    parseResult.projectName?.let { project ->
        PreviewChip(
            label = "/$project",
            icon = Icons.AutoMirrored.Filled.List,
            tint = colors.accent,
            onRemove = findToken(parseResult, NlpTokenType.PROJECT)?.let { token ->
                { onRemoveToken?.invoke(token) }
            },
        )
    }
}

@Composable
private fun TagsAndExtraChips(parseResult: NlpParseResult, onRemoveToken: ((NlpTokenMatch) -> Unit)?) {
    val colors = OS11Theme.colors
    parseResult.tags.forEach { tag ->
        val tagToken = parseResult.matchedTokens.firstOrNull {
            it.type == NlpTokenType.TAG && it.rawText.trimStart('#').equals(tag, ignoreCase = true)
        }
        PreviewChip(
            label = "#$tag",
            icon = null,
            tint = colors.tags.purple,
            onRemove = tagToken?.let { token -> { onRemoveToken?.invoke(token) } },
        )
    }

    parseResult.recurrenceRule?.let { recurrence ->
        PreviewChip(
            label = recurrence,
            icon = Icons.Default.Refresh,
            tint = colors.accent,
            onRemove = findToken(parseResult, NlpTokenType.RECURRENCE)?.let { token ->
                { onRemoveToken?.invoke(token) }
            },
        )
    }

    parseResult.estimatedPomodoros?.let { pomodoros ->
        PreviewChip(
            label = "🍅 $pomodoros",
            icon = null,
            tint = colors.priorityCritical,
            onRemove = findToken(parseResult, NlpTokenType.POMODORO)?.let { token ->
                { onRemoveToken?.invoke(token) }
            },
        )
    }
}

private fun hasAnyTokens(result: NlpParseResult): Boolean = result.dueDate != null ||
    result.dueTime != null ||
    result.priority > 0 ||
    result.areaName != null ||
    result.projectName != null ||
    result.tags.isNotEmpty() ||
    result.recurrenceRule != null ||
    result.estimatedPomodoros != null

private fun findToken(result: NlpParseResult, type: NlpTokenType): NlpTokenMatch? =
    result.matchedTokens.firstOrNull { it.type == type }

@Composable
private fun priorityMeta(priority: Int): Pair<String, Color> {
    val colors = OS11Theme.colors
    return when (priority) {
        PRIORITY_P1 -> "P1 (Critical)" to colors.priorityCritical
        PRIORITY_P2 -> "P2 (High)" to colors.priorityHigh
        PRIORITY_P3 -> "P3 (Medium)" to colors.priorityMedium
        else -> "P4 (Low)" to colors.priorityLow
    }
}

private const val PRIORITY_P1 = 4
private const val PRIORITY_P2 = 3
private const val PRIORITY_P3 = 2

@Composable
private fun PreviewChip(
    label: String,
    icon: ImageVector?,
    tint: Color,
    onRemove: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val chipIconSize = 14.dp
    val removeIconSize = 12.dp

    OSChip(
        label = label,
        modifier = modifier,
        tintColor = tint,
        leadingContent = icon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(chipIconSize),
                )
            }
        },
        trailingContent = onRemove?.let { removeAction ->
            {
                OSIconButton(
                    onClick = removeAction,
                    contentDescription = "Remove $label",
                    modifier = Modifier.size(removeIconSize),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(removeIconSize),
                    )
                }
            }
        },
    )
}
