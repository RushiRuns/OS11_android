package com.os11.app.ui.features.tags

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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.os11.app.service.tag.TagItem
import com.os11.app.ui.components.OSButton
import com.os11.app.ui.components.OSButtonVariant
import com.os11.app.ui.components.OSChip
import com.os11.app.ui.components.OSEmptyState
import com.os11.app.ui.components.OSIconButton
import com.os11.app.ui.components.OSSheet
import com.os11.app.ui.components.OSTagDot
import com.os11.app.ui.components.OSTagDotVariant
import com.os11.app.ui.components.OSTagShape
import com.os11.app.ui.components.OSTextField
import com.os11.app.ui.theme.OS11Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagManagerScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TagManagerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val radius = OS11Theme.radius

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surfaceBase),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s4, vertical = spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OSIconButton(
                    onClick = onNavigateBack,
                    contentDescription = "Back",
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = null,
                        tint = colors.textPrimary,
                    )
                }
                Spacer(modifier = Modifier.width(spacing.s2))
                Text(
                    text = "Tag Manager",
                    style = type.viewTitle.copy(color = colors.textPrimary),
                )
            }

            OSButton(
                onClick = viewModel::openCreateTagDialog,
                variant = OSButtonVariant.Primary,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(spacing.s1))
                Text("New Tag")
            }
        }

        Spacer(modifier = Modifier.height(spacing.s2))

        // Tag List or Empty State
        if (state.tags.isEmpty() && !state.isLoading) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                OSEmptyState(
                    title = "No tags yet",
                    description = "Create tags to categorize your tasks across areas and projects.",
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = spacing.s4),
                verticalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                items(
                    items = state.tags,
                    key = { it.id },
                ) { tag ->
                    TagManagerRow(
                        tag = tag,
                        onRename = { viewModel.openRenameDialog(tag) },
                        onRecolor = { viewModel.openRecolorDialog(tag) },
                        onMerge = { viewModel.openMergeDialog(tag) },
                        onDelete = { viewModel.deleteTag(tag.id) },
                    )
                }
            }
        }
    }

    // Dialogs / Sheets
    if (state.showCreateTagDialog) {
        CreateTagSheet(
            tags = state.tags,
            onDismissRequest = viewModel::closeCreateTagDialog,
            onCreate = { name, color, parentId ->
                viewModel.createTag(name, color, parentId)
            },
        )
    }

    state.selectedTagForRename?.let { tag ->
        RenameTagSheet(
            tag = tag,
            onDismissRequest = viewModel::closeRenameDialog,
            onRename = { newName -> viewModel.renameTag(tag.id, newName) },
        )
    }

    state.selectedTagForRecolor?.let { tag ->
        RecolorTagSheet(
            tag = tag,
            onDismissRequest = viewModel::closeRecolorDialog,
            onRecolor = { newColor -> viewModel.recolorTag(tag.id, newColor) },
        )
    }

    state.selectedTagForMerge?.let { tag ->
        MergeTagSheet(
            sourceTag = tag,
            availableTags = state.tags.filter { it.id != tag.id },
            onDismissRequest = viewModel::closeMergeDialog,
            onMerge = { targetTagId -> viewModel.mergeTags(tag.id, targetTagId) },
        )
    }
}

