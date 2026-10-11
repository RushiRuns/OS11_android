package com.os11.app.ui.features.task

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.os11.app.service.attachment.AttachmentItem
import com.os11.app.service.tag.TagItem
import com.os11.app.ui.components.OSCheckbox
import com.os11.app.ui.components.OSChip
import com.os11.app.ui.components.OSDatePickerSheet
import com.os11.app.ui.components.OSIconButton
import com.os11.app.ui.components.OSPriority
import com.os11.app.ui.components.OSPriorityPicker
import com.os11.app.ui.components.OSSheet
import com.os11.app.ui.components.OSTagDot
import com.os11.app.ui.components.OSTagDotVariant
import com.os11.app.ui.components.OSTagShape
import com.os11.app.ui.components.OSTextField
import com.os11.app.ui.components.TagPickerSheet
import com.os11.app.ui.components.note.NoteEditor
import com.os11.app.ui.theme.OS11Theme

/**
 * Task detail bottom sheet implementing progressive disclosure:
 * Sections collapsed by default; empty sections render nothing until expanded.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskDetailSheet(
    title: String,
    notes: String?,
    dueDate: String?,
    dueTime: String?,
    priority: Int,
    isCompleted: Boolean,
    subtasks: List<TaskUiModel>,
    tags: List<TagItem> = emptyList(),
    allTags: List<TagItem> = emptyList(),
    onTitleChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onDueDateChange: (String?) -> Unit,
    onPriorityChange: (Int) -> Unit,
    onCompletedChange: (Boolean) -> Unit,
    onAddSubtask: (String) -> Unit,
    onSubtaskCompletedChange: (String, Boolean) -> Unit,
    onAddTag: ((String) -> Unit)? = null,
    onRemoveTag: ((String) -> Unit)? = null,
    onCreateTag: ((String, String?) -> Unit)? = null,
    recurrenceRule: String? = null,
    onRecurrenceRuleChange: ((String?) -> Unit)? = null,
    reminders: List<ReminderUiModel> = emptyList(),
    onAddReminder: ((String) -> Unit)? = null,
    onDeleteReminder: ((String) -> Unit)? = null,
    attachments: List<AttachmentItem> = emptyList(),
    onAddFileAttachment: ((android.net.Uri) -> Unit)? = null,
    onAddLinkAttachment: ((url: String, title: String?) -> Unit)? = null,
    onOpenAttachment: ((AttachmentItem) -> Unit)? = null,
    onDeleteAttachment: ((String) -> Unit)? = null,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    var currentTitle by remember(title) { mutableStateOf(title) }
    var currentNotes by remember(notes) { mutableStateOf(notes ?: "") }
    var currentPriority by remember(priority) { mutableIntStateOf(priority) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTagPicker by remember { mutableStateOf(false) }
    var showAddLinkDialog by remember { mutableStateOf(false) }

    var isNotesExpanded by remember { mutableStateOf(!notes.isNullOrBlank()) }
    var isSubtasksExpanded by remember { mutableStateOf(subtasks.isNotEmpty()) }
    var isRemindersExpanded by remember(reminders.isNotEmpty()) { mutableStateOf(reminders.isNotEmpty()) }
    var isAttachmentsExpanded by remember(attachments.isNotEmpty()) { mutableStateOf(attachments.isNotEmpty()) }
    var newSubtaskTitle by remember { mutableStateOf("") }

    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let { onAddFileAttachment?.invoke(it) }
    }

    OSSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(spacing.s4),
        ) {
            // Header: Complete checkbox + Title + Close button
            TaskDetailHeader(
                isCompleted = isCompleted,
                title = currentTitle,
                onCompletedChange = onCompletedChange,
                onTitleChange = {
                    currentTitle = it
                    onTitleChange(it)
                },
                onClose = onDismissRequest,
            )

            Spacer(modifier = Modifier.height(spacing.s3))

            // When / Due date row
            TaskDetailWhenRow(
                dueDate = dueDate,
                dueTime = dueTime,
                onSetDateClick = { showDatePicker = true },
                onClearDueDate = { onDueDateChange(null) },
            )

            Spacer(modifier = Modifier.height(spacing.s3))

            // Repeat / Recurrence row
            TaskDetailRecurrenceRow(
                recurrenceRule = recurrenceRule,
                onRecurrenceRuleChange = onRecurrenceRuleChange,
            )

            Spacer(modifier = Modifier.height(spacing.s3))

            // Priority Selection Row
            TaskDetailPriorityRow(
                currentPriority = currentPriority,
                onPriorityChange = { pLevel ->
                    currentPriority = pLevel
                    onPriorityChange(pLevel)
                },
            )

            Spacer(modifier = Modifier.height(spacing.s3))

            // Tags Row
            TaskDetailTagsRow(
                tags = tags,
                onRemoveTag = onRemoveTag,
                onAddTagClick = { showTagPicker = true },
            )

            Spacer(modifier = Modifier.height(spacing.s4))

            // Subtasks Section (Progressive disclosure: collapsed if empty)
            SectionHeader(
                title = "Subtasks (${subtasks.size})",
                icon = Icons.AutoMirrored.Outlined.List,
                isExpanded = isSubtasksExpanded,
                onClick = { isSubtasksExpanded = !isSubtasksExpanded },
            )

            if (isSubtasksExpanded) {
                TaskDetailSubtasksList(
                    subtasks = subtasks,
                    newSubtaskTitle = newSubtaskTitle,
                    onNewSubtaskTitleChange = { newSubtaskTitle = it },
                    onAddSubtask = {
                        if (newSubtaskTitle.isNotBlank()) {
                            onAddSubtask(newSubtaskTitle.trim())
                            newSubtaskTitle = ""
                        }
                    },
                    onSubtaskCompletedChange = onSubtaskCompletedChange,
                )
            }

            Spacer(modifier = Modifier.height(spacing.s4))

            // Notes Section (Progressive disclosure: collapsed if empty)
            SectionHeader(
                title = "Notes",
                icon = Icons.Outlined.Edit,
                isExpanded = isNotesExpanded,
                onClick = { isNotesExpanded = !isNotesExpanded },
            )

            if (isNotesExpanded) {
                NoteEditor(
                    initialText = currentNotes,
                    onNotesChanged = {
                        currentNotes = it
                        onNotesChange(it)
                    },
                    modifier = Modifier.padding(top = spacing.s2),
                )
            }

            Spacer(modifier = Modifier.height(spacing.s4))

            // Reminders Section (Progressive disclosure: collapsed if empty)
            TaskDetailRemindersSection(
                reminders = reminders,
                isExpanded = isRemindersExpanded,
                onToggleExpand = { isRemindersExpanded = !isRemindersExpanded },
                onAddReminder = onAddReminder,
                onDeleteReminder = onDeleteReminder,
            )

            Spacer(modifier = Modifier.height(spacing.s4))

            // Attachments Section (Progressive disclosure: collapsed if empty)
            AttachmentStrip(
                attachments = attachments,
                isExpanded = isAttachmentsExpanded,
                onToggleExpand = { isAttachmentsExpanded = !isAttachmentsExpanded },
                onAddFileClick = {
                    filePickerLauncher.launch(arrayOf("*/*"))
                },
                onAddLinkClick = {
                    showAddLinkDialog = true
                },
                onOpenAttachment = { item ->
                    onOpenAttachment?.invoke(item)
                },
                onDeleteAttachment = { id ->
                    onDeleteAttachment?.invoke(id)
                },
            )

            Spacer(modifier = Modifier.height(spacing.s6))
        }
    }

    if (showDatePicker) {
        OSDatePickerSheet(
            onDismissRequest = { showDatePicker = false },
            onDateSelected = { selectedDate ->
                showDatePicker = false
                onDueDateChange(selectedDate)
            },
        )
    }

    if (showTagPicker) {
        TagPickerSheet(
            tags = allTags,
            selectedTagIds = tags.map { it.id }.toSet(),
            onTagToggled = { tagId ->
                if (tags.any { it.id == tagId }) {
                    onRemoveTag?.invoke(tagId)
                } else {
                    onAddTag?.invoke(tagId)
                }
            },
            onCreateTag = { name, parentId ->
                onCreateTag?.invoke(name, parentId)
            },
            onDismissRequest = { showTagPicker = false },
        )
    }

    if (showAddLinkDialog) {
        AddLinkSheet(
            onDismissRequest = { showAddLinkDialog = false },
            onAddLink = { url, title ->
                onAddLinkAttachment?.invoke(url, title)
            },
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isExpanded: Boolean,
    onClick: () -> Unit,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(spacing.s2))
        Text(
            text = title,
            style = type.subViewTitle.copy(color = colors.textPrimary),
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = if (isExpanded) {
                Icons.Outlined.KeyboardArrowDown
            } else {
                Icons.AutoMirrored.Outlined.KeyboardArrowRight
            },
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            tint = colors.textTertiary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun TaskDetailHeader(
    isCompleted: Boolean,
    title: String,
    onCompletedChange: (Boolean) -> Unit,
    onTitleChange: (String) -> Unit,
    onClose: () -> Unit,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OSCheckbox(
            checked = isCompleted,
            onCheckedChange = onCompletedChange,
        )

        Spacer(modifier = Modifier.width(spacing.s2))

        OSTextField(
            value = title,
            onValueChange = onTitleChange,
            modifier = Modifier.weight(1f),
            placeholder = "Task title",
        )

        OSIconButton(
            onClick = onClose,
            contentDescription = "Close detail sheet",
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = colors.textSecondary,
            )
        }
    }
}

