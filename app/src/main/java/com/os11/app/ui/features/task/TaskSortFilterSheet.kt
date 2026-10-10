package com.os11.app.ui.features.task

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.os11.app.ui.components.OSChip
import com.os11.app.ui.components.OSSheet
import com.os11.app.ui.theme.OS11Theme

/**
 * Bottom sheet for configuring task list sorting and filtering options.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskSortFilterSheet(
    selectedSort: TaskSortOption,
    selectedFilter: TaskFilterOption,
    onSortSelected: (TaskSortOption) -> Unit,
    onFilterSelected: (TaskFilterOption) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    OSSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.s4),
        ) {
            Text(
                text = "Sort & Filter",
                style = type.subViewTitle.copy(color = colors.textPrimary),
            )

            Spacer(modifier = Modifier.height(spacing.s4))

            // Sort Section
            Text(
                text = "SORT BY",
                style = type.meta.copy(color = colors.textSecondary),
            )
            Spacer(modifier = Modifier.height(spacing.s2))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                verticalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                listOf(
                    TaskSortOption.Manual to "Manual",
                    TaskSortOption.DueDate to "Due Date",
                    TaskSortOption.Priority to "Priority",
                    TaskSortOption.Title to "Title",
                    TaskSortOption.CreatedAt to "Created",
                ).forEach { (sortOpt, label) ->
                    OSChip(
                        label = label,
                        selected = selectedSort == sortOpt,
                        onClick = { onSortSelected(sortOpt) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.s4))

            // Filter Section
            Text(
                text = "FILTER BY",
                style = type.meta.copy(color = colors.textSecondary),
            )
            Spacer(modifier = Modifier.height(spacing.s2))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                verticalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                listOf(
                    TaskFilterOption.All to "All",
                    TaskFilterOption.ActiveOnly to "Active",
                    TaskFilterOption.CompletedOnly to "Completed",
                    TaskFilterOption.HighPriorityOnly to "High Priority",
                    TaskFilterOption.HasDueDateOnly to "Has Due Date",
                ).forEach { (filterOpt, label) ->
                    OSChip(
                        label = label,
                        selected = selectedFilter == filterOpt,
                        onClick = { onFilterSelected(filterOpt) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.s6))
        }
    }
}
