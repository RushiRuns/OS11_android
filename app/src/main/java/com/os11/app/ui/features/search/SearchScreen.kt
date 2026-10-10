package com.os11.app.ui.features.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.os11.app.service.task.TaskItem
import com.os11.app.ui.components.OSCheckbox
import com.os11.app.ui.components.OSChip
import com.os11.app.ui.components.OSEmptyState
import com.os11.app.ui.components.OSIconButton
import com.os11.app.ui.components.OSProgressRing
import com.os11.app.ui.components.OSSearchField
import com.os11.app.ui.features.quickadd.QuickAddSheet
import com.os11.app.ui.features.task.TaskDetailSheet
import com.os11.app.ui.navigation.OS11Route
import com.os11.app.ui.theme.OS11Theme

/**
 * Search screen implementing FTS5 prefix search, grouped results (Actions and Tasks)
 * with contextual query snippets, and device-local recent searches.
 */
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onNavigate: (OS11Route) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing

    var showQuickAddSheet by remember { mutableStateOf(false) }
    var selectedTaskForDetail by remember { mutableStateOf<TaskItem?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surfaceBase)
            .padding(horizontal = spacing.s4),
    ) {
        Spacer(modifier = Modifier.height(spacing.s2))

        // Top Search Bar with Back Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
        ) {
            OSIconButton(
                onClick = onNavigateBack,
                contentDescription = "Navigate back",
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = colors.textSecondary,
                )
            }

            OSSearchField(
                query = state.query,
                onQueryChange = { viewModel.onQueryChanged(it) },
                onSearch = { /* Instant debounced search */ },
                placeholder = "Search tasks, notes, actions...",
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(spacing.s3))

        // Main Content Area
        Box(modifier = Modifier.weight(1f)) {
            when {
                state.query.isBlank() -> {
                    RecentSearchesView(
                        recentSearches = state.recentSearches,
                        onSelectRecent = { viewModel.onRecentSelected(it) },
                        onRemoveRecent = { viewModel.removeRecentSearch(it) },
                        onClearAll = { viewModel.clearRecentSearches() },
                        onNavigate = onNavigate,
                    )
                }
                state.isSearching && state.tasks.isEmpty() && state.actions.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        OSProgressRing(progress = 0.5f, modifier = Modifier.size(32.dp))
                    }
                }
                state.tasks.isEmpty() && state.actions.isEmpty() -> {
                    OSEmptyState(
                        title = "No results found",
                        description = "No tasks or actions matched \"${state.query}\"",
                    )
                }
                else -> {
                    SearchResultsList(
                        actions = state.actions,
                        tasks = state.tasks,
                        onActionClick = { action ->
                            when (action.kind) {
                                SearchActionKind.CreateTask -> showQuickAddSheet = true
                                SearchActionKind.Navigate -> action.targetRoute?.let { onNavigate(it) }
                            }
                        },
                        onTaskToggle = { viewModel.toggleTaskComplete(it) },
                        onTaskClick = { selectedTaskForDetail = it },
                    )
                }
            }
        }
    }

    if (showQuickAddSheet) {
        QuickAddSheet(
            onDismissRequest = { showQuickAddSheet = false },
        )
    }

    selectedTaskForDetail?.let { task ->
        TaskDetailSheet(
            title = task.title,
            notes = task.notes,
            dueDate = task.dueDate,
            dueTime = task.dueTime,
            priority = task.priority,
            isCompleted = task.isCompleted,
            subtasks = emptyList(),
            onTitleChange = {},
            onNotesChange = {},
            onDueDateChange = {},
            onPriorityChange = {},
            onCompletedChange = { viewModel.toggleTaskComplete(task) },
            onAddSubtask = {},
            onSubtaskCompletedChange = { _, _ -> },
            onDismissRequest = { selectedTaskForDetail = null },
        )
    }
}

