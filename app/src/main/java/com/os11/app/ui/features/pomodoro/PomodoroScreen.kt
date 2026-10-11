package com.os11.app.ui.features.pomodoro

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.os11.app.service.pomodoro.PomodoroPhase
import com.os11.app.service.pomodoro.PomodoroStatus
import com.os11.app.service.task.TaskItem
import com.os11.app.ui.components.OSButton
import com.os11.app.ui.components.OSIconButton
import com.os11.app.ui.components.OSSearchField
import com.os11.app.ui.components.OSSheet
import com.os11.app.ui.theme.OS11Theme
import com.os11.app.ui.util.rememberReducedMotion

private const val SECONDS_PER_MINUTE = 60
private const val GAUGE_SIZE_DP = 240
private const val STROKE_WIDTH_DP = 14
private const val SWEEP_MAX = 360f

/**
 * Screen for Pomodoro focus sessions, breaks, and session cycle tracking (ADR-A09).
 */
@Composable
fun PomodoroScreen(
    modifier: Modifier = Modifier,
    onNavigateToFocusMode: (() -> Unit)? = null,
    viewModel: PomodoroViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val availableTasks by viewModel.availableTasks.collectAsState()
    var showTaskPicker by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surfaceBase)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.s4, vertical = spacing.s3),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.s4),
    ) {
        // 1. Phase selector pills + settings button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PhaseSelectorRow(
                selectedPhase = uiState.phase,
                onSelectPhase = { viewModel.selectPhase(it) },
                workMinutes = uiState.workMinutesSetting,
                breakMinutes = uiState.breakMinutesSetting,
                longBreakMinutes = uiState.longBreakMinutesSetting,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onNavigateToFocusMode != null) {
                    IconButton(
                        onClick = onNavigateToFocusMode,
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Fullscreen Focus Mode",
                            tint = colors.accent,
                        )
                    }
                }

                IconButton(
                    onClick = { showSettingsSheet = true },
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Pomodoro Settings",
                        tint = colors.textSecondary,
                    )
                }
            }
        }

        // 2. Circular Progress Gauge
        PomodoroGauge(
            remainingSeconds = uiState.remainingSeconds,
            progress = uiState.progress,
            phase = uiState.phase,
            status = uiState.status,
        )

        // 3. Session dots indicator
        SessionDotsIndicator(
            completedSessions = uiState.completedSessionsInCycle,
            totalSessions = uiState.sessionsBeforeLongBreak,
            isCurrentRunning = uiState.status == PomodoroStatus.RUNNING && uiState.phase == PomodoroPhase.WORK,
        )

        // 4. Linked task card
        LinkedTaskCard(
            taskId = uiState.taskId,
            taskTitle = uiState.taskTitle,
            onPickTask = { showTaskPicker = true },
            onUnlinkTask = { viewModel.unlinkTask() },
        )

        // 5. Timer Controls
        PomodoroControls(
            status = uiState.status,
            phase = uiState.phase,
            onTogglePlayPause = { viewModel.togglePlayPause() },
            onReset = { viewModel.resetTimer() },
            onSkip = { viewModel.skipToNext() },
        )

        // 6. Today's Summary
        TodayStatsCard(
            completedSessions = uiState.todayCompletedSessions,
            focusMinutes = uiState.todayFocusMinutes,
        )

        Spacer(modifier = Modifier.height(spacing.s4))
    }

    if (showTaskPicker) {
        PomodoroTaskPickerSheet(
            tasks = availableTasks,
            onSelectTask = { task ->
                viewModel.linkTask(task)
                showTaskPicker = false
            },
            onDismissRequest = { showTaskPicker = false },
        )
    }

    if (showSettingsSheet) {
        PomodoroSettingsSheet(
            workMin = uiState.workMinutesSetting,
            breakMin = uiState.breakMinutesSetting,
            longBreakMin = uiState.longBreakMinutesSetting,
            sessionsBefore = uiState.sessionsBeforeLongBreak,
            onSave = { w, b, lb, s ->
                viewModel.updateSettings(w, b, lb, s)
                showSettingsSheet = false
            },
            onDismissRequest = { showSettingsSheet = false },
        )
    }
}

@Composable
private fun PhaseSelectorRow(
    selectedPhase: PomodoroPhase,
    onSelectPhase: (PomodoroPhase) -> Unit,
    workMinutes: Int,
    breakMinutes: Int,
    longBreakMinutes: Int,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val radius = OS11Theme.radius

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(radius.full))
            .background(colors.surfaceSidebar)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(spacing.s1),
    ) {
        PomodoroPhase.entries.forEach { phase ->
            val isSelected = phase == selectedPhase
            val label = when (phase) {
                PomodoroPhase.WORK -> "Focus ${workMinutes}m"
                PomodoroPhase.SHORT_BREAK -> "Break ${breakMinutes}m"
                PomodoroPhase.LONG_BREAK -> "Long ${longBreakMinutes}m"
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(radius.full))
                    .background(if (isSelected) colors.surfaceRaised else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelectPhase(phase) }
                    .padding(horizontal = spacing.s3, vertical = spacing.s2),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = type.meta.copy(
                        color = if (isSelected) colors.textPrimary else colors.textSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    ),
                )
            }
        }
    }
}

