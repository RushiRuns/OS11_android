package com.os11.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.os11.app.ui.theme.OS11Theme

/**
 * 15-minute time picker row list.
 */
@Composable
fun OSTimeList(
    selectedTime: String?,
    onTimeSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val radius = OS11Theme.radius
    val layout = OS11Theme.layout
    val type = OS11Theme.type

    // Generate 15-minute intervals throughout 24 hours
    val intervals = remember {
        val list = mutableListOf<String>()
        for (h in 0..23) {
            for (m in listOf(0, 15, 30, 45)) {
                list.add(String.format(java.util.Locale.US, "%02d:%02d", h, m))
            }
        }
        list
    }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 240.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.px),
    ) {
        items(intervals) { time ->
            val isSelected = time == selectedTime
            val interactionSource = remember { MutableInteractionSource() }
            val bgColor = if (isSelected) colors.surfaceSelected else colors.surfaceSidebar
            val textColor = if (isSelected) colors.accent else colors.textPrimary

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = layout.minTouchTarget)
                    .clip(RoundedCornerShape(radius.sm))
                    .background(bgColor)
                    .clickable(
                        role = Role.Button,
                        interactionSource = interactionSource,
                        indication = ripple(color = colors.surfaceHover),
                        onClick = { onTimeSelected(time) },
                    )
                    .padding(horizontal = spacing.s4, vertical = spacing.s2),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = time,
                    style = type.body.copy(color = textColor),
                )
            }
        }
    }
}

/**
 * Quick date selection sheet container with common smart dates.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun OSDatePickerSheet(
    onDismissRequest: () -> Unit,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val colors = OS11Theme.colors

    OSSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.s4),
            verticalArrangement = Arrangement.spacedBy(spacing.s3),
        ) {
            Text(
                text = "Set When",
                style = type.panelTitle.copy(color = colors.textPrimary),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                OSChip(
                    label = "Today",
                    onClick = {
                        onDateSelected("today")
                        onDismissRequest()
                    },
                )
                OSChip(
                    label = "Tomorrow",
                    onClick = {
                        onDateSelected("tomorrow")
                        onDismissRequest()
                    },
                )
                OSChip(
                    label = "Next Week",
                    onClick = {
                        onDateSelected("next_week")
                        onDismissRequest()
                    },
                )
                OSChip(
                    label = "Someday",
                    onClick = {
                        onDateSelected("someday")
                        onDismissRequest()
                    },
                )
            }
        }
    }
}
