package com.os11.app.ui.features.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.os11.app.service.tag.TagItem
import com.os11.app.ui.components.OSButton
import com.os11.app.ui.components.OSButtonVariant
import com.os11.app.ui.components.OSChip
import com.os11.app.ui.components.OSIconButton
import com.os11.app.ui.components.OSSheet
import com.os11.app.ui.components.OSTagDot
import com.os11.app.ui.components.OSTagDotVariant
import com.os11.app.ui.components.OSTagShape
import com.os11.app.ui.components.OSTextField
import com.os11.app.ui.navigation.OS11Route
import com.os11.app.ui.theme.OS11Theme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Suppress("CyclomaticComplexMethod")
@Composable
fun BrowseScreen(
    onNavigate: (OS11Route) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: BrowseViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val radius = OS11Theme.radius

    var newAreaName by remember { mutableStateOf("") }
    var newProjectName by remember { mutableStateOf("") }
    var newProjectAreaId by remember { mutableStateOf<String?>(null) }
    var newTagName by remember { mutableStateOf("") }
    var newTagColor by remember { mutableStateOf<String?>("blue") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surfaceBase)
            .padding(spacing.s4),
        verticalArrangement = Arrangement.spacedBy(spacing.s4),
    ) {
        // 1. Smart Lists Grid
        item(key = "smart_lists_header") {
            Text(
                text = "Smart Lists",
                style = type.sectionHeader.copy(color = colors.textPrimary),
            )
        }

        item(key = "smart_lists_content") {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.s2)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                ) {
                    SmartListCard(
                        title = "Important",
                        count = state.counts.importantCount,
                        icon = Icons.Filled.Star,
                        iconTint = colors.priorityMedium,
                        onClick = { onNavigate(OS11Route.Important) },
                        modifier = Modifier.weight(1f),
                    )
                    SmartListCard(
                        title = "My Day",
                        count = state.counts.myDayCount,
                        icon = Icons.Outlined.Home,
                        iconTint = colors.accent,
                        onClick = { onNavigate(OS11Route.MyDay) },
                        modifier = Modifier.weight(1f),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                ) {
                    SmartListCard(
                        title = "Inbox",
                        count = state.counts.inboxCount,
                        icon = Icons.Outlined.MailOutline,
                        iconTint = colors.textSecondary,
                        onClick = { onNavigate(OS11Route.Inbox) },
                        modifier = Modifier.weight(1f),
                    )
                    SmartListCard(
                        title = "Planned",
                        count = state.counts.plannedCount,
                        icon = Icons.Outlined.DateRange,
                        iconTint = colors.textSecondary,
                        onClick = { onNavigate(OS11Route.Planned) },
                        modifier = Modifier.weight(1f),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                ) {
                    SmartListCard(
                        title = "All Tasks",
                        count = state.counts.inboxCount + state.counts.plannedCount,
                        icon = Icons.AutoMirrored.Outlined.List,
                        iconTint = colors.textSecondary,
                        onClick = { onNavigate(OS11Route.AllTasks) },
                        modifier = Modifier.weight(1f),
                    )
                    SmartListCard(
                        title = "Completed",
                        count = state.counts.completedCount,
                        icon = Icons.Outlined.CheckCircle,
                        iconTint = colors.accent,
                        onClick = { onNavigate(OS11Route.Completed) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // 2. Modules Section
        item(key = "modules_header") {
            Text(
                text = "Modules",
                style = type.sectionHeader.copy(color = colors.textPrimary),
            )
        }

        item(key = "modules_content") {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.s2)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                ) {
                    ModuleBadgeChip(
                        title = "Anytime",
                        count = state.counts.anytimeCount,
                        onClick = { onNavigate(OS11Route.BucketList("anytime")) },
                    )
                    ModuleBadgeChip(
                        title = "Someday",
                        count = state.counts.somedayCount,
                        onClick = { onNavigate(OS11Route.BucketList("someday")) },
                    )
                    ModuleBadgeChip(
                        title = "Waiting For",
                        count = state.counts.waitingForCount,
                        onClick = { onNavigate(OS11Route.BucketList("waiting")) },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                ) {
                    OSChip(
                        label = "Day Plan",
                        onClick = { onNavigate(OS11Route.DayPlan) },
                    )
                    OSChip(
                        label = "Goals",
                        onClick = { onNavigate(OS11Route.Goals) },
                    )
                    OSChip(
                        label = "Pomodoro",
                        onClick = { onNavigate(OS11Route.Pomodoro) },
                    )
                }
            }
        }

        // 3. Areas & Projects Tree
        item(key = "areas_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Areas & Projects",
                    style = type.sectionHeader.copy(color = colors.textPrimary),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.s1)) {
                    OSIconButton(
                        onClick = viewModel::openCreateAreaDialog,
                        contentDescription = "New Area",
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = colors.accent,
                        )
                    }
                    OSButton(
                        onClick = viewModel::openCreateProjectDialog,
                        variant = OSButtonVariant.Secondary,
                    ) {
                        Text("+ Project")
                    }
                }
            }
        }

        // Areas list
        items(
            items = state.areasWithProjects,
            key = { "area_${it.area.id}" },
        ) { areaWithProjects ->
            AreaCard(
                areaWithProjects = areaWithProjects,
                projectTaskCounts = state.projectTaskCounts,
                onAreaClick = { onNavigate(OS11Route.AreaOverview(areaWithProjects.area.id)) },
                onProjectClick = { projectId -> onNavigate(OS11Route.ProjectDetail(projectId)) },
            )
        }

        // Loose projects (projects without an area)
        if (state.looseProjects.isNotEmpty()) {
            item(key = "loose_projects_header") {
                Text(
                    text = "No Area",
                    style = type.meta.copy(color = colors.textSecondary),
                    modifier = Modifier.padding(top = spacing.s2),
                )
            }
            items(
                items = state.looseProjects,
                key = { "loose_proj_${it.id}" },
            ) { project ->
                val count = state.projectTaskCounts[project.id] ?: 0
                ProjectRow(
                    project = project,
                    taskCount = count,
                    onClick = { onNavigate(OS11Route.ProjectDetail(project.id)) },
                )
            }
        }

        // 4. Tags Section
        item(key = "tags_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Tags",
                    style = type.sectionHeader.copy(color = colors.textPrimary),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.s1)) {
                    OSIconButton(
                        onClick = viewModel::openCreateTagDialog,
                        contentDescription = "New Tag",
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = colors.accent,
                        )
                    }
                    OSButton(
                        onClick = { onNavigate(OS11Route.TagManager) },
                        variant = OSButtonVariant.Secondary,
                    ) {
                        Text("Manage")
                    }
                }
            }
        }

        item(key = "tags_content") {
            if (state.tags.isEmpty()) {
                Text(
                    text = "No tags created yet",
                    style = type.meta.copy(color = colors.textSecondary),
                    modifier = Modifier.padding(vertical = spacing.s2),
                )
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                    verticalArrangement = Arrangement.spacedBy(spacing.s2),
                ) {
                    state.tags.forEach { tag ->
                        BrowseTagChip(
                            tag = tag,
                            onClick = { onNavigate(OS11Route.TagDetail(tag.id, tag.name)) },
                        )
                    }
                }
            }
        }
    }

    // Create Area Sheet
    if (state.showCreateAreaDialog) {
        OSSheet(onDismissRequest = viewModel::closeCreateAreaDialog) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.s4),
            ) {
                Text(
                    text = "New Area",
                    style = type.subViewTitle.copy(color = colors.textPrimary),
                )
                Spacer(modifier = Modifier.height(spacing.s3))
                OSTextField(
                    value = newAreaName,
                    onValueChange = { newAreaName = it },
                    placeholder = "Area name...",
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(spacing.s4))
                OSButton(
                    onClick = {
                        if (newAreaName.isNotBlank()) {
                            viewModel.createArea(newAreaName.trim())
                            newAreaName = ""
                        }
                    },
                    variant = OSButtonVariant.Primary,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Create Area")
                }
            }
        }
    }

    // Create Project Sheet
    if (state.showCreateProjectDialog) {
        OSSheet(onDismissRequest = viewModel::closeCreateProjectDialog) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.s4),
            ) {
                Text(
                    text = "New Project",
                    style = type.subViewTitle.copy(color = colors.textPrimary),
                )
                Spacer(modifier = Modifier.height(spacing.s3))
                OSTextField(
                    value = newProjectName,
                    onValueChange = { newProjectName = it },
                    placeholder = "Project name...",
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(spacing.s3))
                Text(
                    text = "Assign to Area (optional):",
                    style = type.meta.copy(color = colors.textSecondary),
                )
                Spacer(modifier = Modifier.height(spacing.s1))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                ) {
                    OSChip(
                        label = "None",
                        selected = newProjectAreaId == null,
                        onClick = { newProjectAreaId = null },
                    )
                    state.areasWithProjects.forEach { areaItem ->
                        OSChip(
                            label = areaItem.area.name,
                            selected = newProjectAreaId == areaItem.area.id,
                            onClick = { newProjectAreaId = areaItem.area.id },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(spacing.s4))
                OSButton(
                    onClick = {
                        if (newProjectName.isNotBlank()) {
                            viewModel.createProject(newProjectName.trim(), newProjectAreaId)
                            newProjectName = ""
                            newProjectAreaId = null
                        }
                    },
                    variant = OSButtonVariant.Primary,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Create Project")
                }
            }
        }
    }

    // Create Tag Sheet
    if (state.showCreateTagDialog) {
        OSSheet(onDismissRequest = viewModel::closeCreateTagDialog) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.s4),
            ) {
                Text(
                    text = "New Tag",
                    style = type.subViewTitle.copy(color = colors.textPrimary),
                )
                Spacer(modifier = Modifier.height(spacing.s3))
                OSTextField(
                    value = newTagName,
                    onValueChange = { newTagName = it },
                    placeholder = "Tag name (e.g. work/client)...",
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(spacing.s3))
                Text(
                    text = "Tag Color",
                    style = type.meta.copy(color = colors.textSecondary),
                )
                Spacer(modifier = Modifier.height(spacing.s2))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                    verticalArrangement = Arrangement.spacedBy(spacing.s2),
                ) {
                    colors.tags.allNames.forEach { colorName ->
                        val isSelected = newTagColor == colorName
                        val col = colors.tags.fromNameOrHex(colorName)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(col)
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) colors.textPrimary else Color.Transparent,
                                    shape = CircleShape,
                                )
                                .clickable { newTagColor = colorName },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(spacing.s4))
                OSButton(
                    onClick = {
                        if (newTagName.isNotBlank()) {
                            viewModel.createTag(newTagName.trim(), newTagColor)
                            newTagName = ""
                            newTagColor = "blue"
                        }
                    },
                    variant = OSButtonVariant.Primary,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Create Tag")
                }
            }
        }
    }
}

