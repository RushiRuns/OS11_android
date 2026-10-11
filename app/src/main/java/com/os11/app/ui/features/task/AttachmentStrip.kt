package com.os11.app.ui.features.task

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.os11.app.service.attachment.AttachmentItem
import com.os11.app.ui.components.OSButton
import com.os11.app.ui.components.OSButtonVariant
import com.os11.app.ui.components.OSChip
import com.os11.app.ui.components.OSSheet
import com.os11.app.ui.components.OSTextField
import com.os11.app.ui.theme.OS11Theme
import java.io.File

/**
 * Attachment strip composable rendering task attachments per Phase 15.
 * Features:
 * - Horizontal strip for attached files and links
 * - Thumbnail preview for images loaded via Coil
 * - Distinct icons for links, PDFs, media, and documents
 * - Status indicator for blob_state (present, missing, pending)
 * - Click to view via intent chooser
 * - Remove button
 * - Add File (SAF) and Add Link triggers
 */
@Composable
fun AttachmentStrip(
    attachments: List<AttachmentItem>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onAddFileClick: () -> Unit,
    onAddLinkClick: () -> Unit,
    onOpenAttachment: (AttachmentItem) -> Unit,
    onDeleteAttachment: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    Column(modifier = modifier.fillMaxWidth()) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onToggleExpand)
                .padding(vertical = spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(spacing.s2))
            Text(
                text = "Attachments",
                style = type.body.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary,
            )
            if (attachments.isNotEmpty()) {
                Spacer(modifier = Modifier.width(spacing.s2))
                Box(
                    modifier = Modifier
                        .background(colors.surfaceRaised, RoundedCornerShape(spacing.s1))
                        .padding(horizontal = spacing.s2, vertical = spacing.px),
                ) {
                    Text(
                        text = attachments.size.toString(),
                        style = type.meta.copy(fontWeight = FontWeight.Bold),
                        color = colors.accent,
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))

            // Quick add buttons on header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.s1),
            ) {
                OSChip(
                    label = "+ File",
                    selected = false,
                    onClick = onAddFileClick,
                )
                OSChip(
                    label = "+ Link",
                    selected = false,
                    onClick = onAddLinkClick,
                )
            }

            Spacer(modifier = Modifier.width(spacing.s2))
            Icon(
                imageVector = if (isExpanded) {
                    Icons.Outlined.KeyboardArrowDown
                } else {
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight
                },
                contentDescription = if (isExpanded) "Collapse attachments" else "Expand attachments",
                tint = colors.textSecondary,
                modifier = Modifier.size(20.dp),
            )
        }

        // Expanded Content
        if (isExpanded) {
            Spacer(modifier = Modifier.height(spacing.s2))
            if (attachments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surfaceBase, RoundedCornerShape(OS11Theme.radius.lg))
                        .border(spacing.px, colors.borderSubtle, RoundedCornerShape(OS11Theme.radius.lg))
                        .padding(spacing.s4),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No attachments yet. Add a file or web link.",
                        style = type.meta,
                        color = colors.textTertiary,
                    )
                }
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.s3),
                ) {
                    items(attachments, key = { it.id }) { item ->
                        AttachmentCard(
                            item = item,
                            onClick = { onOpenAttachment(item) },
                            onDelete = { onDeleteAttachment(item.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
@Suppress("CyclomaticComplexMethod")
private fun AttachmentCard(item: AttachmentItem, onClick: () -> Unit, onDelete: () -> Unit) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val radius = OS11Theme.radius
    val type = OS11Theme.type

    val cardShape = RoundedCornerShape(radius.lg)

    Box(
        modifier = Modifier
            .width(136.dp)
            .height(132.dp)
            .clip(cardShape)
            .background(colors.surfaceRaised)
            .border(spacing.px, colors.borderGlass, cardShape)
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Preview / Thumbnail area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .background(colors.surfaceBase),
                contentAlignment = Alignment.Center,
            ) {
                AttachmentThumbnailPreview(item)

                // Delete button in top-right
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(spacing.s1)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceRaised)
                        .clickable(role = Role.Button, onClick = onDelete),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove attachment",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }

            // Info area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.s2, vertical = spacing.s1),
            ) {
                Text(
                    text = item.originalName,
                    style = type.meta.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = item.formattedSize,
                        style = type.meta,
                        color = colors.textSecondary,
                    )
                    if (item.blobState != "present") {
                        Text(
                            text = if (item.blobState == "pending") "Pending" else "Missing",
                            style = type.meta.copy(fontWeight = FontWeight.Bold),
                            color = colors.warning,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Bottom sheet modal for adding a link attachment.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLinkSheet(onDismissRequest: () -> Unit, onAddLink: (url: String, title: String?) -> Unit) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    var urlInput by remember { mutableStateOf("") }
    var titleInput by remember { mutableStateOf("") }

    OSSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.s4),
        ) {
            Text(
                text = "Add Link Attachment",
                style = type.subViewTitle,
                color = colors.textPrimary,
            )
            Spacer(modifier = Modifier.height(spacing.s3))

            OSTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                placeholder = "https://example.com/docs",
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(spacing.s2))

            OSTextField(
                value = titleInput,
                onValueChange = { titleInput = it },
                placeholder = "Title or description (optional)",
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(spacing.s4))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OSButton(
                    onClick = onDismissRequest,
                    variant = OSButtonVariant.Ghost,
                ) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(spacing.s2))
                OSButton(
                    onClick = {
                        if (urlInput.isNotBlank()) {
                            onAddLink(urlInput.trim(), titleInput.trim().ifBlank { null })
                            onDismissRequest()
                        }
                    },
                    enabled = urlInput.isNotBlank(),
                    variant = OSButtonVariant.Primary,
                ) {
                    Text("Add Link")
                }
            }
            Spacer(modifier = Modifier.height(spacing.s2))
        }
    }
}

@Composable
private fun AttachmentThumbnailPreview(item: AttachmentItem) {
    val colors = OS11Theme.colors
    val loadPath = item.thumbnailPath ?: item.filePath
    if (item.isImage && !loadPath.isNullOrBlank()) {
        AsyncImage(
            model = File(loadPath),
            contentDescription = item.originalName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
        )
    } else {
        val icon = when {
            item.isLink -> Icons.Outlined.Edit
            item.isAudio || item.isVideo -> Icons.Outlined.Refresh
            else -> Icons.Outlined.DateRange
        }
        val tint = when {
            item.isLink -> colors.accent
            item.isPdf -> colors.danger
            else -> colors.textSecondary
        }
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(28.dp),
        )
    }
}
