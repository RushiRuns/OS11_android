package com.os11.app.ui.features.dayplan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.os11.app.service.task.TaskItem
import com.os11.app.ui.components.OSButton
import com.os11.app.ui.components.OSButtonVariant
import com.os11.app.ui.components.OSCheckbox
import com.os11.app.ui.components.OSChip
import com.os11.app.ui.components.OSIconButton
import com.os11.app.ui.components.OSPriority
import com.os11.app.ui.components.OSPriorityBar
import com.os11.app.ui.components.OSSheet
import com.os11.app.ui.theme.OS11Theme
import java.time.LocalTime

private const val MINUTES_PER_HOUR = 60
private const val HOURS_IN_DAY = 24
private const val HOUR_HEIGHT_NUM = 80
private const val TIME_COLUMN_WIDTH_NUM = 52
private const val TIMELINE_GAP_NUM = 4
private const val QUICK_SNAP_STEP = 15

@Composable
fun DayPlanScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DayPlanViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surfaceBase),
    ) {
        // 1. Top bar
        DayPlanTopBar(
            onNavigateBack = onNavigateBack,
            date = state.date,
        )

        // 2. Module Disabled Warning Banner
        if (!state.isAgendaEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.s4)
                    .background(colors.surfaceRaised, RoundedCornerShape(OS11Theme.radius.md))
                    .border(1.dp, colors.borderSubtle, RoundedCornerShape(OS11Theme.radius.md))
                    .padding(spacing.s4),
            ) {
                Text(
                    text = "The Agenda module is currently disabled. Enable it in Settings to plan your day.",
                    style = type.body.copy(color = colors.textSecondary),
                )
            }
        }

        // 3. Main Timeline Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            TimelineContent(
                blocks = state.scheduledBlocks,
                onMoveBlock = viewModel::moveBlock,
                onSelectBlock = viewModel::selectBlockForAction,
            )

            // 4. Docked Bottom Sheet for Unscheduled Tasks
            UnscheduledDockedSheet(
                unscheduledTasks = state.unscheduledTasks,
                selectedIds = state.selectedUnscheduledIds,
                isExpanded = state.isSheetExpanded,
                onToggleExpand = { viewModel.setSheetExpanded(!state.isSheetExpanded) },
                onToggleSelect = viewModel::toggleSelectUnscheduled,
                onSelectAll = viewModel::selectAllUnscheduled,
                onClearAll = viewModel::clearSelectedUnscheduled,
                onAddToPlan = viewModel::addSelectedToPlan,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    // Block Action Sheet (Thumb Friendly)
    state.selectedBlockForAction?.let { block ->
        DayPlanBlockActionSheet(
            block = block,
            onDismiss = { viewModel.selectBlockForAction(null) },
            onQuickAdjustTime = { delta -> viewModel.quickAdjustTime(block.task.id, delta) },
            onQuickAdjustDuration = { delta -> viewModel.quickAdjustDuration(block.task.id, delta) },
            onRemoveFromPlan = {
                viewModel.removeFromPlan(block.task.id)
                viewModel.selectBlockForAction(null)
            },
        )
    }
}

@Composable
private fun DayPlanTopBar(onNavigateBack: () -> Unit, date: String) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.s3, vertical = spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OSIconButton(
                onClick = onNavigateBack,
                contentDescription = "Back",
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = colors.textPrimary,
                )
            }
            Spacer(modifier = Modifier.width(spacing.s2))
            Column {
                Text(
                    text = "Day Plan",
                    style = type.subViewTitle.copy(color = colors.textPrimary),
                )
                Text(
                    text = if (date.isNotBlank()) date else "Today",
                    style = type.meta.copy(color = colors.textSecondary),
                )
            }
        }
    }
}

