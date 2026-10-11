package com.os11.app.ui.features.dashboard

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Share
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.os11.app.service.dashboard.DailyTrendPoint
import com.os11.app.service.dashboard.DashboardOverviewStats
import com.os11.app.service.dashboard.DateRangeOption
import com.os11.app.service.dashboard.HeatmapDay
import com.os11.app.service.dashboard.PriorityDistributionItem
import com.os11.app.ui.components.OSChip
import com.os11.app.ui.components.OSIconButton
import com.os11.app.ui.theme.OS11Theme
import java.util.Locale

private const val HEATMAP_WEEKS = 12
private const val DAYS_IN_WEEK = 7

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surfaceBase),
    ) {
        // 1. Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s3, vertical = spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OSIconButton(onClick = onNavigateBack, contentDescription = "Back") {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = colors.textPrimary,
                    )
                }
                Spacer(modifier = Modifier.width(spacing.s2))
                Text(
                    text = "Dashboard",
                    style = type.viewTitle.copy(color = colors.textPrimary),
                )
            }

            if (state.isDashboardEnabled) {
                OSIconButton(
                    onClick = {
                        viewModel.exportCsv { csv ->
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_TEXT, csv)
                                putExtra(Intent.EXTRA_SUBJECT, "OS11 Dashboard Export")
                                setType("text/csv")
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Dashboard CSV"))
                        }
                    },
                    contentDescription = "Export CSV",
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = null,
                        tint = colors.textSecondary,
                    )
                }
            }
        }

        if (!state.isDashboardEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(spacing.s4),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "The Dashboard module is currently disabled. " +
                        "Enable it in Settings to view statistics and trends.",
                    style = type.body.copy(color = colors.textSecondary),
                )
            }
            return
        }

        // 2. Scrollable Dashboard Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.s4, vertical = spacing.s2),
            verticalArrangement = Arrangement.spacedBy(spacing.s4),
        ) {
            // Date Range Filter Chips
            DateRangeFilterRow(
                selectedRange = state.selectedRange,
                onSelectRange = viewModel::selectRange,
            )

            // Four Stat Cards (2x2 Grid)
            StatCardsGrid(stats = state.stats)

            // Completion Progress Bar
            CompletionProgressBar(stats = state.stats)

            // Priority Distribution Donut / Stacked Bar
            PriorityDistributionCard(priorities = state.priorities, totalTasks = state.stats.totalTasks)

            // Daily Completion Trend
            CompletionTrendCard(dailyTrend = state.dailyTrend)

            // Activity Heatmap (12 weeks)
            ActivityHeatmapCard(heatmapDays = state.heatmapDays)

            Spacer(modifier = Modifier.height(spacing.s6))
        }
    }
}

@Composable
private fun DateRangeFilterRow(selectedRange: DateRangeOption, onSelectRange: (DateRangeOption) -> Unit) {
    val spacing = OS11Theme.spacing
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.s2),
    ) {
        DateRangeOption.entries.forEach { option ->
            val isSelected = option == selectedRange
            OSChip(
                label = option.label,
                selected = isSelected,
                onClick = { onSelectRange(option) },
            )
        }
    }
}

@Composable
private fun StatCardsGrid(stats: DashboardOverviewStats) {
    val spacing = OS11Theme.spacing
    val colors = OS11Theme.colors

    Column(verticalArrangement = Arrangement.spacedBy(spacing.s2)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
        ) {
            StatCard(
                title = "Total Tasks",
                value = stats.totalTasks.toString(),
                valueColor = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                title = "Completed",
                value = stats.completedTasks.toString(),
                valueColor = colors.accent,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
        ) {
            StatCard(
                title = "Completion Rate",
                value = String.format(Locale.US, "%.0f%%", stats.completionRate),
                valueColor = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                title = "Overdue",
                value = stats.overdueTasks.toString(),
                valueColor = if (stats.overdueTasks > 0) colors.priorityCritical else colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(OS11Theme.radius.lg))
            .background(colors.surfaceRaised)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(OS11Theme.radius.lg))
            .padding(spacing.s3),
    ) {
        Text(
            text = title,
            style = type.sectionLabel.copy(color = colors.textSecondary),
            maxLines = 1,
        )
        Spacer(modifier = Modifier.height(spacing.s1))
        Text(
            text = value,
            style = type.statNumber.copy(color = valueColor, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
    }
}

@Composable
private fun CompletionProgressBar(stats: DashboardOverviewStats) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    val fraction = if (stats.totalTasks > 0) {
        (stats.completedTasks.toFloat() / stats.totalTasks).coerceIn(0f, 1f)
    } else {
        0f
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OS11Theme.radius.lg))
            .background(colors.surfaceRaised)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(OS11Theme.radius.lg))
            .padding(spacing.s4),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Task Progress",
                style = type.panelTitle.copy(color = colors.textPrimary),
            )
            val rateStr = String.format(Locale.US, "%.0f%%", stats.completionRate)
            Text(
                text = "${stats.completedTasks} of ${stats.totalTasks} done ($rateStr)",
                style = type.meta.copy(color = colors.textSecondary),
            )
        }
        Spacer(modifier = Modifier.height(spacing.s3))

        // Progress Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(OS11Theme.radius.full))
                .background(colors.surfaceBase),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(10.dp)
                    .clip(RoundedCornerShape(OS11Theme.radius.full))
                    .background(colors.accent),
            )
        }
    }
}

