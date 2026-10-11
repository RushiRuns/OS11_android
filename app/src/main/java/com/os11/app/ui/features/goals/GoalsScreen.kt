package com.os11.app.ui.features.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.os11.app.service.goal.GoalItem
import com.os11.app.service.project.ProjectItem
import com.os11.app.service.task.TaskItem
import com.os11.app.ui.components.OSButton
import com.os11.app.ui.components.OSButtonVariant
import com.os11.app.ui.components.OSCheckbox
import com.os11.app.ui.components.OSChip
import com.os11.app.ui.components.OSEmptyState
import com.os11.app.ui.components.OSIconButton
import com.os11.app.ui.components.OSProgressBar
import com.os11.app.ui.components.OSSheet
import com.os11.app.ui.components.OSTextField
import com.os11.app.ui.theme.OS11Theme

@Composable
fun GoalsScreen(modifier: Modifier = Modifier, viewModel: GoalsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surfaceBase),
    ) {
        // 1. Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s4, vertical = spacing.s3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "Goals",
                    style = type.subViewTitle.copy(color = colors.textPrimary),
                )
                Text(
                    text = "High-level objectives, linked tasks & streaks",
                    style = type.meta.copy(color = colors.textSecondary),
                )
            }
            OSIconButton(
                onClick = viewModel::openCreateSheet,
                contentDescription = "New Goal",
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = colors.accent,
                )
            }
        }

        // 2. Module Disabled Notice
        if (!state.isGoalsEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.s4, vertical = spacing.s2)
                    .background(colors.surfaceRaised, RoundedCornerShape(OS11Theme.radius.md))
                    .border(1.dp, colors.borderSubtle, RoundedCornerShape(OS11Theme.radius.md))
                    .padding(spacing.s4),
            ) {
                Text(
                    text = "The Goals module is disabled. Enable it in Settings to track high-level targets.",
                    style = type.body.copy(color = colors.textSecondary),
                )
            }
        }

        // 3. Goals List or Empty State
        if (state.goals.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(spacing.s4),
                contentAlignment = Alignment.Center,
            ) {
                OSEmptyState(
                    title = "No Goals Yet",
                    description = "Create a goal to link projects, tasks, and maintain streaks.",
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = spacing.s4),
                verticalArrangement = Arrangement.spacedBy(spacing.s3),
            ) {
                items(state.goals, key = { it.id }) { goal ->
                    GoalCard(
                        goal = goal,
                        onClick = { viewModel.selectGoal(goal) },
                        onLogProgress = { viewModel.recordProgress(goal.id) },
                    )
                }
            }
        }
    }

    // Goal Detail Modal Sheet
    state.selectedGoalForDetail?.let { goal ->
        GoalDetailSheet(
            goal = goal,
            availableTasks = state.availableTasks,
            availableProjects = state.availableProjects,
            onDismiss = { viewModel.selectGoal(null) },
            onRecordProgress = { inc -> viewModel.recordProgress(goal.id, inc) },
            onLinkTask = { taskId -> viewModel.linkTask(goal.id, taskId) },
            onUnlinkTask = { taskId -> viewModel.unlinkTask(goal.id, taskId) },
            onLinkProject = { projectId -> viewModel.linkProject(goal.id, projectId) },
            onUnlinkProject = { projectId -> viewModel.unlinkProject(goal.id, projectId) },
            onDelete = { viewModel.deleteGoal(goal.id) },
        )
    }

    // Create Goal Sheet
    if (state.isCreateSheetOpen) {
        CreateGoalSheet(
            onDismiss = viewModel::closeCreateSheet,
            onCreate = viewModel::createGoal,
        )
    }
}