@Composable
private fun RecentSearchesView(
    recentSearches: List<String>,
    onSelectRecent: (String) -> Unit,
    onRemoveRecent: (String) -> Unit,
    onClearAll: () -> Unit,
    onNavigate: (OS11Route) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(spacing.s4)) {
        if (recentSearches.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Recent Searches",
                    style = type.sectionHeader.copy(color = colors.textSecondary),
                )
                Text(
                    text = "Clear all",
                    style = type.meta.copy(color = colors.accent),
                    modifier = Modifier.clickable { onClearAll() },
                )
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                contentPadding = PaddingValues(vertical = spacing.s1),
            ) {
                items(recentSearches, key = { it }) { query ->
                    OSChip(
                        label = query,
                        onClick = { onSelectRecent(query) },
                        trailingContent = {
                            OSIconButton(
                                onClick = { onRemoveRecent(query) },
                                contentDescription = "Remove $query",
                                modifier = Modifier.size(14.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(12.dp),
                                )
                            }
                        },
                    )
                }
            }
        }

        Text(
            text = "Quick Navigation",
            style = type.sectionHeader.copy(color = colors.textSecondary),
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.s2)) {
            item {
                OSChip(label = "My Day", onClick = { onNavigate(OS11Route.MyDay) })
            }
            item {
                OSChip(label = "Inbox", onClick = { onNavigate(OS11Route.Inbox) })
            }
            item {
                OSChip(label = "Planned", onClick = { onNavigate(OS11Route.Planned) })
            }
            item {
                OSChip(label = "Important", onClick = { onNavigate(OS11Route.Important) })
            }
            item {
                OSChip(label = "Browse", onClick = { onNavigate(OS11Route.Browse) })
            }
        }
    }
}

@Composable
private fun SearchResultsList(
    actions: List<SearchActionItem>,
    tasks: List<SearchTaskSnippet>,
    onActionClick: (SearchActionItem) -> Unit,
    onTaskToggle: (TaskItem) -> Unit,
    onTaskClick: (TaskItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(spacing.s3),
        contentPadding = PaddingValues(bottom = spacing.s6),
    ) {
        if (actions.isNotEmpty()) {
            item {
                Text(
                    text = "Actions",
                    style = type.sectionHeader.copy(color = colors.textSecondary),
                    modifier = Modifier.padding(top = spacing.s2),
                )
            }
            items(actions, key = { it.id }) { action ->
                SearchActionRow(action = action, onClick = { onActionClick(action) })
            }
        }

        if (tasks.isNotEmpty()) {
            item {
                Text(
                    text = "Tasks (${tasks.size})",
                    style = type.sectionHeader.copy(color = colors.textSecondary),
                    modifier = Modifier.padding(top = spacing.s3),
                )
            }
            items(tasks, key = { it.task.id }) { item ->
                SearchTaskRow(
                    taskSnippet = item,
                    onToggle = { onTaskToggle(item.task) },
                    onClick = { onTaskClick(item.task) },
                )
            }
        }
    }
}

@Composable
private fun SearchActionRow(action: SearchActionItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val radius = OS11Theme.radius
    val type = OS11Theme.type

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radius.sm))
            .background(colors.surfaceRaised)
            .clickable(
                role = Role.Button,
                indication = ripple(color = colors.surfaceHover),
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            )
            .padding(horizontal = spacing.s3, vertical = spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.s3),
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                imageVector = when (action.kind) {
                    SearchActionKind.CreateTask -> Icons.Default.Add
                    SearchActionKind.Navigate -> Icons.AutoMirrored.Filled.ArrowForward
                },
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(20.dp),
            )
            Column {
                Text(
                    text = action.title,
                    style = type.body.copy(color = colors.textPrimary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                action.subtitle?.let {
                    Text(
                        text = it,
                        style = type.meta.copy(color = colors.textSecondary),
                    )
                }
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun SearchTaskRow(
    taskSnippet: SearchTaskSnippet,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val radius = OS11Theme.radius
    val type = OS11Theme.type
    val task = taskSnippet.task

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radius.sm))
            .background(colors.surfaceRaised)
            .clickable(
                role = Role.Button,
                indication = ripple(color = colors.surfaceHover),
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            )
            .padding(horizontal = spacing.s3, vertical = spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OSCheckbox(
            checked = task.isCompleted,
            onCheckedChange = { onToggle() },
        )

        Spacer(modifier = Modifier.width(spacing.s2))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = type.body.copy(
                    color = if (task.isCompleted) colors.textTertiary else colors.textPrimary,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            // Render matching snippet with contextual excerpt
            taskSnippet.snippet?.let { snippet ->
                Text(
                    text = snippet,
                    style = type.meta.copy(
                        color = colors.textSecondary,
                        fontStyle = FontStyle.Italic,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            task.dueDate?.let { date ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = spacing.s1),
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(12.dp),
                    )
                    Text(
                        text = date,
                        style = type.meta.copy(color = colors.accent),
                    )
                }
            }
        }
    }
}
