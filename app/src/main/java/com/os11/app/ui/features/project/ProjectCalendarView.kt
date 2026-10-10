package com.os11.app.ui.features.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.os11.app.ui.components.OSEmptyState
import com.os11.app.ui.components.calendar.WeekStrip
import com.os11.app.ui.features.task.TaskRow
import com.os11.app.ui.features.task.TaskUiModel
import com.os11.app.ui.theme.OS11Theme
import java.time.LocalDate

@Composable
@Suppress("LongParameterList")
fun ProjectCalendarView(
    tasks: List<TaskUiModel>,
    referenceDate: LocalDate,
    selectedDate: LocalDate?,
    datesWithTasks: Set<String>,
    onDateSelected: (LocalDate?) -> Unit,
    onWeekOffsetChange: (Int) -> Unit,
    onCompletedChange: (taskId: String, isCompleted: Boolean) -> Unit,
    onTaskClick: (TaskUiModel) -> Unit,
    onTaskLongClick: (TaskUiModel) -> Unit,
    onToggleStar: (taskId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    val filteredTasks = if (selectedDate != null) {
        val dateString = selectedDate.toString()
        tasks.filter { it.dueDate == dateString }
    } else {
        tasks.filter { it.dueDate != null }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // WeekStrip calendar strip component
        WeekStrip(
            referenceDate = referenceDate,
            selectedDate = selectedDate,
            datesWithTasks = datesWithTasks,
            onDateSelected = onDateSelected,
            onWeekOffsetChange = onWeekOffsetChange,
        )

        if (filteredTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(spacing.s4),
                contentAlignment = Alignment.Center,
            ) {
                OSEmptyState(
                    title = if (selectedDate != null) "No tasks for this date" else "No scheduled tasks",
                    description = "Tasks in this project with due dates will appear here.",
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(spacing.s1),
            ) {
                if (selectedDate != null) {
                    item(key = "calendar_date_header") {
                        Text(
                            text = "Due on $selectedDate",
                            style = type.sectionHeader.copy(color = colors.textSecondary),
                            modifier = Modifier.padding(horizontal = spacing.s4, vertical = spacing.s2),
                        )
                    }
                }
                items(
                    items = filteredTasks,
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
    }
}
