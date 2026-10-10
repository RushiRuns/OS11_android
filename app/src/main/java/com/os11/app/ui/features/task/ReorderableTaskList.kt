package com.os11.app.ui.features.task

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.os11.app.ui.components.OSEmptyState
import com.os11.app.ui.theme.OS11Theme
import com.os11.domain.logic.ordering.FractionalIndex
import kotlinx.collections.immutable.ImmutableList
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * Shared performant reorderable task list component satisfying Phase 8 specifications:
 * - Backed by sh.calvin.reorderable for 60/120fps smooth drag animations
 * - Zero writes during drag: reordering executes purely on local in-memory UI list
 * - Exactly ONE tracked write on drop, computing fractional index position via FractionalIndex
 * - Multi-select mode awareness (disables drag during selection)
 * - Empty state and content padding support
 */
@Composable
fun ReorderableTaskList(
    tasks: ImmutableList<TaskUiModel>,
    onCompletedChange: (TaskUiModel, Boolean) -> Unit,
    onClick: (TaskUiModel) -> Unit,
    onLongClick: (TaskUiModel) -> Unit,
    onStarClick: (TaskUiModel) -> Unit,
    onReorderDrop: (fromIndex: Int, toIndex: Int, movedTask: TaskUiModel, newSortOrder: Double) -> Unit,
    modifier: Modifier = Modifier,
    onWhenClick: ((TaskUiModel) -> Unit)? = null,
    enableSwipe: Boolean = true,
    isSelectionMode: Boolean = false,
    selectedTaskIds: Set<String> = emptySet(),
    onSelectionChange: ((TaskUiModel, Boolean) -> Unit)? = null,
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
        return
    }

    // Local in-memory list tracking reordered state during drag without writing to DB
    var localList by remember(tasks) { mutableStateOf(tasks.toList()) }
    var lastMovedFromIndex by remember { mutableIntStateOf(-1) }
    var lastMovedToIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(tasks) {
        localList = tasks.toList()
    }

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
    ) { from, to ->
        lastMovedFromIndex = from.index
        lastMovedToIndex = to.index
        localList = localList.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        items(
            items = localList,
            key = { it.id },
            contentType = { "task_row" },
        ) { task ->
            ReorderableItem(
                state = reorderableState,
                key = task.id,
                enabled = !isSelectionMode,
            ) { isDragging ->
                val handleModifier = if (!isSelectionMode) {
                    Modifier.longPressDraggableHandle(
                        onDragStopped = {
                            val targetIndex = localList.indexOfFirst { it.id == task.id }
                            if (targetIndex >= 0 && lastMovedFromIndex >= 0) {
                                // Calculate new FractionalIndex position
                                val newSortOrder = calculateNewSortOrder(localList, targetIndex)
                                onReorderDrop(lastMovedFromIndex, targetIndex, task, newSortOrder)
                                lastMovedFromIndex = -1
                                lastMovedToIndex = -1
                            }
                        },
                    )
                } else {
                    Modifier
                }

                TaskRow(
                    task = task,
                    onCompletedChange = { completed -> onCompletedChange(task, completed) },
                    onClick = { onClick(task) },
                    onLongClick = { onLongClick(task) },
                    onStarClick = { onStarClick(task) },
                    onWhenClick = onWhenClick?.let { { it(task) } },
                    enableSwipe = enableSwipe && !isDragging,
                    isSelectionMode = isSelectionMode,
                    isSelected = selectedTaskIds.contains(task.id),
                    onSelectionChange = onSelectionChange?.let { { selected -> it(task, selected) } },
                    dragHandleModifier = handleModifier,
                )
            }
        }
    }
}

/**
 * Calculates deterministic fractional index order after drag drop.
 */
private fun calculateNewSortOrder(list: List<TaskUiModel>, targetIndex: Int): Double {
    if (list.size <= 1) return 0.0

    return when (targetIndex) {
        0 -> {
            // Moved to the very top: before the next element
            val nextSort = list.getOrNull(1)?.sortOrder
            FractionalIndex.atStart(nextSort)
        }
        list.size - 1 -> {
            // Moved to the very end: after the previous element
            val prevSort = list.getOrNull(list.size - 2)?.sortOrder
            FractionalIndex.atEnd(prevSort)
        }
        else -> {
            // Moved in between two neighbors
            val prevSort = list[targetIndex - 1].sortOrder
            val nextSort = list[targetIndex + 1].sortOrder
            FractionalIndex.between(prevSort, nextSort)
        }
    }
}
