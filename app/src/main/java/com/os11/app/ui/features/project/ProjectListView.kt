package com.os11.app.ui.features.project

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.os11.app.service.project.SectionItem
import com.os11.app.ui.components.OSButton
import com.os11.app.ui.components.OSEmptyState
import com.os11.app.ui.components.OSIconButton
import com.os11.app.ui.features.task.TaskRow
import com.os11.app.ui.features.task.TaskUiModel
import com.os11.app.ui.theme.OS11Theme

@Composable
@Suppress("LongParameterList")
fun ProjectListView(
    sections: List<SectionItem>,
    tasks: List<TaskUiModel>,
    onCompletedChange: (taskId: String, isCompleted: Boolean) -> Unit,
    onTaskClick: (TaskUiModel) -> Unit,
    onTaskLongClick: (TaskUiModel) -> Unit,
    onToggleStar: (taskId: String) -> Unit,
    onToggleCollapse: (sectionId: String, isCollapsed: Boolean) -> Unit,
    onRenameSection: (SectionItem) -> Unit,
    onMoveSectionUp: (SectionItem) -> Unit,
    onMoveSectionDown: (SectionItem) -> Unit,
    onDeleteSection: (SectionItem) -> Unit,
    onAddSectionClick: () -> Unit,
    onAddTaskToSection: (sectionId: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    if (tasks.isEmpty() && sections.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(spacing.s4),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.s3),
            ) {
                OSEmptyState(
                    title = "No tasks yet",
                    description = "Add tasks to plan and track deliverables for this project.",
                )
                OSButton(onClick = onAddSectionClick) {
                    Text("Add Section")
                }
            }
        }
        return
    }

    val unsectionedTasks = tasks.filter { it.sectionId == null }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(spacing.s2),
    ) {
        // 1. Unsectioned tasks (if any)
        if (unsectionedTasks.isNotEmpty() && sections.isNotEmpty()) {
            item(key = "unsectioned_header", contentType = "section_header") {
                SectionHeaderRow(
                    title = "General",
                    count = unsectionedTasks.size,
                    isCollapsed = false,
                    canCollapse = false,
                    onToggleCollapse = { },
                    onOptionsClick = null,
                    onAddTaskClick = { onAddTaskToSection(null) },
                )
            }
            items(
                items = unsectionedTasks,
                key = { it.id },
                contentType = { "task_row" },
            ) { task ->
                TaskRow(
                    task = task,
                    onCompletedChange = { onCompletedChange(task.id, it) },
                    onClick = { onTaskClick(task) },
                    onLongClick = { onTaskLongClick(task) },
                    onStarClick = { onToggleStar(task.id) },
                )
            }
        } else if (sections.isEmpty()) {
            // No sections defined: list all tasks directly
            items(
                items = tasks,
                key = { it.id },
                contentType = { "task_row" },
            ) { task ->
                TaskRow(
                    task = task,
                    onCompletedChange = { onCompletedChange(task.id, it) },
                    onClick = { onTaskClick(task) },
                    onLongClick = { onTaskLongClick(task) },
                    onStarClick = { onToggleStar(task.id) },
                )
            }
        }

        // 2. Sections
        sections.forEachIndexed { index, section ->
            val sectionTasks = tasks.filter { it.sectionId == section.id }
            val isFirst = index == 0
            val isLast = index == sections.size - 1

            item(key = "section_${section.id}", contentType = "section_header") {
                SectionHeader(
                    section = section,
                    count = sectionTasks.size,
                    isFirst = isFirst,
                    isLast = isLast,
                    onToggleCollapse = { onToggleCollapse(section.id, !section.isCollapsed) },
                    onRename = { onRenameSection(section) },
                    onMoveUp = { onMoveSectionUp(section) },
                    onMoveDown = { onMoveSectionDown(section) },
                    onDelete = { onDeleteSection(section) },
                    onAddTask = { onAddTaskToSection(section.id) },
                )
            }

            if (!section.isCollapsed) {
                items(
                    items = sectionTasks,
                    key = { it.id },
                    contentType = { "task_row" },
                ) { task ->
                    TaskRow(
                        task = task,
                        onCompletedChange = { onCompletedChange(task.id, it) },
                        onClick = { onTaskClick(task) },
                        onLongClick = { onTaskLongClick(task) },
                        onStarClick = { onToggleStar(task.id) },
                    )
                }
            }
        }

        // 3. Add section button at bottom
        item(key = "add_section_footer") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.s4, vertical = spacing.s3),
                horizontalArrangement = Arrangement.Center,
            ) {
                OSButton(onClick = onAddSectionClick) {
                    Text("+ Add Section")
                }
            }
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun SectionHeader(
    section: SectionItem,
    count: Int,
    isFirst: Boolean,
    isLast: Boolean,
    onToggleCollapse: () -> Unit,
    onRename: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    onAddTask: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    SectionHeaderRow(
        title = section.name,
        count = count,
        isCollapsed = section.isCollapsed,
        canCollapse = true,
        onToggleCollapse = onToggleCollapse,
        onOptionsClick = { menuExpanded = true },
        onAddTaskClick = onAddTask,
    )

    if (menuExpanded) {
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("Rename") },
                onClick = {
                    menuExpanded = false
                    onRename()
                },
            )
            if (!isFirst) {
                DropdownMenuItem(
                    text = { Text("Move Up") },
                    onClick = {
                        menuExpanded = false
                        onMoveUp()
                    },
                )
            }
            if (!isLast) {
                DropdownMenuItem(
                    text = { Text("Move Down") },
                    onClick = {
                        menuExpanded = false
                        onMoveDown()
                    },
                )
            }
            DropdownMenuItem(
                text = { Text("Delete Section") },
                onClick = {
                    menuExpanded = false
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun SectionHeaderRow(
    title: String,
    count: Int,
    isCollapsed: Boolean,
    canCollapse: Boolean,
    onToggleCollapse: () -> Unit,
    onOptionsClick: (() -> Unit)?,
    onAddTaskClick: () -> Unit,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(OS11Theme.radius.md))
            .background(colors.surfaceRaised)
            .clickable(enabled = canCollapse, role = Role.Button, onClick = onToggleCollapse)
            .padding(horizontal = spacing.s3, vertical = spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.s1),
        ) {
            if (canCollapse) {
                Icon(
                    imageVector = if (isCollapsed) {
                        Icons.AutoMirrored.Filled.KeyboardArrowRight
                    } else {
                        Icons.Default.KeyboardArrowDown
                    },
                    contentDescription = if (isCollapsed) "Expand" else "Collapse",
                    tint = colors.textSecondary,
                )
            }
            Text(
                text = title,
                style = type.sectionHeader.copy(color = colors.textPrimary),
            )
            Text(
                text = "($count)",
                style = type.meta.copy(color = colors.textTertiary),
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.s1),
        ) {
            OSIconButton(
                onClick = onAddTaskClick,
                contentDescription = "Add task in section $title",
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = colors.accent,
                )
            }
            if (onOptionsClick != null) {
                OSIconButton(
                    onClick = onOptionsClick,
                    contentDescription = "Section options",
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = null,
                        tint = colors.textSecondary,
                    )
                }
            }
        }
    }
}
