package com.os11.app.ui.features.task

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.os11.app.ui.components.OSIconButton
import com.os11.app.ui.theme.OS11Theme

/**
 * Bottom action bar for multi-select bulk operations per Phase 8 specifications:
 * - Shows selected count and Select/Deselect All toggle
 * - Bulk actions: Complete, Move, Set Date, Tag, Priority, Trash
 * - 48dp minimum hit targets and accessible TalkBack semantics
 */
@Composable
fun TaskBulkActionBar(
    selectedCount: Int,
    totalCount: Int,
    onSelectAllToggle: () -> Unit,
    onComplete: () -> Unit,
    onSetDate: () -> Unit,
    onMove: () -> Unit,
    onTag: () -> Unit,
    onPriority: () -> Unit,
    onTrash: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val radius = OS11Theme.radius

    val isAllSelected = selectedCount > 0 && selectedCount >= totalCount

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = radius.lg, topEnd = radius.lg))
            .background(colors.surfaceSidebar)
            .border(
                width = spacing.px,
                color = colors.borderGlass,
                shape = RoundedCornerShape(topStart = radius.lg, topEnd = radius.lg),
            )
            .padding(horizontal = spacing.s3, vertical = spacing.s2),
    ) {
        // Top summary row: Close + Selected count + Select all toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                OSIconButton(
                    onClick = onClose,
                    contentDescription = "Exit multi-select mode",
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Text(
                    text = "$selectedCount selected",
                    style = type.subViewTitle.copy(color = colors.textPrimary),
                )
            }

            BoxActionTextButton(
                text = if (isAllSelected) "Deselect All" else "Select All",
                onClick = onSelectAllToggle,
            )
        }

        Spacer(modifier = Modifier.height(spacing.s1))

        // Actions row: Complete, When (Date), Move, Tag, Priority, Trash
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BulkActionButton(
                icon = Icons.Outlined.CheckCircle,
                label = "Complete",
                onClick = onComplete,
                tint = colors.accent,
            )

            BulkActionButton(
                icon = Icons.Outlined.DateRange,
                label = "When",
                onClick = onSetDate,
            )

            BulkActionButton(
                icon = Icons.AutoMirrored.Outlined.ArrowForward,
                label = "Move",
                onClick = onMove,
            )

            BulkActionButton(
                icon = Icons.AutoMirrored.Outlined.List,
                label = "Tag",
                onClick = onTag,
            )

            BulkActionButton(
                icon = Icons.Filled.Star,
                label = "Priority",
                onClick = onPriority,
            )

            BulkActionButton(
                icon = Icons.Outlined.Delete,
                label = "Trash",
                onClick = onTrash,
                tint = colors.priorityCritical,
            )
        }
    }
}

@Composable
private fun BulkActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = OS11Theme.colors.textPrimary,
) {
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    Column(
        modifier = modifier
            .size(width = 54.dp, height = 48.dp)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = label,
            style = type.meta.copy(color = tint),
            maxLines = 1,
        )
    }
}

@Composable
private fun BoxActionTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val radius = OS11Theme.radius

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(radius.sm))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = spacing.s3, vertical = spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = type.meta.copy(color = colors.accent),
        )
    }
}