@Composable
private fun PomodoroGauge(remainingSeconds: Int, progress: Float, phase: PomodoroPhase, status: PomodoroStatus) {
    val colors = OS11Theme.colors
    val type = OS11Theme.type
    val isReducedMotion = rememberReducedMotion()

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = if (isReducedMotion) tween(0) else tween(durationMillis = 300),
        label = "pomodoro_gauge_progress",
    )

    val minutes = remainingSeconds / SECONDS_PER_MINUTE
    val seconds = remainingSeconds % SECONDS_PER_MINUTE
    val timeFormatted = String.format(java.util.Locale.ROOT, "%02d:%02d", minutes, seconds)

    val trackColor = colors.surfaceSidebar
    val activeColor = if (phase == PomodoroPhase.WORK) colors.accent else colors.success

    Box(
        modifier = Modifier.size(GAUGE_SIZE_DP.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = STROKE_WIDTH_DP.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)

            // Background circle
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = SWEEP_MAX,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )

            // Progress arc (starts from top = -90 deg)
            drawArc(
                color = activeColor,
                startAngle = -90f,
                sweepAngle = animatedProgress * SWEEP_MAX,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = timeFormatted,
                style = type.viewTitle.copy(
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                ),
            )

            Spacer(modifier = Modifier.height(4.dp))

            val statusText = when (status) {
                PomodoroStatus.RUNNING -> if (phase == PomodoroPhase.WORK) "FOCUSING" else "ON BREAK"
                PomodoroStatus.PAUSED -> "PAUSED"
                PomodoroStatus.COMPLETED -> "FINISHED"
                PomodoroStatus.IDLE -> "READY"
            }

            Text(
                text = statusText,
                style = type.sectionLabel.copy(
                    color = if (status == PomodoroStatus.RUNNING) activeColor else colors.textTertiary,
                    letterSpacing = 1.5.sp,
                ),
            )
        }
    }
}

@Composable
private fun SessionDotsIndicator(completedSessions: Int, totalSessions: Int, isCurrentRunning: Boolean) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.s1),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (i in 0 until totalSessions) {
                val isCompleted = i < completedSessions
                val isCurrent = i == completedSessions && isCurrentRunning

                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isCompleted -> colors.accent
                                isCurrent -> colors.accent.copy(alpha = 0.35f)
                                else -> colors.surfaceSidebar
                            },
                        )
                        .border(
                            width = 1.5.dp,
                            color = when {
                                isCompleted -> colors.accent
                                isCurrent -> colors.accent
                                else -> colors.borderGlass
                            },
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = colors.textOnAccent,
                            modifier = Modifier.size(10.dp),
                        )
                    }
                }
            }
        }

        Text(
            text = "Cycle: session $completedSessions of $totalSessions • Long break after $totalSessions",
            style = type.meta.copy(color = colors.textTertiary),
        )
    }
}

@Composable
private fun LinkedTaskCard(taskId: String?, taskTitle: String?, onPickTask: () -> Unit, onUnlinkTask: () -> Unit) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val radius = OS11Theme.radius

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radius.lg))
            .background(colors.surfaceSidebar)
            .border(1.dp, colors.borderGlass, RoundedCornerShape(radius.lg))
            .padding(spacing.s3),
    ) {
        if (taskId != null && taskTitle != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "LINKED TASK",
                        style = type.sectionLabel.copy(color = colors.accent),
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = taskTitle,
                        style = type.taskTitle.copy(color = colors.textPrimary),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPickTask) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Change task",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    IconButton(onClick = onUnlinkTask) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Unlink task",
                            tint = colors.textTertiary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClick = onPickTask),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Link a Task",
                        style = type.taskTitle.copy(color = colors.textPrimary),
                    )
                    Text(
                        text = "Connect a task to track sessions and pomodoro counts",
                        style = type.meta.copy(color = colors.textSecondary),
                    )
                }

                OSButton(
                    onClick = onPickTask,
                ) {
                    Text("Select")
                }
            }
        }
    }
}

@Composable
private fun PomodoroControls(
    status: PomodoroStatus,
    phase: PomodoroPhase,
    onTogglePlayPause: () -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Reset / Stop button
        OSIconButton(
            onClick = onReset,
            contentDescription = "Reset timer",
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = null,
                tint = colors.textSecondary,
            )
        }

        Spacer(modifier = Modifier.width(spacing.s4))

        // Main action button
        val mainButtonText = when (status) {
            PomodoroStatus.IDLE -> if (phase == PomodoroPhase.WORK) "Start Focus" else "Start Break"
            PomodoroStatus.RUNNING -> "Pause"
            PomodoroStatus.PAUSED -> "Resume"
            PomodoroStatus.COMPLETED -> "Next Phase"
        }
        val mainIcon = when (status) {
            PomodoroStatus.RUNNING -> Icons.Default.Close
            else -> Icons.Default.PlayArrow
        }

        OSButton(
            onClick = onTogglePlayPause,
            leadingIcon = {
                Icon(
                    imageVector = mainIcon,
                    contentDescription = null,
                    tint = colors.textOnAccent,
                )
            },
            modifier = Modifier.height(52.dp),
        ) {
            Text(
                text = mainButtonText,
                style = OS11Theme.type.body.copy(fontWeight = FontWeight.SemiBold),
            )
        }

        Spacer(modifier = Modifier.width(spacing.s4))

        // Skip to next button
        OSIconButton(
            onClick = onSkip,
            contentDescription = "Skip to next phase",
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = colors.textSecondary,
            )
        }
    }
}

