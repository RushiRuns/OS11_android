package com.os11.app.ui.features.trash

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.os11.app.ui.components.OSButton
import com.os11.app.ui.components.OSButtonVariant
import com.os11.app.ui.components.OSEmptyState
import com.os11.app.ui.components.OSIconButton
import com.os11.app.ui.features.task.TaskUiModel
import com.os11.app.ui.theme.OS11Theme

/**
 * Trash screen displaying trashed items, restore actions, permanent deletion,
 * and 30-day retention policies.
 */
@Composable
fun TrashScreen(modifier: Modifier = Modifier, viewModel: TrashViewModel = hiltViewModel()) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val radius = OS11Theme.radius

    val trashedTasks by viewModel.trashedTasks.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.s4),
    ) {
        // 30-Day Retention Notice Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(radius.md))
                .background(colors.surfaceRaised)
                .border(spacing.px, colors.borderSubtle, RoundedCornerShape(radius.md))
                .padding(spacing.s3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "Items in trash are automatically purged after 30 days.",
                style = type.meta.copy(color = colors.textSecondary),
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(spacing.s3))

        // Empty Trash button if tasks exist
        if (trashedTasks.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                OSButton(
                    variant = OSButtonVariant.Danger,
                    onClick = { viewModel.emptyTrash() },
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(spacing.s1))
                    Text("Empty Trash")
                }
            }
            Spacer(modifier = Modifier.height(spacing.s2))
        }

        // List or Empty State
        if (trashedTasks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                OSEmptyState(
                    title = "Trash is empty",
                    description = "Deleted tasks will appear here before being permanently removed.",
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(spacing.s1),
            ) {
                items(
                    items = trashedTasks,
                    key = { it.id },
                ) { task ->
                    TrashRow(
                        task = task,
                        onRestore = { viewModel.restoreTask(task.id) },
                        onDeletePermanently = { viewModel.deletePermanently(task.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TrashRow(task: TaskUiModel, onRestore: () -> Unit, onDeletePermanently: () -> Unit) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val radius = OS11Theme.radius

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radius.md))
            .background(colors.surfaceBase)
            .border(spacing.px, colors.borderSubtle, RoundedCornerShape(radius.md))
            .padding(horizontal = spacing.s3, vertical = spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = type.body.copy(color = colors.textPrimary),
            )
            if (task.formattedDueDate != null) {
                Text(
                    text = "Due: ${task.formattedDueDate}",
                    style = type.meta.copy(color = colors.textTertiary),
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.s1),
        ) {
            OSIconButton(
                onClick = onRestore,
                contentDescription = "Restore task",
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = null,
                    tint = colors.accent,
                )
            }

            OSIconButton(
                onClick = onDeletePermanently,
                contentDescription = "Delete permanently",
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = colors.priorityCritical,
                )
            }
        }
    }
}
