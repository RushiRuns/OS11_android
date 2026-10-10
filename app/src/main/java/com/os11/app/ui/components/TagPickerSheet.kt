package com.os11.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.os11.app.service.tag.TagItem
import com.os11.app.ui.theme.OS11Theme

/**
 * Modal Bottom Sheet for searching, picking, and inline creating tags with nested tag support.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagPickerSheet(
    tags: List<TagItem>,
    selectedTagIds: Set<String>,
    onTagToggled: (String) -> Unit,
    onCreateTag: (name: String, parentTagId: String?) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val radius = OS11Theme.radius
    val type = OS11Theme.type

    var query by remember { mutableStateOf("") }
    var selectedParentId by remember { mutableStateOf<String?>(null) }

    val filteredTags = remember(tags, query) {
        if (query.isBlank()) {
            tags
        } else {
            val q = query.trim().lowercase()
            tags.filter { it.name.lowercase().contains(q) }
        }
    }

    val exactMatch = remember(tags, query) {
        val q = query.trim().lowercase()
        tags.any { it.name.lowercase() == q }
    }

    OSSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(spacing.s4),
        ) {
            Text(
                text = "Tags",
                style = type.subViewTitle.copy(color = colors.textPrimary),
            )
            Spacer(modifier = Modifier.height(spacing.s3))

            // Search / Create field
            OSTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search or create tag...",
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(spacing.s3))

            // Inline Create option if no exact match
            if (query.isNotBlank() && !exactMatch) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radius.md))
                        .background(colors.surfaceRaised)
                        .border(spacing.px, colors.borderDefault, RoundedCornerShape(radius.md))
                        .padding(spacing.s3),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button) {
                                onCreateTag(query.trim(), selectedParentId)
                                query = ""
                                selectedParentId = null
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = colors.accent,
                            )
                            Text(
                                text = "Create \"#${query.trim()}\"",
                                style = type.body.copy(color = colors.accent),
                            )
                        }
                    }

                    // Optional parent tag assignment for nesting
                    if (tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(spacing.s2))
                        Text(
                            text = "Nest under parent tag (optional):",
                            style = type.meta.copy(color = colors.textSecondary),
                        )
                        Spacer(modifier = Modifier.height(spacing.s1))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                        ) {
                            OSChip(
                                label = "Top level",
                                selected = selectedParentId == null,
                                onClick = { selectedParentId = null },
                            )
                            tags.take(4).forEach { tag ->
                                OSChip(
                                    label = tag.displayName,
                                    selected = selectedParentId == tag.id,
                                    onClick = { selectedParentId = tag.id },
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(spacing.s3))
            }

            // Tag List with nesting indentation
            if (filteredTags.isEmpty() && query.isBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = spacing.s6),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No tags yet. Type above to create one.",
                        style = type.meta.copy(color = colors.textTertiary),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing.s1),
                ) {
                    items(filteredTags, key = { it.id }) { tag ->
                        val isSelected = tag.id in selectedTagIds
                        val shape = OSTagShape.fromTag(tag.name, tag.color)
                        val color = colors.tags.fromNameOrHex(tag.color)
                        val indent = (tag.depth * 16).dp

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(radius.sm))
                                .background(if (isSelected) colors.surfaceSelected else colors.surfaceBase)
                                .clickable(role = Role.Checkbox) { onTagToggled(tag.id) }
                                .padding(horizontal = spacing.s3)
                                .semantics {
                                    contentDescription = "${tag.name} tag (${shape.label})"
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (indent > 0.dp) {
                                Spacer(modifier = Modifier.width(indent))
                            }

                            OSTagDot(
                                color = color,
                                shape = shape,
                                variant = OSTagDotVariant.TaskRow,
                            )

                            Spacer(modifier = Modifier.width(spacing.s3))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tag.displayName,
                                    style = type.body.copy(
                                        color = if (isSelected) colors.accent else colors.textPrimary,
                                    ),
                                )
                                if (tag.parentPath != null) {
                                    Text(
                                        text = tag.parentPath.orEmpty(),
                                        style = type.meta.copy(color = colors.textTertiary),
                                    )
                                }
                            }

                            if (tag.taskCount > 0) {
                                Text(
                                    text = "${tag.taskCount}",
                                    style = type.meta.copy(color = colors.textSecondary),
                                    modifier = Modifier.padding(end = spacing.s2),
                                )
                            }

                            OSCheckbox(
                                checked = isSelected,
                                onCheckedChange = { onTagToggled(tag.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}