@Composable
private fun TimelineContent(
    blocks: List<DayPlanBlock>,
    onMoveBlock: (String, Int) -> Unit,
    onSelectBlock: (DayPlanBlock) -> Unit,
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val hourHeightDp = HOUR_HEIGHT_NUM.dp
    val hourHeightPx = with(density) { hourHeightDp.toPx() }
    val pxPerMinute = hourHeightPx / MINUTES_PER_HOUR.toFloat()

    // Auto-scroll to current time on open
    LaunchedEffect(Unit) {
        val now = LocalTime.now()
        val currentMinutes = now.hour * MINUTES_PER_HOUR + now.minute
        val targetScrollPx = ((currentMinutes - 60).coerceAtLeast(0) * pxPerMinute).toInt()
        scrollState.scrollTo(targetScrollPx)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
    ) {
        val totalHeight = hourHeightDp * HOURS_IN_DAY
        val availableWidth = maxWidth - TIME_COLUMN_WIDTH_NUM.dp

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeight),
        ) {
            // Background Hour Grid Lines
            HourGridBackground(hourHeight = hourHeightDp)

            // Current Time Indicator Line
            CurrentTimeLine(
                hourHeightPx = hourHeightPx,
                timeColumnWidth = TIME_COLUMN_WIDTH_NUM.dp,
            )

            // Scheduled Task Blocks
            for (block in blocks) {
                TimelineBlockItem(
                    block = block,
                    availableWidth = availableWidth,
                    pxPerMinute = pxPerMinute,
                    onMoveBlock = onMoveBlock,
                    onSelectBlock = onSelectBlock,
                )
            }
        }
    }
}

