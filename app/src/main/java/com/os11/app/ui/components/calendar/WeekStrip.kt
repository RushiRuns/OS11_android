package com.os11.app.ui.components.calendar

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.os11.app.ui.components.OSIconButton
import com.os11.app.ui.theme.OS11Theme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Collapsible horizontal week strip calendar component per UTILITIES.md.
 * Allows filtering tasks by date with visual indicators for days with scheduled tasks.
 */
@Composable
fun WeekStrip(
    referenceDate: LocalDate,
    selectedDate: LocalDate?,
    datesWithTasks: Set<String>,
    onDateSelected: (LocalDate?) -> Unit,
    onWeekOffsetChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val radius = OS11Theme.radius

    val startOfWeek = referenceDate.with(DayOfWeek.MONDAY)
    val days = (0L..6L).map { startOfWeek.plusDays(it) }
    val today = LocalDate.now()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surfaceBase)
            .padding(vertical = spacing.s2),
    ) {
        // Navigation header: Month Year + Previous/Next buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "${startOfWeek.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${startOfWeek.year}",
                style = type.subViewTitle.copy(color = colors.textPrimary),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.s1),
            ) {
                // All / Clear filter button
                if (selectedDate != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(radius.xs))
                            .clickable(role = Role.Button) { onDateSelected(null) }
                            .padding(horizontal = spacing.s2, vertical = spacing.s1),
                    ) {
                        Text(
                            text = "Show All",
                            style = type.meta.copy(color = colors.accent),
                        )
                    }
                }

                OSIconButton(
                    onClick = { onWeekOffsetChange(-1) },
                    contentDescription = "Previous week",
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }

                OSIconButton(
                    onClick = { onWeekOffsetChange(1) },
                    contentDescription = "Next week",
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.s2))

        // 7 days row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s2),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            days.forEach { date ->
                val isSelected = selectedDate == date
                val isToday = date == today
                val dateIso = date.toString()
                val hasTasks = datesWithTasks.contains(dateIso)

                val dayOfWeekLabel = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    .take(1)

                Column(
                    modifier = Modifier
                        .size(width = 44.dp, height = 56.dp)
                        .clip(RoundedCornerShape(radius.sm))
                        .background(
                            when {
                                isSelected -> colors.accent
                                isToday -> colors.surfaceHover
                                else -> androidx.compose.ui.graphics.Color.Transparent
                            },
                        )
                        .then(
                            if (isToday && !isSelected) {
                                Modifier.border(spacing.px, colors.accent, RoundedCornerShape(radius.sm))
                            } else {
                                Modifier
                            },
                        )
                        .clickable(role = Role.Button) {
                            if (isSelected) {
                                onDateSelected(null)
                            } else {
                                onDateSelected(date)
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = dayOfWeekLabel,
                        style = type.meta.copy(
                            color = if (isSelected) colors.textOnAccent else colors.textTertiary,
                        ),
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = date.dayOfMonth.toString(),
                        style = type.body.copy(
                            color = if (isSelected) colors.textOnAccent else colors.textPrimary,
                        ),
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Task indicator dot
                    if (hasTasks) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) colors.textOnAccent else colors.accent),
                        )
                    } else {
                        Spacer(modifier = Modifier.size(4.dp))
                    }
                }
            }
        }
    }
}