@Composable
private fun TaskDetailWhenRow(
    dueDate: String?,
    dueTime: String?,
    onSetDateClick: () -> Unit,
    onClearDueDate: () -> Unit,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing

    val whenLabel = when {
        !dueDate.isNullOrBlank() && !dueTime.isNullOrBlank() -> "When: $dueDate $dueTime"
        !dueDate.isNullOrBlank() -> "When: $dueDate"
        else -> "Set date (When)"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.s2),
    ) {
        OSChip(
            label = whenLabel,
            selected = !dueDate.isNullOrBlank(),
            onClick = onSetDateClick,
            leadingContent = {
                Icon(
                    imageVector = Icons.Outlined.DateRange,
                    contentDescription = null,
                    tint = if (!dueDate.isNullOrBlank()) colors.accent else colors.textSecondary,
                    modifier = Modifier.size(16.dp),
                )
            },
        )

        if (!dueDate.isNullOrBlank()) {
            OSIconButton(
                onClick = onClearDueDate,
                contentDescription = "Clear due date",
                modifier = Modifier.size(24.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun TaskDetailPriorityRow(currentPriority: Int, onPriorityChange: (Int) -> Unit) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.s2),
    ) {
        Text(
            text = "Priority:",
            style = type.meta.copy(color = colors.textSecondary),
        )
        OSPriorityPicker(
            selectedPriority = OSPriority.fromInt(currentPriority),
            onPriorityChange = { priority ->
                onPriorityChange(priority.value)
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskDetailTagsRow(tags: List<TagItem>, onRemoveTag: ((String) -> Unit)?, onAddTagClick: () -> Unit) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val radius = OS11Theme.radius

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.s2),
    ) {
        Text(
            text = "Tags:",
            style = type.meta.copy(color = colors.textSecondary),
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            verticalArrangement = Arrangement.spacedBy(spacing.s2),
        ) {
            tags.forEach { tag ->
                val tagColor = colors.tags.fromNameOrHex(tag.color)
                val shape = OSTagShape.fromTag(tag.name)

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(radius.full))
                        .background(colors.surfaceRaised)
                        .border(spacing.px, colors.borderSubtle, RoundedCornerShape(radius.full))
                        .padding(
                            start = spacing.s3,
                            end = if (onRemoveTag != null) spacing.s1 else spacing.s3,
                            top = spacing.s1,
                            bottom = spacing.s1,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.s1),
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
                    if (onRemoveTag != null) {
                        OSIconButton(
                            onClick = { onRemoveTag(tag.id) },
                            contentDescription = "Remove tag ${tag.name}",
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = colors.textTertiary,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }

            // + Tag Chip
            OSChip(
                label = "+ Tag",
                selected = false,
                onClick = onAddTagClick,
            )
        }
    }
}

@Composable
private fun TaskDetailSubtasksList(
    subtasks: List<TaskUiModel>,
    newSubtaskTitle: String,
    onNewSubtaskTitleChange: (String) -> Unit,
    onAddSubtask: () -> Unit,
    onSubtaskCompletedChange: (String, Boolean) -> Unit,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = spacing.s4, top = spacing.s2),
    ) {
        subtasks.forEach { subtask ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.s1),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OSCheckbox(
                    checked = subtask.isCompleted,
                    onCheckedChange = { onSubtaskCompletedChange(subtask.id, it) },
                )
                Spacer(modifier = Modifier.width(spacing.s2))
                Text(
                    text = subtask.title,
                    style = type.body.copy(
                        color = if (subtask.isCompleted) colors.textTertiary else colors.textPrimary,
                    ),
                )
            }
        }

        // Inline add subtask input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OSTextField(
                value = newSubtaskTitle,
                onValueChange = onNewSubtaskTitleChange,
                modifier = Modifier.weight(1f),
                placeholder = "Add subtask...",
            )
            Spacer(modifier = Modifier.width(spacing.s2))
            OSIconButton(
                onClick = onAddSubtask,
                contentDescription = "Add subtask",
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = colors.accent,
                )
            }
        }
    }
}