@Composable
private fun TodayStatsCard(completedSessions: Int, focusMinutes: Int) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val radius = OS11Theme.radius

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radius.lg))
            .background(colors.surfaceSidebar)
            .border(1.dp, colors.borderGlass, RoundedCornerShape(radius.lg))
            .padding(spacing.s3),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$completedSessions 🍅",
                style = type.statNumber.copy(color = colors.accent),
            )
            Text(
                text = "Sessions today",
                style = type.meta.copy(color = colors.textSecondary),
            )
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(36.dp)
                .background(colors.borderGlass),
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${focusMinutes}m",
                style = type.statNumber.copy(color = colors.textPrimary),
            )
            Text(
                text = "Focus time today",
                style = type.meta.copy(color = colors.textSecondary),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PomodoroTaskPickerSheet(
    tasks: List<TaskItem>,
    onSelectTask: (TaskItem) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    val filtered = remember(tasks, searchQuery) {
        if (searchQuery.isBlank()) {
            tasks
        } else {
            tasks.filter { it.title.contains(searchQuery, ignoreCase = true) }
        }
    }

    OSSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.s4),
        ) {
            Text(
                text = "Select Task to Focus On",
                style = type.subViewTitle.copy(color = colors.textPrimary),
            )

            Spacer(modifier = Modifier.height(spacing.s3))

            OSSearchField(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = {},
                placeholder = "Search active tasks...",
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(spacing.s3))

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No active tasks found",
                        style = type.meta.copy(color = colors.textTertiary),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing.s2),
                ) {
                    items(filtered, key = { it.id }) { task ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(OS11Theme.radius.md))
                                .clickable { onSelectTask(task) }
                                .padding(spacing.s3),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = task.title,
                                style = type.taskTitle.copy(color = colors.textPrimary),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PomodoroSettingsSheet(
    workMin: Int,
    breakMin: Int,
    longBreakMin: Int,
    sessionsBefore: Int,
    onSave: (Int, Int, Int, Int) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var work by remember { mutableStateOf(workMin) }
    var sBreak by remember { mutableStateOf(breakMin) }
    var lBreak by remember { mutableStateOf(longBreakMin) }
    var sessions by remember { mutableStateOf(sessionsBefore) }

    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    OSSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.s4),
            verticalArrangement = Arrangement.spacedBy(spacing.s3),
        ) {
            Text(
                text = "Pomodoro Timer Settings",
                style = type.subViewTitle.copy(color = colors.textPrimary),
            )

            DurationPickerRow(
                label = "Focus Duration",
                minutes = work,
                options = listOf(15, 20, 25, 30, 45, 50, 60),
                onSelect = { work = it },
            )

            DurationPickerRow(
                label = "Short Break",
                minutes = sBreak,
                options = listOf(3, 5, 10, 15),
                onSelect = { sBreak = it },
            )

            DurationPickerRow(
                label = "Long Break",
                minutes = lBreak,
                options = listOf(10, 15, 20, 25, 30),
                onSelect = { lBreak = it },
            )

            DurationPickerRow(
                label = "Sessions Before Long Break",
                minutes = sessions,
                options = listOf(2, 3, 4, 5, 6),
                onSelect = { sessions = it },
            )

            Spacer(modifier = Modifier.height(spacing.s2))

            OSButton(
                onClick = { onSave(work, sBreak, lBreak, sessions) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save Settings")
            }
        }
    }
}

@Composable
private fun DurationPickerRow(label: String, minutes: Int, options: List<Int>, onSelect: (Int) -> Unit) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    Column(verticalArrangement = Arrangement.spacedBy(spacing.s1)) {
        Text(
            text = "$label: $minutes min",
            style = type.meta.copy(color = colors.textPrimary, fontWeight = FontWeight.SemiBold),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            modifier = Modifier.fillMaxWidth(),
        ) {
            options.forEach { opt ->
                val isSelected = opt == minutes
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(OS11Theme.radius.full))
                        .background(if (isSelected) colors.accent else colors.surfaceSidebar)
                        .clickable { onSelect(opt) }
                        .padding(horizontal = spacing.s3, vertical = spacing.s1),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "$opt",
                        style = type.meta.copy(
                            color = if (isSelected) colors.textOnAccent else colors.textSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        ),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
