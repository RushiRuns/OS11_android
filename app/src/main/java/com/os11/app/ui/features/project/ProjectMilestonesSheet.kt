package com.os11.app.ui.features.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import com.os11.app.service.project.MilestoneItem
import com.os11.app.ui.components.OSButton
import com.os11.app.ui.components.OSCheckbox
import com.os11.app.ui.components.OSIconButton
import com.os11.app.ui.components.OSSheet
import com.os11.app.ui.components.OSTextField
import com.os11.app.ui.theme.OS11Theme
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectMilestonesSheet(
    milestones: List<MilestoneItem>,
    onToggleCompleted: (milestoneId: String, isCompleted: Boolean) -> Unit,
    onDeleteMilestone: (milestoneId: String) -> Unit,
    onCreateMilestone: (title: String, dueDate: String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    var titleInput by remember { mutableStateOf("") }
    var dateInput by remember { mutableStateOf(LocalDate.now().plusWeeks(1).toString()) }

    OSSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.s4),
        ) {
            Text(
                text = "Project Milestones",
                style = type.panelTitle.copy(color = colors.textPrimary),
            )
            Spacer(modifier = Modifier.height(spacing.s3))

            // Inline Create Milestone
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OSTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    placeholder = "Milestone title...",
                    modifier = Modifier.weight(1f),
                )
                OSTextField(
                    value = dateInput,
                    onValueChange = { dateInput = it },
                    placeholder = "YYYY-MM-DD",
                    modifier = Modifier.weight(0.7f),
                )
                OSIconButton(
                    onClick = {
                        if (titleInput.isNotBlank() && dateInput.isNotBlank()) {
                            onCreateMilestone(titleInput.trim(), dateInput.trim())
                            titleInput = ""
                        }
                    },
                    contentDescription = "Add milestone",
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = colors.accent,
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.s4))

            // Milestones list
            if (milestones.isEmpty()) {
                Text(
                    text = "No milestones set for this project.",
                    style = type.body.copy(color = colors.textTertiary),
                    modifier = Modifier.padding(vertical = spacing.s3),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing.s2),
                ) {
                    items(
                        items = milestones,
                        key = { it.id },
                    ) { milestone ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(OS11Theme.radius.md))
                                .background(colors.surfaceRaised)
                                .padding(spacing.s2),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                                modifier = Modifier.weight(1f),
                            ) {
                                OSCheckbox(
                                    checked = milestone.isCompleted,
                                    onCheckedChange = { onToggleCompleted(milestone.id, it) },
                                )
                                Column {
                                    Text(
                                        text = milestone.title,
                                        style = type.taskTitle.copy(color = colors.textPrimary),
                                    )
                                    Text(
                                        text = "Target: ${milestone.dueDate}",
                                        style = type.meta.copy(color = colors.textSecondary),
                                    )
                                }
                            }
                            OSIconButton(
                                onClick = { onDeleteMilestone(milestone.id) },
                                contentDescription = "Delete milestone",
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
            OSButton(
                onClick = onDismissRequest,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Close")
            }
        }
    }
}
