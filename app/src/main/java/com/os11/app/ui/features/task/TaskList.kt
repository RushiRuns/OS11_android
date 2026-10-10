package com.os11.app.ui.features.task

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.os11.app.ui.components.OSEmptyState
import com.os11.app.ui.theme.OS11Theme
import kotlinx.collections.immutable.ImmutableList

/**
 * Performant task list backed by LazyColumn with stable keys and content types.
 */
@Composable
fun TaskList(
    tasks: ImmutableList<TaskUiModel>,
    onCompletedChange: (TaskUiModel, Boolean) -> Unit,
    onClick: (TaskUiModel) -> Unit,
    onLongClick: (TaskUiModel) -> Unit,
    onStarClick: (TaskUiModel) -> Unit,
    modifier: Modifier = Modifier,
    emptyTitle: String = "No tasks yet",
    emptyDescription: String = "Create a task to get started.",
    contentPadding: PaddingValues = PaddingValues(bottom = OS11Theme.spacing.s16),
) {
    if (tasks.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(OS11Theme.spacing.s4),
            contentAlignment = Alignment.Center,
        ) {
            OSEmptyState(
                title = emptyTitle,
                description = emptyDescription,
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = contentPadding,
        ) {
            items(
                items = tasks,
                key = { it.id },
                contentType = { "task_row" },
            ) { task ->
                TaskRow(
                    task = task,
                    onCompletedChange = { completed -> onCompletedChange(task, completed) },
                    onClick = { onClick(task) },
                    onLongClick = { onLongClick(task) },
                    onStarClick = { onStarClick(task) },
                )
            }
        }
    }
}