@Composable
private fun PriorityDistributionCard(priorities: List<PriorityDistributionItem>, totalTasks: Int) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OS11Theme.radius.lg))
            .background(colors.surfaceRaised)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(OS11Theme.radius.lg))
            .padding(spacing.s4),
    ) {
        Text(
            text = "Priority Distribution",
            style = type.panelTitle.copy(color = colors.textPrimary),
        )
        Spacer(modifier = Modifier.height(spacing.s3))

        // Canvas Segmented Bar
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(OS11Theme.radius.full)),
        ) {
            val width = size.width
            val height = size.height

            if (totalTasks == 0) {
                drawRect(color = colors.surfaceBase, size = size)
            } else {
                var currentX = 0f
                for (item in priorities) {
                    val segColor = getPriorityColor(item.priority, colors)
                    val segWidth = width * (item.percentage / 100f)
                    if (segWidth > 0f) {
                        drawRect(
                            color = segColor,
                            topLeft = Offset(currentX, 0f),
                            size = Size(segWidth, height),
                        )
                        currentX += segWidth
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.s3))

        // Priority Legend Items
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            for (item in priorities) {
                val dotColor = getPriorityColor(item.priority, colors)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(dotColor, CircleShape),
                    )
                    Spacer(modifier = Modifier.width(spacing.s1))
                    Text(
                        text = "${item.label} (${item.count})",
                        style = type.meta.copy(color = colors.textSecondary, fontSize = 11.sp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CompletionTrendCard(dailyTrend: List<DailyTrendPoint>) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    var selectedPoint by remember { mutableStateOf<DailyTrendPoint?>(null) }
    val maxCount = (dailyTrend.maxOfOrNull { it.count } ?: 0).coerceAtLeast(4)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OS11Theme.radius.lg))
            .background(colors.surfaceRaised)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(OS11Theme.radius.lg))
            .padding(spacing.s4),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Completion Trend",
                style = type.panelTitle.copy(color = colors.textPrimary),
            )
            selectedPoint?.let { sel ->
                Text(
                    text = "${sel.date}: ${sel.count} completed",
                    style = type.meta.copy(color = colors.accent, fontWeight = FontWeight.SemiBold),
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.s3))

        // Trend Canvas Bar Chart
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .pointerInput(dailyTrend) {
                    detectTapGestures { offset ->
                        if (dailyTrend.isNotEmpty()) {
                            val barSlotWidth = size.width / dailyTrend.size.toFloat()
                            val index = (offset.x / barSlotWidth).toInt().coerceIn(0, dailyTrend.size - 1)
                            selectedPoint = dailyTrend[index]
                        }
                    }
                },
        ) {
            val chartWidth = size.width
            val chartHeight = size.height - 24.dp.toPx()
            val pointCount = dailyTrend.size

            if (pointCount == 0) return@Canvas

            // Baseline guide line
            drawLine(
                color = colors.borderSubtle,
                start = Offset(0f, chartHeight),
                end = Offset(chartWidth, chartHeight),
                strokeWidth = 1.dp.toPx(),
            )

            val slotWidth = chartWidth / pointCount.toFloat()
            val barWidth = (slotWidth * 0.65f).coerceAtMost(20.dp.toPx())

            for (i in dailyTrend.indices) {
                val point = dailyTrend[i]
                val barHeight = if (maxCount > 0) (point.count.toFloat() / maxCount) * chartHeight else 0f
                val centerX = (i * slotWidth) + (slotWidth / 2f)
                val isSelected = point.date == selectedPoint?.date

                val barTop = chartHeight - barHeight
                val barLeft = centerX - (barWidth / 2f)

                if (barHeight > 0f) {
                    drawRoundRect(
                        color = if (isSelected) colors.accent else colors.accent.copy(alpha = 0.7f),
                        topLeft = Offset(barLeft, barTop),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                    )
                } else {
                    // Small dot for zero completions
                    drawCircle(
                        color = colors.borderSubtle,
                        radius = 2.dp.toPx(),
                        center = Offset(centerX, chartHeight - 3.dp.toPx()),
                    )
                }
            }
        }

        // X-Axis Date Labels Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val step = (dailyTrend.size / 6).coerceAtLeast(1)
            for (i in dailyTrend.indices step step) {
                Text(
                    text = dailyTrend[i].dayLabel,
                    style = type.meta.copy(color = colors.textSecondary, fontSize = 10.sp),
                )
            }
        }
    }
}