@Composable
private fun TagManagerRow(
    tag: TagItem,
    onRename: () -> Unit,
    onRecolor: () -> Unit,
    onMerge: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val radius = OS11Theme.radius

    val tagColor = colors.tags.fromNameOrHex(tag.color)
    val tagShape = OSTagShape.fromTag(tag.name, tag.color)
    val indent = (tag.depth * 16).dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(radius.md))
            .background(colors.surfaceRaised)
            .border(spacing.px, colors.borderDefault, RoundedCornerShape(radius.md))
            .padding(horizontal = spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (indent > 0.dp) {
            Spacer(modifier = Modifier.width(indent))
        }

        OSTagDot(
            color = tagColor,
            shape = tagShape,
            variant = OSTagDotVariant.TaskRow,
        )

        Spacer(modifier = Modifier.width(spacing.s3))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tag.displayName,
                style = type.body.copy(color = colors.textPrimary),
            )
            Text(
                text = "${tag.taskCount} tasks" + if (tag.parentPath != null) " • in ${tag.parentPath}" else "",
                style = type.meta.copy(color = colors.textSecondary),
            )
        }

        // Action icons
        OSIconButton(onClick = onRecolor, contentDescription = "Recolor tag") {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(18.dp),
            )
        }

        OSIconButton(onClick = onRename, contentDescription = "Rename tag") {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(18.dp),
            )
        }

        OSIconButton(onClick = onMerge, contentDescription = "Merge tag") {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(18.dp),
            )
        }

        OSIconButton(onClick = onDelete, contentDescription = "Delete tag") {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = null,
                tint = colors.danger,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RenameTagSheet(tag: TagItem, onDismissRequest: () -> Unit, onRename: (String) -> Unit) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    var name by remember(tag) { mutableStateOf(tag.displayName) }

    OSSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.s4),
        ) {
            Text(
                text = "Rename Tag",
                style = type.subViewTitle.copy(color = colors.textPrimary),
            )
            Spacer(modifier = Modifier.height(spacing.s3))

            OSTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = "Tag name",
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(spacing.s4))

            OSButton(
                onClick = {
                    if (name.isNotBlank()) {
                        val fullName = if (tag.parentPath != null) "${tag.parentPath}/$name" else name
                        onRename(fullName.trim())
                    }
                },
                variant = OSButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecolorTagSheet(tag: TagItem, onDismissRequest: () -> Unit, onRecolor: (String) -> Unit) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val radius = OS11Theme.radius

    val palette = colors.tags.all
    val paletteNames = colors.tags.allNames

    OSSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.s4),
        ) {
            Text(
                text = "Recolor #${tag.displayName}",
                style = type.subViewTitle.copy(color = colors.textPrimary),
            )
            Spacer(modifier = Modifier.height(spacing.s3))

            Column(verticalArrangement = Arrangement.spacedBy(spacing.s2)) {
                palette.chunked(4).forEachIndexed { rowIndex, rowColors ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                    ) {
                        rowColors.forEachIndexed { colIndex, color ->
                            val colorIndex = rowIndex * 4 + colIndex
                            val colorName = paletteNames.getOrElse(colorIndex) { "blue" }
                            val isSelected = tag.color?.lowercase() == colorName

                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        if (isSelected) 3.dp else spacing.px,
                                        if (isSelected) colors.textPrimary else colors.borderDefault,
                                        CircleShape,
                                    )
                                    .clickable(role = Role.RadioButton) {
                                        onRecolor(colorName)
                                    }
                                    .semantics {
                                        contentDescription = "$colorName color"
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(colors.surfaceBase),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MergeTagSheet(
    sourceTag: TagItem,
    availableTags: List<TagItem>,
    onDismissRequest: () -> Unit,
    onMerge: (String) -> Unit,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val radius = OS11Theme.radius

    var selectedTargetId by remember { mutableStateOf<String?>(null) }

    OSSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.s4),
        ) {
            Text(
                text = "Merge Tag #${sourceTag.displayName}",
                style = type.subViewTitle.copy(color = colors.textPrimary),
            )
            Spacer(modifier = Modifier.height(spacing.s2))

            Text(
                text = "All tasks tagged #${sourceTag.displayName} (${sourceTag.taskCount} tasks) " +
                    "will be re-assigned to the selected tag. #${sourceTag.displayName} will be " +
                    "deleted. You can undo this action immediately via the snackbar.",
                style = type.meta.copy(color = colors.textSecondary),
            )
            Spacer(modifier = Modifier.height(spacing.s3))

            Text(
                text = "Select destination tag:",
                style = type.body.copy(color = colors.textPrimary),
            )
            Spacer(modifier = Modifier.height(spacing.s2))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                verticalArrangement = Arrangement.spacedBy(spacing.s1),
            ) {
                items(availableTags, key = { it.id }) { target ->
                    val isSelected = target.id == selectedTargetId
                    val tagColor = colors.tags.fromNameOrHex(target.color)
                    val tagShape = OSTagShape.fromTag(target.name, target.color)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(radius.sm))
                            .background(if (isSelected) colors.surfaceSelected else colors.surfaceRaised)
                            .border(
                                spacing.px,
                                if (isSelected) colors.accent else colors.borderDefault,
                                RoundedCornerShape(radius.sm),
                            )
                            .clickable(role = Role.RadioButton) { selectedTargetId = target.id }
                            .padding(horizontal = spacing.s3),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OSTagDot(color = tagColor, shape = tagShape, variant = OSTagDotVariant.TaskRow)
                        Spacer(modifier = Modifier.width(spacing.s2))
                        Text(
                            text = "#${target.name}",
                            style = type.body.copy(
                                color = if (isSelected) colors.accent else colors.textPrimary,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "${target.taskCount} tasks",
                            style = type.meta.copy(color = colors.textSecondary),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.s4))

            OSButton(
                onClick = {
                    selectedTargetId?.let { targetId ->
                        onMerge(targetId)
                    }
                },
                enabled = selectedTargetId != null,
                variant = OSButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Merge Tags")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateTagSheet(
    tags: List<TagItem>,
    onDismissRequest: () -> Unit,
    onCreate: (name: String, color: String?, parentId: String?) -> Unit,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("blue") }
    var selectedParentId by remember { mutableStateOf<String?>(null) }

    OSSheet(onDismissRequest = onDismissRequest) {
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
                value = name,
                onValueChange = { name = it },
                placeholder = "Tag name...",
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(spacing.s3))

            // Nesting parent tag selection
            if (tags.isNotEmpty()) {
                Text(
                    text = "Nest under parent (optional):",
                    style = type.meta.copy(color = colors.textSecondary),
                )
                Spacer(modifier = Modifier.height(spacing.s1))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                ) {
                    OSChip(
                        label = "None",
                        selected = selectedParentId == null,
                        onClick = { selectedParentId = null },
                    )
                    tags.take(3).forEach { parentTag ->
                        OSChip(
                            label = parentTag.displayName,
                            selected = selectedParentId == parentTag.id,
                            onClick = { selectedParentId = parentTag.id },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(spacing.s3))
            }

            // Color picker row
            Text(
                text = "Color:",
                style = type.meta.copy(color = colors.textSecondary),
            )
            Spacer(modifier = Modifier.height(spacing.s1))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                colors.tags.allNames.take(6).forEach { colorName ->
                    val color = colors.tags.fromNameOrHex(colorName)
                    val isSelected = selectedColor == colorName
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                if (isSelected) 2.dp else spacing.px,
                                if (isSelected) colors.textPrimary else colors.borderDefault,
                                CircleShape,
                            )
                            .clickable(role = Role.RadioButton) { selectedColor = colorName },
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.s4))

            OSButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(name.trim(), selectedColor, selectedParentId)
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
