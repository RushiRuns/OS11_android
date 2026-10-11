package com.os11.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.os11.app.service.pomodoro.PomodoroState
import com.os11.app.service.pomodoro.PomodoroStatus
import com.os11.app.ui.theme.OS11Theme

/**
 * In-app persistent Pomodoro pill displayed across the app shell (ADR-A09).
 * Shows current remaining time, phase, and quick controls.
 */
@Composable
fun PomodoroPill(
    state: PomodoroState,
    onClick: () -> Unit,
    onTogglePause: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val radius = OS11Theme.radius

    val now = System.currentTimeMillis()
    val remaining = state.remainingSeconds(now)
    val minutes = remaining / SECONDS_PER_MINUTE
    val seconds = remaining % SECONDS_PER_MINUTE
    val formattedTime = String.format(java.util.Locale.ROOT, "%02d:%02d", minutes, seconds)

    AnimatedVisibility(
        visible = state.isActive,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut(),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s4, vertical = spacing.s1)
                .clip(RoundedCornerShape(radius.full))
                .background(colors.pomodoroPillBg)
                .border(
                    BorderStroke(1.dp, colors.pomodoroPillBorder),
                    shape = RoundedCornerShape(radius.full),
                )
                .clickable(
                    role = Role.Button,
                    onClickLabel = "Open Pomodoro",
                    onClick = onClick,
                )
                .padding(horizontal = spacing.s3, vertical = spacing.s1),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = "🍅",
                        style = type.body,
                    )
                    Spacer(modifier = Modifier.width(spacing.s2))
                    Text(
                        text = formattedTime,
                        style = type.mono.copy(
                            color = colors.pomodoroPillText,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Spacer(modifier = Modifier.width(spacing.s2))
                    Text(
                        text = "• ${state.phase.label}",
                        style = type.meta.copy(
                            color = colors.pomodoroPillText.copy(alpha = 0.8f),
                        ),
                    )

                    state.taskTitle?.let { title ->
                        Spacer(modifier = Modifier.width(spacing.s2))
                        Text(
                            text = "($title)",
                            style = type.meta.copy(color = colors.textSecondary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onTogglePause,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = if (state.status == PomodoroStatus.RUNNING) {
                                Icons.Default.Close // or Pause
                            } else {
                                Icons.Default.PlayArrow
                            },
                            contentDescription = if (state.status == PomodoroStatus.RUNNING) "Pause" else "Resume",
                            tint = colors.pomodoroPillText,
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    IconButton(
                        onClick = onStop,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Stop Pomodoro",
                            tint = colors.textTertiary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

private const val SECONDS_PER_MINUTE = 60
