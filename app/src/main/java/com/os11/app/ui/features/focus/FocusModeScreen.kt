package com.os11.app.ui.features.focus

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.os11.app.service.pomodoro.PomodoroPhase
import com.os11.app.service.pomodoro.PomodoroStatus
import com.os11.app.ui.components.OSButton
import com.os11.app.ui.components.OSButtonVariant
import com.os11.app.ui.components.OSIconButton
import com.os11.app.ui.components.OSSwitch
import com.os11.app.ui.theme.OS11Theme
import com.os11.app.ui.util.rememberReducedMotion

private const val SECONDS_PER_MINUTE = 60
private const val GAUGE_SIZE_DP = 250
private const val STROKE_WIDTH_DP = 16
private const val SWEEP_MAX = 360f

/**
 * Full-screen Focus Mode per ANDROID_VS_DESKTOP.md:
 * - Current task and large timer
 * - Optional keep-screen-on toggle
 * - Distraction-free actions (Complete, Pause/Play, Skip, Exit)
 */
@Composable
fun FocusModeScreen(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FocusModeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val context = LocalContext.current

    // Keep-screen-on handling via WindowManager FLAG_KEEP_SCREEN_ON
    DisposableEffect(state.keepScreenOn) {
        val activity = context as? Activity
        if (state.keepScreenOn) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    BackHandler(onBack = onExit)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surfaceBase)
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.s4, vertical = spacing.s3),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // 1. Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            OSIconButton(
                onClick = onExit,
                contentDescription = "Exit focus mode",
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = colors.textPrimary,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                Text(
                    text = "Keep Screen On",
                    style = type.meta.copy(color = colors.textSecondary),
                )
                OSSwitch(
                    checked = state.keepScreenOn,
                    onCheckedChange = { viewModel.toggleKeepScreenOn() },
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.s4))

        // 2. Task Details Card
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s2),
        ) {
            val phaseLabel = when (state.phase) {
                PomodoroPhase.WORK -> "FOCUSING ON"
                PomodoroPhase.SHORT_BREAK -> "SHORT BREAK"
                PomodoroPhase.LONG_BREAK -> "LONG BREAK"
            }
            Text(
                text = phaseLabel,
                style = type.meta.copy(
                    color = colors.accent,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                ),
            )

            Spacer(modifier = Modifier.height(spacing.s2))

            Text(
                text = state.taskTitle,
                style = type.viewTitle.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                ),
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )

            if (!state.taskNotes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(spacing.s1))
                Text(
                    text = state.taskNotes.orEmpty(),
                    style = type.body.copy(color = colors.textSecondary),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.s6))

        // 3. Circular Countdown Gauge
        FocusTimerGauge(
            remainingSeconds = state.remainingSeconds,
            progress = state.progress,
            status = state.status,
        )

        Spacer(modifier = Modifier.height(spacing.s6))

        // 4. Primary Actions
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.s3),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.s4),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Skip / Reset
                OSIconButton(
                    onClick = { viewModel.skipToNext() },
                    contentDescription = "Skip to next phase",
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(24.dp),
                    )
                }

                // Play / Pause FAB
                val isRunning = state.status == PomodoroStatus.RUNNING
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(colors.accent)
                        .clickable(role = Role.Button) { viewModel.togglePlayPause() }
                        .semantics {
                            contentDescription = if (isRunning) "Pause timer" else "Resume timer"
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = colors.textOnAccent,
                        modifier = Modifier.size(32.dp),
                    )
                }

                // Reset timer
                OSIconButton(
                    onClick = { viewModel.resetTimer() },
                    contentDescription = "Reset timer",
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            // Complete Task button (if task exists)
            if (state.taskId != null && !state.isTaskCompleted) {
                OSButton(
                    onClick = { viewModel.completeTask(onCompleted = onExit) },
                    variant = OSButtonVariant.Primary,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(0.8f),
                ) {
                    Text("Complete Task")
                }
            } else if (state.isTaskCompleted) {
                Text(
                    text = "✓ Task Completed",
                    style = type.body.copy(color = colors.accent, fontWeight = FontWeight.Bold),
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.s4))
    }
}

@Composable
private fun FocusTimerGauge(remainingSeconds: Int, progress: Float, status: PomodoroStatus) {
    val colors = OS11Theme.colors
    val type = OS11Theme.type
    val isReducedMotion = rememberReducedMotion()

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = if (isReducedMotion) tween(0) else tween(durationMillis = 300),
        label = "focus_gauge_progress",
    )

    val minutes = remainingSeconds / SECONDS_PER_MINUTE
    val seconds = remainingSeconds % SECONDS_PER_MINUTE
    val timeFormatted = "%02d:%02d".format(minutes, seconds)

    val ringTrackColor = colors.surfaceRaised
    val ringFillColor = colors.accent

    Box(
        modifier = Modifier
            .size(GAUGE_SIZE_DP.dp)
            .semantics {
                contentDescription = "Timer: $minutes minutes $seconds seconds remaining"
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidthPx = STROKE_WIDTH_DP.dp.toPx()
            val diameter = size.minDimension - strokeWidthPx
            val arcSize = Size(diameter, diameter)
            val arcOffset = Offset(strokeWidthPx / 2f, strokeWidthPx / 2f)

            // Background track
            drawArc(
                color = ringTrackColor,
                startAngle = 0f,
                sweepAngle = SWEEP_MAX,
                useCenter = false,
                topLeft = arcOffset,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
            )

            // Progress arc
            if (animatedProgress > 0f) {
                drawArc(
                    color = ringFillColor,
                    startAngle = -90f,
                    sweepAngle = animatedProgress * SWEEP_MAX,
                    useCenter = false,
                    topLeft = arcOffset,
                    size = arcSize,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = timeFormatted,
                style = type.statNumber.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 44.sp,
                ),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when (status) {
                    PomodoroStatus.RUNNING -> "In Focus"
                    PomodoroStatus.PAUSED -> "Paused"
                    PomodoroStatus.COMPLETED -> "Completed"
                    else -> "Ready"
                },
                style = type.meta.copy(
                    color = if (status == PomodoroStatus.RUNNING) colors.accent else colors.textSecondary,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }
}