@Suppress("CyclomaticComplexMethod")
@Composable
private fun TaskDetailRecurrenceRow(recurrenceRule: String?, onRecurrenceRuleChange: ((String?) -> Unit)?) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val radius = OS11Theme.radius

    val label = when (recurrenceRule) {
        null -> "Does not repeat"
        "FREQ=DAILY" -> "Every day"
        "FREQ=WEEKLY" -> "Every week"
        "FREQ=MONTHLY" -> "Every month"
        "FREQ=YEARLY" -> "Every year"
        else -> "Custom ($recurrenceRule)"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radius.sm))
            .background(colors.surfaceRaised)
            .border(spacing.px, colors.borderSubtle, RoundedCornerShape(radius.sm))
            .clickable(
                enabled = onRecurrenceRuleChange != null,
                role = Role.Button,
                onClick = {
                    val next = when (recurrenceRule) {
                        null -> "FREQ=DAILY"
                        "FREQ=DAILY" -> "FREQ=WEEKLY"
                        "FREQ=WEEKLY" -> "FREQ=MONTHLY"
                        "FREQ=MONTHLY" -> "FREQ=YEARLY"
                        else -> null
                    }
                    onRecurrenceRuleChange?.invoke(next)
                },
            )
            .padding(horizontal = spacing.s3, vertical = spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
        ) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = null,
                tint = if (recurrenceRule != null) colors.accent else colors.textTertiary,
                modifier = Modifier.size(spacing.s4),
            )
            Text(
                text = "Repeat: $label",
                style = type.meta.copy(
                    color = if (recurrenceRule != null) colors.textPrimary else colors.textSecondary,
                ),
            )
        }

        if (recurrenceRule != null && onRecurrenceRuleChange != null) {
            OSIconButton(
                onClick = { onRecurrenceRuleChange.invoke(null) },
                contentDescription = "Clear repeat rule",
                modifier = Modifier.size(24.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun TaskDetailRemindersSection(
    reminders: List<ReminderUiModel>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onAddReminder: ((String) -> Unit)?,
    onDeleteReminder: ((String) -> Unit)?,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val radius = OS11Theme.radius

    SectionHeader(
        title = "Reminders (${reminders.size})",
        icon = Icons.Outlined.Notifications,
        isExpanded = isExpanded,
        onClick = onToggleExpand,
    )

    if (isExpanded) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.s2),
            verticalArrangement = Arrangement.spacedBy(spacing.s2),
        ) {
            reminders.forEach { reminder ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radius.sm))
                        .background(colors.surfaceRaised)
                        .border(spacing.px, colors.borderSubtle, RoundedCornerShape(radius.sm))
                        .padding(horizontal = spacing.s3, vertical = spacing.s2),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(spacing.s4),
                        )
                        Text(
                            text = reminder.formattedRemindAt,
                            style = type.meta.copy(color = colors.textPrimary),
                        )
                    }

                    if (onDeleteReminder != null) {
                        OSIconButton(
                            onClick = { onDeleteReminder(reminder.id) },
                            contentDescription = "Delete reminder",
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = colors.textTertiary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }

            if (onAddReminder != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                ) {
                    OSChip(
                        label = "+ In 1 hour",
                        onClick = {
                            val trigger = java.time.Instant.now()
                                .plus(1, java.time.temporal.ChronoUnit.HOURS)
                                .toString()
                            onAddReminder(trigger)
                        },
                    )
                    OSChip(
                        label = "+ Tomorrow 9 AM",
                        onClick = {
                            val trigger = java.time.LocalDate.now().plusDays(1)
                                .atTime(9, 0)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toInstant()
                                .toString()
                            onAddReminder(trigger)
                        },
                    )
                }
            }
        }
    }
}