@Composable
private fun HourGridBackground(hourHeight: Dp) {
    val colors = OS11Theme.colors
    val type = OS11Theme.type

    Column(modifier = Modifier.fillMaxSize()) {
        for (hour in 0 until HOURS_IN_DAY) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(hourHeight)
                    .border(0.5.dp, colors.borderSubtle),
            ) {
                val label = formatHourLabel(hour)
                Text(
                    text = label,
                    style = type.meta.copy(color = colors.textSecondary, fontSize = 11.sp),
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun CurrentTimeLine(hourHeightPx: Float, timeColumnWidth: Dp) {
    val now = LocalTime.now()
    val currentMinutes = now.hour * MINUTES_PER_HOUR + now.minute
    val density = LocalDensity.current
    val currentY = with(density) { (currentMinutes * (hourHeightPx / MINUTES_PER_HOUR)).toDp() }
    val colors = OS11Theme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = currentY),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(timeColumnWidth)
                .padding(end = 4.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(colors.accent, CircleShape),
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(2.dp)
                .background(colors.accent),
        )
    }
}

@Composable
private fun TimelineBlockItem(
    block: DayPlanBlock,
    availableWidth: Dp,
    pxPerMinute: Float,
    onMoveBlock: (String, Int) -> Unit,
    onSelectBlock: (DayPlanBlock) -> Unit,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    val topOffset = (block.startMin * HOUR_HEIGHT_NUM / MINUTES_PER_HOUR).dp
    val blockHeight = (block.durationMin * HOUR_HEIGHT_NUM / MINUTES_PER_HOUR).dp.coerceAtLeast(24.dp)

    val laneWidth = (availableWidth - (TIMELINE_GAP_NUM * 2).dp) / block.totalLanes
    val leftOffset = TIME_COLUMN_WIDTH_NUM.dp + (laneWidth * block.laneIndex) + TIMELINE_GAP_NUM.dp

    var dragAccumulatorY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .offset(x = leftOffset, y = topOffset)
            .width(laneWidth - TIMELINE_GAP_NUM.dp)
            .height(blockHeight)
            .clip(RoundedCornerShape(OS11Theme.radius.md))
            .background(colors.surfaceRaised)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(OS11Theme.radius.md))
            .clickable(
                role = Role.Button,
                onClick = { onSelectBlock(block) },
            )
            .pointerInput(block.task.id) {
                detectDragGestures(
                    onDragStart = { dragAccumulatorY = 0f },
                    onDragEnd = {
                        val minutesDelta = (dragAccumulatorY / pxPerMinute).toInt()
                        if (minutesDelta != 0) {
                            onMoveBlock(block.task.id, block.startMin + minutesDelta)
                        }
                        dragAccumulatorY = 0f
                    },
                    onDrag = { _, dragAmount ->
                        dragAccumulatorY += dragAmount.y
                    },
                )
            },
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            OSPriorityBar(priority = OSPriority.fromInt(block.task.priority))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = spacing.s2, vertical = spacing.s1),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = block.task.title,
                    style = type.body.copy(color = colors.textPrimary, fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                )
                Text(
                    text = block.formattedTimeRange,
                    style = type.meta.copy(color = colors.textSecondary, fontSize = 11.sp),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun UnscheduledDockedSheet(
    unscheduledTasks: List<TaskItem>,
    selectedIds: Set<String>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onToggleSelect: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit,
    onAddToPlan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    val sheetHeight = if (isExpanded) 360.dp else 120.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(sheetHeight)
            .clip(RoundedCornerShape(topStart = OS11Theme.radius.lg, topEnd = OS11Theme.radius.lg))
            .background(colors.surfaceRaised)
            .border(
                width = 1.dp,
                color = colors.borderSubtle,
                shape = RoundedCornerShape(topStart = OS11Theme.radius.lg, topEnd = OS11Theme.radius.lg),
            )
            .padding(horizontal = spacing.s4, vertical = spacing.s2),
    ) {
        // Drag handle / Title Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onToggleExpand),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    tint = colors.textSecondary,
                )
                Spacer(modifier = Modifier.width(spacing.s2))
                Text(
                    text = "Unscheduled Tasks (${unscheduledTasks.size})",
                    style = type.sectionHeader.copy(color = colors.textPrimary),
                )
            }

            if (unscheduledTasks.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.s2)) {
                    Text(
                        text = if (selectedIds.size == unscheduledTasks.size) "Clear" else "Select All",
                        style = type.meta.copy(color = colors.accent),
                        modifier = Modifier.clickable {
                            if (selectedIds.size == unscheduledTasks.size) onClearAll() else onSelectAll()
                        },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.s2))

        // Unscheduled Task Pool List
        if (unscheduledTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "All My Day tasks are scheduled!",
                    style = type.meta.copy(color = colors.textSecondary),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.s1),
            ) {
                items(unscheduledTasks, key = { it.id }) { task ->
                    val isSelected = task.id in selectedIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(OS11Theme.radius.sm))
                            .clickable(role = Role.Checkbox) { onToggleSelect(task.id) }
                            .padding(vertical = spacing.s1),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OSCheckbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelect(task.id) },
                        )
                        Spacer(modifier = Modifier.width(spacing.s2))
                        Text(
                            text = task.title,
                            style = type.body.copy(color = colors.textPrimary),
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                        )
                        val estMin = task.estimatedMinutes ?: 30
                        OSChip(
                            label = "${estMin}m",
                            onClick = { onToggleSelect(task.id) },
                        )
                    }
                }
            }
        }

        // Thumb-Friendly "Add to plan" Button
        if (selectedIds.isNotEmpty()) {
            Spacer(modifier = Modifier.height(spacing.s2))
            OSButton(
                onClick = onAddToPlan,
                modifier = Modifier.fillMaxWidth(),
                variant = OSButtonVariant.Primary,
            ) {
                Text("Add to Plan (${selectedIds.size})")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayPlanBlockActionSheet(
    block: DayPlanBlock,
    onDismiss: () -> Unit,
    onQuickAdjustTime: (Int) -> Unit,
    onQuickAdjustDuration: (Int) -> Unit,
    onRemoveFromPlan: () -> Unit,
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
                text = block.task.title,
                style = type.subViewTitle.copy(color = colors.textPrimary),
            )
            Text(
                text = block.formattedTimeRange,
                style = type.meta.copy(color = colors.textSecondary),
            )

            Spacer(modifier = Modifier.height(spacing.s4))

            // Start Time Quick Adjust
            Text(
                text = "Start Time",
                style = type.sectionHeader.copy(color = colors.textPrimary),
            )
            Spacer(modifier = Modifier.height(spacing.s2))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                OSButton(
                    onClick = { onQuickAdjustTime(-QUICK_SNAP_STEP) },
                    variant = OSButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("-15 min")
                }
                OSButton(
                    onClick = { onQuickAdjustTime(QUICK_SNAP_STEP) },
                    variant = OSButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("+15 min")
                }
            }

            Spacer(modifier = Modifier.height(spacing.s3))

            // Duration Quick Adjust
            Text(
                text = "Duration",
                style = type.sectionHeader.copy(color = colors.textPrimary),
            )
            Spacer(modifier = Modifier.height(spacing.s2))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                OSButton(
                    onClick = { onQuickAdjustDuration(-QUICK_SNAP_STEP) },
                    variant = OSButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("-15 min")
                }
                OSButton(
                    onClick = { onQuickAdjustDuration(QUICK_SNAP_STEP) },
                    variant = OSButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("+15 min")
                }
            }

            Spacer(modifier = Modifier.height(spacing.s4))

            // Remove from plan action
            OSButton(
                onClick = onRemoveFromPlan,
                variant = OSButtonVariant.Danger,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(imageVector = Icons.Outlined.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(spacing.s2))
                Text("Remove from Plan")
            }
        }
    }
}

private fun formatHourLabel(hour: Int): String = when {
    hour == 0 -> "12 AM"
    hour < 12 -> "$hour AM"
    hour == 12 -> "12 PM"
    else -> "${hour - 12} PM"
}
