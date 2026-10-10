package com.os11.app.ui.features.project

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.os11.app.service.project.SectionItem
import com.os11.app.ui.components.OSCheckbox
import com.os11.app.ui.components.OSEmptyState
import com.os11.app.ui.components.OSIconButton
import com.os11.app.ui.components.OSPriority
import com.os11.app.ui.components.OSPriorityBar
import com.os11.app.ui.features.task.TaskUiModel
import com.os11.app.ui.theme.OS11Theme
import kotlinx.coroutines.launch

data class BoardColumn(val id: String?, val name: String)

@Composable
@Suppress("LongParameterList")
fun ProjectBoardView(
    sections: List<SectionItem>,
    tasks: List<TaskUiModel>,
    onCompletedChange: (taskId: String, isCompleted: Boolean) -> Unit,
    onTaskClick: (TaskUiModel) -> Unit,
    onTaskLongClick: (TaskUiModel) -> Unit,
    onMoveTaskToSection: (taskId: String, targetSectionId: String?) -> Unit,
    onAddTaskToSection: (sectionId: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val scope = rememberCoroutineScope()

    val columns = remember(sections, tasks) {
        val list = mutableListOf<BoardColumn>()
        val hasUnsectioned = tasks.any { it.sectionId == null }
        if (hasUnsectioned || sections.isEmpty()) {
            list.add(BoardColumn(id = null, name = if (sections.isEmpty()) "All Tasks" else "General"))
        }
        sections.forEach { section ->
            list.add(BoardColumn(id = section.id, name = section.name))
        }
        list
    }

    if (columns.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(spacing.s4),
            contentAlignment = Alignment.Center,
        ) {
            OSEmptyState(
                title = "No board columns",
                description = "Add sections to organize tasks into board columns.",
            )
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { columns.size })

    Column(modifier = modifier.fillMaxSize()) {
        // Tab row for quick column jumping
        if (columns.size > 1) {
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage.coerceIn(0, columns.size - 1),
                edgePadding = spacing.s4,
                containerColor = colors.surfaceRaised,
                contentColor = colors.accent,
                modifier = Modifier.fillMaxWidth(),
            ) {
                columns.forEachIndexed { index, col ->
                    val taskCount = tasks.count { it.sectionId == col.id }
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
                        text = {
                            Text(
                                text = "${col.name} ($taskCount)",
                                style = type.sectionHeader,
                            )
                        },
                    )
                }
            }
        }

        // Pager of columns
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = spacing.s3, vertical = spacing.s2),
            pageSpacing = spacing.s3,
        ) { page ->
            val col = columns[page]
            val colTasks = tasks.filter { it.sectionId == col.id }

            BoardColumnContent(
                column = col,
                tasks = colTasks,
                allColumns = columns,
                onCompletedChange = onCompletedChange,
                onTaskClick = onTaskClick,
                onTaskLongClick = onTaskLongClick,
                onMoveTaskToSection = onMoveTaskToSection,
                onAddTask = { onAddTaskToSection(col.id) },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun BoardColumnContent(
    column: BoardColumn,
    tasks: List<TaskUiModel>,
    allColumns: List<BoardColumn>,
    onCompletedChange: (taskId: String, isCompleted: Boolean) -> Unit,
    onTaskClick: (TaskUiModel) -> Unit,
    onTaskLongClick: (TaskUiModel) -> Unit,
    onMoveTaskToSection: (taskId: String, targetSectionId: String?) -> Unit,
    onAddTask: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    Column(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(OS11Theme.radius.lg))
            .background(colors.surfaceRaised)
            .padding(spacing.s3),
    ) {
        // Column Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                Text(
                    text = column.name,
                    style = type.panelTitle.copy(color = colors.textPrimary),
                )
                Text(
                    text = "${tasks.size}",
                    style = type.meta.copy(color = colors.textTertiary),
                )
            }
            OSIconButton(
                onClick = onAddTask,
                contentDescription = "Add task to ${column.name}",
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = colors.accent,
                )
            }
        }

        // Column task list
        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(spacing.s4),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No tasks in this column",
                    style = type.body.copy(color = colors.textTertiary),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                items(
                    items = tasks,
                    key = { it.id },
                    contentType = { "board_card" },
                ) { task ->
                    BoardTaskCard(
                        task = task,
                        currentColumnId = column.id,
                        allColumns = allColumns,
                        onCompletedChange = { onCompletedChange(task.id, it) },
                        onClick = { onTaskClick(task) },
                        onLongClick = { onTaskLongClick(task) },
                        onMove = { targetId -> onMoveTaskToSection(task.id, targetId) },
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
@Suppress("LongParameterList")
private fun BoardTaskCard(
    task: TaskUiModel,
    currentColumnId: String?,
    allColumns: List<BoardColumn>,
    onCompletedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMove: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    var moveMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(OS11Theme.radius.md))
            .background(colors.surfaceRaised)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Priority strip
        OSPriorityBar(
            priority = OSPriority.fromInt(task.priority),
            modifier = Modifier
                .width(4.dp)
                .height(36.dp),
        )
        Spacer(modifier = Modifier.width(spacing.s2))

        // Checkbox
        OSCheckbox(
            checked = task.isCompleted,
            onCheckedChange = onCompletedChange,
        )
        Spacer(modifier = Modifier.width(spacing.s2))

        // Title and meta
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = type.taskTitle.copy(color = colors.textPrimary),
                maxLines = 2,
            )
            if (task.formattedDueDate != null) {
                Text(
                    text = task.formattedDueDate,
                    style = type.meta.copy(
                        color = if (task.isOverdue) colors.danger else colors.textTertiary,
                    ),
                )
            }
        }

        // Move to column dropdown
        if (allColumns.size > 1) {
            Box {
                OSIconButton(
                    onClick = { moveMenuExpanded = true },
                    contentDescription = "Move task to another column",
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = colors.textSecondary,
                    )
                }

                DropdownMenu(
                    expanded = moveMenuExpanded,
                    onDismissRequest = { moveMenuExpanded = false },
                ) {
                    allColumns.filter { it.id != currentColumnId }.forEach { target ->
                        DropdownMenuItem(
                            text = { Text("Move to ${target.name}") },
                            onClick = {
                                moveMenuExpanded = false
                                onMove(target.id)
                            },
                        )
                    }
                }
            }
        }
    }
}