@Composable
private fun GoalCard(goal: GoalItem, onClick: () -> Unit, onLogProgress: () -> Unit) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OS11Theme.radius.lg))
            .background(colors.surfaceRaised)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(OS11Theme.radius.lg))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(spacing.s4),
    ) {
        // Title & Badges Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Star,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(spacing.s2))
                Text(
                    text = goal.title,
                    style = type.panelTitle.copy(color = colors.textPrimary),
                    maxLines = 1,
                )
            }

            // Streak Badge
            if (goal.streakCount > 0) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(colors.accent.copy(alpha = 0.15f))
                        .padding(horizontal = spacing.s2, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${goal.streakCount}d streak",
                        style = type.meta.copy(color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 11.sp),
                    )
                }
            }
        }

        if (!goal.description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(spacing.s1))
            Text(
                text = goal.description,
                style = type.meta.copy(color = colors.textSecondary),
                maxLines = 2,
            )
        }

        Spacer(modifier = Modifier.height(spacing.s3))

        // Progress Bar
        val progress = (goal.progressPercent / 100f).coerceIn(0f, 1f)
        OSProgressBar(
            progress = progress,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(spacing.s2))

        // Footer Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val progressText = if (goal.linkedTasksCount > 0) {
                "${goal.completedTasksCount}/${goal.linkedTasksCount} tasks"
            } else {
                "${goal.currentValue.toInt()} / ${goal.targetValue.toInt()} (${goal.progressPercent.toInt()}%)"
            }

            Text(
                text = progressText,
                style = type.meta.copy(color = colors.textSecondary),
            )

            // One-Thumb Quick Log Progress Button
            OSButton(
                onClick = onLogProgress,
                variant = OSButtonVariant.Secondary,
            ) {
                Text("+ Progress")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalDetailSheet(
    goal: GoalItem,
    availableTasks: List<TaskItem>,
    availableProjects: List<ProjectItem>,
    onDismiss: () -> Unit,
    onRecordProgress: (Double) -> Unit,
    onLinkTask: (String) -> Unit,
    onUnlinkTask: (String) -> Unit,
    onLinkProject: (String) -> Unit,
    onUnlinkProject: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    var isLinkTaskSheetOpen by remember { mutableStateOf(false) }
    var isLinkProjectSheetOpen by remember { mutableStateOf(false) }

    OSSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.s4),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.title,
                        style = type.subViewTitle.copy(color = colors.textPrimary),
                    )
                    Text(
                        text = "${goal.goalType.replaceFirstChar {
                            it.uppercase()
                        }} Goal • Streak: ${goal.streakCount} days",
                        style = type.meta.copy(color = colors.textSecondary),
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.s3))

            // Progress Quick Stepper
            Text(
                text = "Log Progress",
                style = type.sectionHeader.copy(color = colors.textPrimary),
            )
            Spacer(modifier = Modifier.height(spacing.s1))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                OSButton(
                    onClick = { onRecordProgress(1.0) },
                    variant = OSButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("+1")
                }
                OSButton(
                    onClick = { onRecordProgress(5.0) },
                    variant = OSButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("+5")
                }
                OSButton(
                    onClick = { onRecordProgress(10.0) },
                    variant = OSButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("+10")
                }
            }

            Spacer(modifier = Modifier.height(spacing.s4))

            // Linked Items Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                OSButton(
                    onClick = { isLinkTaskSheetOpen = true },
                    variant = OSButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("+ Link Task (${goal.linkedTaskIds.size})")
                }
                OSButton(
                    onClick = { isLinkProjectSheetOpen = true },
                    variant = OSButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("+ Link Project (${goal.linkedProjectIds.size})")
                }
            }

            Spacer(modifier = Modifier.height(spacing.s4))

            // Delete Goal Button
            OSButton(
                onClick = {
                    onDelete()
                    onDismiss()
                },
                variant = OSButtonVariant.Danger,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(imageVector = Icons.Outlined.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(spacing.s2))
                Text("Delete Goal")
            }
        }
    }

    if (isLinkTaskSheetOpen) {
        LinkResourcePickerSheet(
            title = "Link Task",
            items = availableTasks.map { it.id to it.title },
            linkedIds = goal.linkedTaskIds.toSet(),
            onDismiss = { isLinkTaskSheetOpen = false },
            onToggle = { id ->
                if (id in goal.linkedTaskIds) onUnlinkTask(id) else onLinkTask(id)
            },
        )
    }

    if (isLinkProjectSheetOpen) {
        LinkResourcePickerSheet(
            title = "Link Project",
            items = availableProjects.map { it.id to it.name },
            linkedIds = goal.linkedProjectIds.toSet(),
            onDismiss = { isLinkProjectSheetOpen = false },
            onToggle = { id ->
                if (id in goal.linkedProjectIds) onUnlinkProject(id) else onLinkProject(id)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinkResourcePickerSheet(
    title: String,
    items: List<Pair<String, String>>,
    linkedIds: Set<String>,
    onDismiss: () -> Unit,
    onToggle: (String) -> Unit,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    OSSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.s4),
        ) {
            Text(
                text = title,
                style = type.subViewTitle.copy(color = colors.textPrimary),
            )
            Spacer(modifier = Modifier.height(spacing.s3))

            if (items.isEmpty()) {
                Text(
                    text = "No items available to link.",
                    style = type.meta.copy(color = colors.textSecondary),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing.s2),
                ) {
                    items(items, key = { it.first }) { (id, name) ->
                        val isLinked = id in linkedIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(OS11Theme.radius.sm))
                                .clickable(role = Role.Checkbox) { onToggle(id) }
                                .padding(vertical = spacing.s2),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OSCheckbox(
                                checked = isLinked,
                                onCheckedChange = { onToggle(id) },
                            )
                            Spacer(modifier = Modifier.width(spacing.s2))
                            Text(
                                text = name,
                                style = type.body.copy(color = colors.textPrimary),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.s3))
            OSButton(
                onClick = onDismiss,
                variant = OSButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Done")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateGoalSheet(onDismiss: () -> Unit, onCreate: (String, String, String, String?, Double) -> Unit) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("target") }
    var targetValue by remember { mutableDoubleStateOf(100.0) }

    OSSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.s4),
        ) {
            Text(
                text = "New Goal",
                style = type.subViewTitle.copy(color = colors.textPrimary),
            )
            Spacer(modifier = Modifier.height(spacing.s3))

            OSTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = "Goal title...",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(spacing.s2))

            OSTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = "Description (optional)...",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(spacing.s3))

            // Goal Type Selector Chips
            Text(
                text = "Type",
                style = type.sectionHeader.copy(color = colors.textPrimary),
            )
            Spacer(modifier = Modifier.height(spacing.s1))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                listOf(
                    "target" to "Target",
                    "habit" to "Habit",
                    "milestone" to "Milestone",
                ).forEach { (typeKey, label) ->
                    val isSelected = selectedType == typeKey
                    OSChip(
                        label = label,
                        selected = isSelected,
                        onClick = { selectedType = typeKey },
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.s4))

            OSButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onCreate(title.trim(), description.trim(), selectedType, null, targetValue)
                    }
                },
                variant = OSButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Create Goal")
            }
        }
    }
}