@Composable
private fun SmartListCard(
    title: String,
    count: Int,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val radius = OS11Theme.radius

    Row(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(radius.md))
            .background(colors.surfaceRaised)
            .border(spacing.px, colors.borderSubtle, RoundedCornerShape(radius.md))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = title,
                style = type.body.copy(color = colors.textPrimary),
            )
        }
        Text(
            text = count.toString(),
            style = type.meta.copy(color = colors.textSecondary),
        )
    }
}

@Composable
private fun ModuleBadgeChip(title: String, count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OSChip(
        label = "$title ($count)",
        selected = false,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun AreaCard(
    areaWithProjects: AreaWithProjects,
    projectTaskCounts: Map<String, Int>,
    onAreaClick: () -> Unit,
    onProjectClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val radius = OS11Theme.radius

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radius.md))
            .background(colors.surfaceRaised)
            .border(spacing.px, colors.borderSubtle, RoundedCornerShape(radius.md))
            .padding(spacing.s3),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onAreaClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(colors.accent),
                )
                Text(
                    text = areaWithProjects.area.name,
                    style = type.subViewTitle.copy(color = colors.textPrimary),
                )
            }
            Text(
                text = "${areaWithProjects.taskCount} tasks",
                style = type.meta.copy(color = colors.textSecondary),
            )
        }

        if (areaWithProjects.projects.isNotEmpty()) {
            Spacer(modifier = Modifier.height(spacing.s2))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.s1),
            ) {
                areaWithProjects.projects.forEach { project ->
                    val count = projectTaskCounts[project.id] ?: 0
                    ProjectRow(
                        project = project,
                        taskCount = count,
                        onClick = { onProjectClick(project.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectRow(
    project: com.os11.app.service.project.ProjectItem,
    taskCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val radius = OS11Theme.radius

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radius.sm))
            .background(colors.surfaceBase)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = spacing.s3, vertical = spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(colors.priorityMedium),
            )
            Text(
                text = project.name,
                style = type.body.copy(color = colors.textPrimary),
            )
        }
        Text(
            text = "$taskCount",
            style = type.meta.copy(color = colors.textTertiary),
        )
    }
}

@Composable
private fun BrowseTagChip(tag: TagItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val radius = OS11Theme.radius
    val tagColor = colors.tags.fromNameOrHex(tag.color)
    val shape = OSTagShape.fromTag(tag.name)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(radius.full))
            .background(colors.surfaceRaised)
            .border(spacing.px, colors.borderSubtle, RoundedCornerShape(radius.full))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = spacing.s3, vertical = spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.s2),
    ) {
        OSTagDot(
            color = tagColor,
            shape = shape,
            variant = OSTagDotVariant.PillDot,
            contentDescriptionText = "Tag ${tag.name}",
        )
        Text(
            text = tag.name,
            style = type.body.copy(color = colors.textPrimary),
        )
        Text(
            text = "${tag.taskCount}",
            style = type.meta.copy(color = colors.textTertiary),
        )
    }
}
