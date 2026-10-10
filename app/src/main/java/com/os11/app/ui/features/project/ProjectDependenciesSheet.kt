package com.os11.app.ui.features.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.unit.dp
import com.os11.app.service.project.TaskDependencyItem
import com.os11.app.ui.components.OSButton
import com.os11.app.ui.components.OSIconButton
import com.os11.app.ui.components.OSSheet
import com.os11.app.ui.features.task.TaskUiModel
import com.os11.app.ui.theme.OS11Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList")
fun ProjectDependenciesSheet(
    task: TaskUiModel,
    allTasks: List<TaskUiModel>,
    dependencies: List<TaskDependencyItem>,
    errorMessage: String?,
    onAddDependency: (taskId: String, dependsOnId: String) -> Unit,
    onRemoveDependency: (taskId: String, dependsOnId: String) -> Unit,
    onClearErrorMessage: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    val taskMap = remember(allTasks) { allTasks.associateBy { it.id } }
    val blockedByList = remember(dependencies, task.id) {
        dependencies.filter { it.taskId == task.id }
    }
    val blocksList = remember(dependencies, task.id) {
        dependencies.filter { it.dependsOnId == task.id }
    }

    val candidates = remember(allTasks, task.id, blockedByList) {
        val existingDependsOn = blockedByList.map { it.dependsOnId }.toSet()
        allTasks.filter { it.id != task.id && it.id !in existingDependsOn }
    }

    var pickerExpanded by remember { mutableStateOf(false) }

    OSSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.s4),
        ) {
            Text(
                text = "Task Dependencies",
                style = type.panelTitle.copy(color = colors.textPrimary),
            )
            Text(
                text = task.title,
                style = type.taskTitle.copy(color = colors.textSecondary),
            )

            // Cycle error alert if any
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(spacing.s2))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(OS11Theme.radius.md))
                        .background(colors.danger.copy(alpha = 0.15f))
                        .padding(spacing.s3),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = colors.danger,
                    )
                    Text(
                        text = errorMessage,
                        style = type.body.copy(color = colors.danger),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.s3))

            // Add dependency picker button
            Box {
                OSButton(
                    onClick = {
                        onClearErrorMessage()
                        pickerExpanded = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("+ Add Dependency (Depends On...)")
                }

                DropdownMenu(
                    expanded = pickerExpanded,
                    onDismissRequest = { pickerExpanded = false },
                ) {
                    if (candidates.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No other tasks available") },
                            onClick = { pickerExpanded = false },
                        )
                    } else {
                        candidates.forEach { candidate ->
                            DropdownMenuItem(
                                text = { Text(candidate.title) },
                                onClick = {
                                    pickerExpanded = false
                                    onAddDependency(task.id, candidate.id)
                                },
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.s4))

            // Depends on (Blocked By) list
            Text(
                text = "Blocked By (${blockedByList.size})",
                style = type.sectionHeader.copy(color = colors.textPrimary),
            )
            Spacer(modifier = Modifier.height(spacing.s1))

            if (blockedByList.isEmpty()) {
                Text(
                    text = "This task does not depend on any other task.",
                    style = type.meta.copy(color = colors.textTertiary),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing.s1),
                ) {
                    items(items = blockedByList) { dep ->
                        val depTitle = taskMap[dep.dependsOnId]?.title ?: "Unknown Task (${dep.dependsOnId})"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(OS11Theme.radius.md))
                                .background(colors.surfaceRaised)
                                .padding(horizontal = spacing.s3, vertical = spacing.s1),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = depTitle,
                                style = type.body.copy(color = colors.textPrimary),
                                modifier = Modifier.weight(1f),
                            )
                            OSIconButton(
                                onClick = { onRemoveDependency(dep.taskId, dep.dependsOnId) },
                                contentDescription = "Remove dependency",
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = colors.textTertiary,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.s3))

            // Blocks list
            Text(
                text = "Blocks (${blocksList.size})",
                style = type.sectionHeader.copy(color = colors.textPrimary),
            )
            Spacer(modifier = Modifier.height(spacing.s1))

            if (blocksList.isEmpty()) {
                Text(
                    text = "No other tasks are waiting on this task.",
                    style = type.meta.copy(color = colors.textTertiary),
                )
            } else {
                blocksList.forEach { dep ->
                    val blockerTitle = taskMap[dep.taskId]?.title ?: "Unknown Task (${dep.taskId})"
                    Text(
                        text = "• $blockerTitle",
                        style = type.body.copy(color = colors.textSecondary),
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.s4))
            OSButton(
                onClick = onDismissRequest,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Done")
            }
        }
    }
}
