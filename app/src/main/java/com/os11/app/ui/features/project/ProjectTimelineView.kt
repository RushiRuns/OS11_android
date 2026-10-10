package com.os11.app.ui.features.project

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.os11.app.service.project.MilestoneItem
import com.os11.app.service.project.TaskDependencyItem
import com.os11.app.ui.components.OSButton
import com.os11.app.ui.components.OSChip
import com.os11.app.ui.components.OSEmptyState
import com.os11.app.ui.components.OSIconButton
import com.os11.app.ui.features.task.TaskUiModel
import com.os11.app.ui.theme.OS11Theme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private const val SHORT_TIMELINE_DAYS = 14
private const val LONG_TIMELINE_DAYS = 28

@Composable
@Suppress("LongParameterList")
fun ProjectTimelineView(
    tasks: List<TaskUiModel>,
    milestones: List<MilestoneItem>,
    dependencies: List<TaskDependencyItem>,
    zoomWeeks: Boolean,
    onToggleZoom: () -> Unit,
    onOpenMilestones: () -> Unit,
    onOpenDependencies: (TaskUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    val tasksWithDates = remember(tasks) { tasks.filter { it.dueDate != null } }
    val tasksWithoutDates = remember(tasks) { tasks.filter { it.dueDate == null } }

    val daysCount = if (zoomWeeks) LONG_TIMELINE_DAYS else SHORT_TIMELINE_DAYS
    val dayWidth = if (zoomWeeks) 32.dp else 48.dp
    val today = remember { LocalDate.now() }
    val dateList = remember(today, daysCount) {
        (0 until daysCount).map { today.plusDays(it.toLong()) }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Controls bar: Zoom toggle + Milestones button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s4, vertical = spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            OSChip(
                label = if (zoomWeeks) "Scale: 4 Weeks" else "Scale: 2 Weeks",
                selected = zoomWeeks,
                onClick = onToggleZoom,
            )

            OSButton(onClick = onOpenMilestones) {
                Text("Milestones (${milestones.size})")
            }
        }

        if (tasks.isEmpty() && milestones.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(spacing.s4),
                contentAlignment = Alignment.Center,
            ) {
                OSEmptyState(
                    title = "Timeline is empty",
                    description = "Schedule due dates on tasks to visualize deliverables along the timeline.",
                )
            }
            return
        }

        val horizontalScrollState = rememberScrollState()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(spacing.s2),
        ) {
            // 1. Date header strip (read-only scrollable)
            item(key = "timeline_date_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(horizontalScrollState)
                        .padding(horizontal = spacing.s4),
                ) {
                    Spacer(modifier = Modifier.width(140.dp))
                    dateList.forEach { date ->
                        Box(
                            modifier = Modifier
                                .width(dayWidth)
                                .padding(vertical = spacing.s1),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = date.format(DateTimeFormatter.ofPattern("EEE")),
                                    style = type.meta.copy(color = colors.textTertiary),
                                )
                                Text(
                                    text = "${date.dayOfMonth}",
                                    style = type.sectionLabel.copy(
                                        color = if (date == today) colors.accent else colors.textSecondary,
                                    ),
                                )
                            }
                        }
                    }
                }
            }

            // 2. Milestones Row
            if (milestones.isNotEmpty()) {
                item(key = "timeline_milestones_row") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(horizontalScrollState)
                            .padding(horizontal = spacing.s4, vertical = spacing.s1),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Milestones",
                            style = type.sectionHeader.copy(color = colors.accent),
                            modifier = Modifier.width(140.dp),
                        )
                        dateList.forEach { date ->
                            val dateIso = date.toString()
                            val matching = milestones.filter { it.dueDate == dateIso }
                            Box(
                                modifier = Modifier
                                    .width(dayWidth)
                                    .height(28.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (matching.isNotEmpty()) {
                                    Icon(
                                        imageVector = if (matching.all { it.isCompleted }) {
                                            Icons.Default.CheckCircle
                                        } else {
                                            Icons.Default.Info
                                        },
                                        contentDescription = "Milestone on $dateIso",
                                        tint = colors.accent,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Tasks with Due Dates
            items(
                items = tasksWithDates,
                key = { "timeline_task_${it.id}" },
                contentType = { "timeline_row" },
            ) { task ->
                TimelineTaskRow(
                    task = task,
                    dateList = dateList,
                    dayWidth = dayWidth,
                    dependencies = dependencies,
                    horizontalScrollState = horizontalScrollState,
                    onOpenDependencies = { onOpenDependencies(task) },
                )
            }

            // 4. Tasks without Due Dates
            if (tasksWithoutDates.isNotEmpty()) {
                item(key = "timeline_undated_header") {
                    Text(
                        text = "Unscheduled Tasks (${tasksWithoutDates.size})",
                        style = type.sectionHeader.copy(color = colors.textSecondary),
                        modifier = Modifier.padding(horizontal = spacing.s4, vertical = spacing.s2),
                    )
                }
                items(
                    items = tasksWithoutDates,
                    key = { "undated_task_${it.id}" },
                    contentType = { "undated_row" },
                ) { task ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = spacing.s4, vertical = spacing.s1)
                            .clip(RoundedCornerShape(OS11Theme.radius.md))
                            .background(colors.surfaceRaised)
                            .padding(spacing.s2),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = task.title,
                            style = type.body.copy(color = colors.textPrimary),
                        )
                        OSIconButton(
                            onClick = { onOpenDependencies(task) },
                            contentDescription = "Manage dependencies for ${task.title}",
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.List,
                                contentDescription = null,
                                tint = colors.textSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineTaskRow(
    task: TaskUiModel,
    dateList: List<LocalDate>,
    dayWidth: Dp,
    dependencies: List<TaskDependencyItem>,
    horizontalScrollState: androidx.compose.foundation.ScrollState,
    onOpenDependencies: () -> Unit,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    val taskDate = remember(task.dueDate) {
        task.dueDate?.let {
            try {
                LocalDate.parse(it)
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }

    val taskDependencies = remember(dependencies, task.id) {
        dependencies.filter { it.taskId == task.id }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(horizontalScrollState)
            .padding(horizontal = spacing.s4, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.width(140.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = task.title,
                style = type.taskTitle.copy(color = colors.textPrimary),
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            if (taskDependencies.isNotEmpty()) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = "Has dependencies",
                    tint = colors.accent,
                    modifier = Modifier
                        .size(14.dp)
                        .clickable(onClick = onOpenDependencies),
                )
            }
        }

        dateList.forEach { date ->
            val isDueOnThisDate = taskDate == date
            Box(
                modifier = Modifier
                    .width(dayWidth)
                    .height(24.dp)
                    .padding(horizontal = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (isDueOnThisDate) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(OS11Theme.radius.xs))
                            .background(if (task.isCompleted) colors.surfaceRaised else colors.accent),
                    )
                }
            }
        }
    }
}