@Composable
private fun ActivityHeatmapCard(heatmapDays: List<HeatmapDay>) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    var selectedDay by remember { mutableStateOf<HeatmapDay?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OS11Theme.radius.lg))
            .background(colors.surfaceRaised)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(OS11Theme.radius.lg))
            .padding(spacing.s4),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Activity Heatmap",
                style = type.panelTitle.copy(color = colors.textPrimary),
            )
            selectedDay?.let { sel ->
                Text(
                    text = "${sel.date}: ${sel.count} done",
                    style = type.meta.copy(color = colors.accent, fontWeight = FontWeight.SemiBold),
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.s3))

        // Heatmap Matrix
        val cellSize = 13.dp
        val cellGap = 3.dp
        val weeks = HEATMAP_WEEKS

        Row(modifier = Modifier.fillMaxWidth()) {
            // Day of Week Labels Column
            Column(
                modifier = Modifier.padding(end = spacing.s1),
                verticalArrangement = Arrangement.spacedBy(cellGap),
            ) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { idx, label ->
                    Box(
                        modifier = Modifier.size(cellSize),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (idx % 2 == 0) {
                            Text(
                                text = label,
                                style = type.meta.copy(color = colors.textSecondary, fontSize = 9.sp),
                            )
                        }
                    }
                }
            }

            val gridWidth = (cellSize * weeks) + (cellGap * (weeks - 1))
            val gridHeight = (cellSize * DAYS_IN_WEEK) +
                (cellGap * (DAYS_IN_WEEK - 1))

            // Canvas Grid for Weeks
            Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                Canvas(
                    modifier = Modifier
                        .size(width = gridWidth, height = gridHeight)
                        .pointerInput(heatmapDays) {
                            detectTapGestures { offset ->
                                val cellPx = cellSize.toPx()
                                val gapPx = cellGap.toPx()
                                val col = (offset.x / (cellPx + gapPx)).toInt().coerceIn(0, weeks - 1)
                                val row = (offset.y / (cellPx + gapPx)).toInt().coerceIn(
                                    0,
                                    DAYS_IN_WEEK - 1,
                                )
                                val index = col * DAYS_IN_WEEK + row
                                if (index in heatmapDays.indices) {
                                    selectedDay = heatmapDays[index]
                                }
                            }
                        },
                ) {
                    val cellPx = cellSize.toPx()
                    val gapPx = cellGap.toPx()

                    for (col in 0 until weeks) {
                        for (row in 0 until DAYS_IN_WEEK) {
                            val index = col * DAYS_IN_WEEK + row
                            if (index in heatmapDays.indices) {
                                val day = heatmapDays[index]
                                val left = col * (cellPx + gapPx)
                                val top = row * (cellPx + gapPx)

                                val cellColor = when (day.level) {
                                    0 -> colors.surfaceBase
                                    1 -> colors.accent.copy(alpha = 0.25f)
                                    2 -> colors.accent.copy(alpha = 0.5f)
                                    3 -> colors.accent.copy(alpha = 0.75f)
                                    else -> colors.accent
                                }

                                drawRoundRect(
                                    color = cellColor,
                                    topLeft = Offset(left, top),
                                    size = Size(cellPx, cellPx),
                                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                                )

                                if (day.level == 0) {
                                    drawRoundRect(
                                        color = colors.borderSubtle,
                                        topLeft = Offset(left, top),
                                        size = Size(cellPx, cellPx),
                                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                                        style = Stroke(width = 0.5.dp.toPx()),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.s2))

        // Heatmap Legend Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Less",
                style = type.meta.copy(color = colors.textSecondary, fontSize = 10.sp),
            )
            Spacer(modifier = Modifier.width(spacing.s1))
            listOf(
                colors.surfaceBase,
                colors.accent.copy(alpha = 0.25f),
                colors.accent.copy(alpha = 0.5f),
                colors.accent.copy(alpha = 0.75f),
                colors.accent,
            ).forEach { swatchColor ->
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(swatchColor)
                        .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(2.dp)),
                )
                Spacer(modifier = Modifier.width(2.dp))
            }
            Text(
                text = "More",
                style = type.meta.copy(color = colors.textSecondary, fontSize = 10.sp),
            )
        }
    }
}

private fun getPriorityColor(priority: Int, colors: com.os11.app.ui.theme.OS11Colors): Color = when (priority) {
    4 -> colors.priorityCritical
    3 -> colors.priorityHigh
    2 -> colors.priorityMedium
    1 -> colors.priorityLow
    else -> colors.priorityNone
}
