package com.os11.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.os11.app.ui.theme.OS11Theme

/**
 * Inline Priority Picker row component.
 */
@Composable
fun OSPriorityPicker(
    selectedPriority: OSPriority,
    onPriorityChange: (OSPriority) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = OS11Theme.spacing

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OSPriority.entries.forEach { priority ->
            val isSelected = priority == selectedPriority
            OSPriorityChip(
                priority = priority,
                isSelected = isSelected,
                onClick = { onPriorityChange(priority) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Individual priority selectable chip with 2.5dp left border and critical alpha pulse.
 */
@Composable
private fun OSPriorityChip(
    priority: OSPriority,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val radius = OS11Theme.radius
    val type = OS11Theme.type

    val shape = RoundedCornerShape(radius.sm)
    val bgColor = if (isSelected) colors.surfaceSelected else colors.surfaceRaised
    val borderColor = if (isSelected) colors.accent else colors.borderDefault

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .height(IntrinsicSize.Min)
            .clip(shape)
            .background(bgColor)
            .border(spacing.px, borderColor, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics {
                contentDescription = "${priority.label} priority"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (priority != OSPriority.None) {
            OSPriorityBar(priority = priority)
        } else {
            Spacer(modifier = Modifier.width(spacing.s1))
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = spacing.s1),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = priority.label,
                style = type.meta.copy(
                    color = if (isSelected) colors.accent else colors.textPrimary,
                ),
                maxLines = 1,
            )
        }
    }
}

/**
 * Modal Bottom Sheet for selecting task priority (e.g., from Bulk Actions or Detail view).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OSPriorityPickerSheet(
    selectedPriority: OSPriority,
    onPrioritySelected: (OSPriority) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val radius = OS11Theme.radius
    val type = OS11Theme.type

    OSSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(spacing.s4),
        ) {
            Text(
                text = "Set Priority",
                style = type.subViewTitle.copy(color = colors.textPrimary),
            )
            Spacer(modifier = Modifier.height(spacing.s3))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                OSPriority.entries.forEach { priority ->
                    val isSelected = priority == selectedPriority
                    val shape = RoundedCornerShape(radius.md)
                    val bgColor = if (isSelected) colors.surfaceSelected else colors.surfaceRaised
                    val borderColor = if (isSelected) colors.accent else colors.borderDefault

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(shape)
                            .background(bgColor)
                            .border(spacing.px, borderColor, shape)
                            .clickable(role = Role.RadioButton) {
                                onPrioritySelected(priority)
                                onDismissRequest()
                            }
                            .semantics {
                                contentDescription = "${priority.label} Priority"
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (priority != OSPriority.None) {
                            OSPriorityBar(priority = priority)
                        } else {
                            Box(modifier = Modifier.width(spacing.s2))
                        }

                        Spacer(modifier = Modifier.width(spacing.s3))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = priority.label,
                                style = type.body.copy(
                                    color = if (isSelected) colors.accent else colors.textPrimary,
                                ),
                            )
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = colors.accent,
                                modifier = Modifier
                                    .padding(end = spacing.s3)
                                    .size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
