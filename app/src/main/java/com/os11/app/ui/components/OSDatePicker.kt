package com.os11.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.os11.app.ui.theme.OS11Theme
import com.os11.domain.nlp.NlpParseResult
import com.os11.domain.nlp.NlpParser
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * 15-minute time picker row list.
 */
@Composable
fun OSTimeList(selectedTime: String?, onTimeSelected: (String) -> Unit, modifier: Modifier = Modifier) {
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
                list.add(String.format(Locale.US, "%02d:%02d", h, m))
            }
        }
        list
    }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp),
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
 * Full Date Picker Sheet satisfying Phase 8 specifications:
 * - Natural-language input field powered by domain NlpParser
 * - Quick options (Today, Tomorrow, Next Week, Someday, Clear)
 * - Interactive calendar grid with month navigation and today highlight
 * - 15-minute time list with quick time presets
 * - Non-gesture accessible TalkBack paths
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OSDatePickerSheet(
    onDismissRequest: () -> Unit,
    onDateSelected: (dueDate: String?, dueTime: String?) -> Unit,
    modifier: Modifier = Modifier,
    initialDate: String? = null,
    initialTime: String? = null,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    val today = remember { LocalDate.now() }
    var selectedDate by remember {
        mutableStateOf(
            initialDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
        )
    }
    var selectedTime by remember { mutableStateOf(initialTime) }
    var displayedMonth by remember {
        mutableStateOf(
            selectedDate?.let { YearMonth.from(it) } ?: YearMonth.from(today),
        )
    }
    var naturalLanguageInput by remember { mutableStateOf("") }
    var showTimePicker by remember { mutableStateOf(selectedTime != null) }

    val parsedNlp = remember(naturalLanguageInput) {
        if (naturalLanguageInput.isNotBlank()) {
            NlpParser.parse(naturalLanguageInput, today)
        } else {
            null
        }
    }

    OSSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.s4)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.s3),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DateRange,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Set When",
                        style = type.panelTitle.copy(color = colors.textPrimary),
                    )
                }

                if (selectedDate != null || selectedTime != null) {
                    OSButton(
                        onClick = {
                            selectedDate = null
                            selectedTime = null
                            onDateSelected(null, null)
                            onDismissRequest()
                        },
                        variant = OSButtonVariant.Ghost,
                    ) {
                        Text("Clear Date")
                    }
                }
            }

            // 1. Natural Language Input Field
            OSDatePickerNlpInput(
                input = naturalLanguageInput,
                onInputChange = { naturalLanguageInput = it },
                parsed = parsedNlp,
                onApply = { date, time ->
                    date?.let {
                        selectedDate = it
                        displayedMonth = YearMonth.from(it)
                    }
                    if (time != null) {
                        selectedTime = time
                        showTimePicker = true
                    }
                    naturalLanguageInput = ""
                },
            )

            // 2. Quick Options Row
            OSDatePickerQuickOptions(
                today = today,
                selectedDate = selectedDate,
                onSelectDate = { date ->
                    selectedDate = date
                    displayedMonth = YearMonth.from(date)
                },
                onSelectSomeday = {
                    selectedDate = null
                    selectedTime = null
                    onDateSelected("someday", null)
                    onDismissRequest()
                },
            )

            // 3. Calendar Grid Section
            OSDatePickerCalendar(
                displayedMonth = displayedMonth,
                onPrevMonth = { displayedMonth = displayedMonth.minusMonths(1) },
                onNextMonth = { displayedMonth = displayedMonth.plusMonths(1) },
                selectedDate = selectedDate,
                today = today,
                onDateSelected = { date ->
                    selectedDate = if (selectedDate == date) null else date
                },
            )

            // 4. 15-Minute Time List Section
            OSDatePickerTimeSection(
                selectedTime = selectedTime,
                showTimePicker = showTimePicker,
                onToggleShowTime = { showTimePicker = !showTimePicker },
                onTimeSelected = { selectedTime = it },
                onClearTime = {
                    selectedTime = null
                    showTimePicker = false
                },
            )

            // 5. Actions Row (Cancel & Confirm)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                OSButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.weight(1f),
                    variant = OSButtonVariant.Secondary,
                ) {
                    Text("Cancel")
                }

                OSButton(
                    onClick = {
                        val dateIso = selectedDate?.format(DateTimeFormatter.ISO_LOCAL_DATE)
                        onDateSelected(dateIso, selectedTime)
                        onDismissRequest()
                    },
                    modifier = Modifier.weight(1f),
                    variant = OSButtonVariant.Primary,
                ) {
                    Text("Done")
                }
            }
        }
    }
}

@Composable
private fun OSDatePickerNlpInput(
    input: String,
    onInputChange: (String) -> Unit,
    parsed: NlpParseResult?,
    onApply: (LocalDate?, String?) -> Unit,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val radius = OS11Theme.radius

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.s1),
    ) {
        OSTextField(
            value = input,
            onValueChange = onInputChange,
            placeholder = "Type date e.g. 'tomorrow 5pm', 'in 3 days'...",
            modifier = Modifier.fillMaxWidth(),
        )

        if (parsed != null && (parsed.dueDate != null || parsed.dueTime != null)) {
            val date = parsed.dueDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radius.sm))
                    .background(colors.surfaceSidebar)
                    .clickable(role = Role.Button) {
                        onApply(date, parsed.dueTime)
                    }
                    .padding(horizontal = spacing.s3, vertical = spacing.s2),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val label = "Parsed: ${parsed.dueDate ?: ""} ${parsed.dueTime ?: ""}".trim()
                Text(
                    text = label,
                    style = type.meta.copy(color = colors.accent),
                )
                Text(
                    text = "Tap to apply",
                    style = type.meta.copy(color = colors.textSecondary),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OSDatePickerQuickOptions(
    today: LocalDate,
    selectedDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit,
    onSelectSomeday: () -> Unit,
) {
    val spacing = OS11Theme.spacing
    val tomorrow = remember(today) { today.plusDays(1) }
    val nextWeek = remember(today) { today.plusWeeks(1) }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.s2),
        verticalArrangement = Arrangement.spacedBy(spacing.s2),
    ) {
        OSChip(
            label = "Today",
            onClick = { onSelectDate(today) },
            selected = selectedDate == today,
        )
        OSChip(
            label = "Tomorrow",
            onClick = { onSelectDate(tomorrow) },
            selected = selectedDate == tomorrow,
        )
        OSChip(
            label = "Next Week",
            onClick = { onSelectDate(nextWeek) },
            selected = selectedDate == nextWeek,
        )
        OSChip(
            label = "Someday",
            onClick = onSelectSomeday,
        )
    }
}

@Composable
private fun OSDatePickerCalendar(
    displayedMonth: YearMonth,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    selectedDate: LocalDate?,
    today: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val radius = OS11Theme.radius

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radius.md))
            .background(colors.surfaceSidebar)
            .padding(spacing.s3),
    ) {
        // Month Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val monthName = displayedMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
            Text(
                text = "$monthName ${displayedMonth.year}",
                style = type.subViewTitle.copy(color = colors.textPrimary),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(spacing.s1)) {
                OSIconButton(
                    onClick = onPrevMonth,
                    contentDescription = "Previous month",
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
                OSIconButton(
                    onClick = onNextMonth,
                    contentDescription = "Next month",
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

        // Days of Week Header (Mon .. Sun)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { dayLabel ->
                Text(
                    text = dayLabel,
                    style = type.meta.copy(color = colors.textTertiary),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.size(36.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.s1))

        // 7-Column Calendar Month Grid
        val firstOfMonth = displayedMonth.atDay(1)
        val daysInMonth = displayedMonth.lengthOfMonth()
        val startDayOffset = (firstOfMonth.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
        val totalCells = startDayOffset + daysInMonth
        val totalRows = (totalCells + 6) / 7

        for (row in 0 until totalRows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                for (col in 0..6) {
                    val cellIndex = row * 7 + col
                    val dayNumber = cellIndex - startDayOffset + 1

                    if (dayNumber in 1..daysInMonth) {
                        val date = displayedMonth.atDay(dayNumber)
                        val isSelected = selectedDate == date
                        val isToday = date == today
                        val cellBg = if (isSelected) {
                            colors.accent
                        } else {
                            androidx.compose.ui.graphics.Color.Transparent
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(cellBg)
                                .then(
                                    if (isToday && !isSelected) {
                                        Modifier.border(spacing.px, colors.accent, CircleShape)
                                    } else {
                                        Modifier
                                    },
                                )
                                .clickable(role = Role.Button) {
                                    onDateSelected(date)
                                }
                                .semantics {
                                    val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                                    contentDescription = if (isSelected) "$dateStr, selected" else dateStr
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = dayNumber.toString(),
                                style = type.body.copy(
                                    color = when {
                                        isSelected -> colors.textOnAccent
                                        isToday -> colors.accent
                                        else -> colors.textPrimary
                                    },
                                ),
                            )
                        }
                    } else {
                        Box(modifier = Modifier.size(36.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OSDatePickerTimeSection(
    selectedTime: String?,
    showTimePicker: Boolean,
    onToggleShowTime: () -> Unit,
    onTimeSelected: (String) -> Unit,
    onClearTime: () -> Unit,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val radius = OS11Theme.radius

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radius.md))
            .background(colors.surfaceSidebar)
            .padding(spacing.s3),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) { onToggleShowTime() }
                .padding(vertical = spacing.s1),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                Text(
                    text = "Time (15-min intervals)",
                    style = type.body.copy(color = colors.textPrimary),
                )
                selectedTime?.let {
                    Text(
                        text = it,
                        style = type.body.copy(color = colors.accent),
                    )
                }
            }

            val toggleLabel = when {
                showTimePicker -> "Hide"
                selectedTime != null -> "Change"
                else -> "Add time"
            }
            Text(
                text = toggleLabel,
                style = type.meta.copy(color = colors.accent),
            )
        }

        if (showTimePicker) {
            Spacer(modifier = Modifier.height(spacing.s2))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                verticalArrangement = Arrangement.spacedBy(spacing.s1),
            ) {
                listOf("09:00", "12:00", "17:00", "20:00").forEach { timePreset ->
                    OSChip(
                        label = timePreset,
                        onClick = { onTimeSelected(timePreset) },
                        selected = selectedTime == timePreset,
                    )
                }
                if (selectedTime != null) {
                    OSChip(
                        label = "Clear time",
                        onClick = onClearTime,
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.s2))

            OSTimeList(
                selectedTime = selectedTime,
                onTimeSelected = onTimeSelected,
            )
        }
    }
}

/**
 * Backward-compatible single-date overload.
 */
@Composable
fun OSDatePickerSheet(onDismissRequest: () -> Unit, onDateSelected: (String) -> Unit, modifier: Modifier = Modifier) =
    OSDatePickerSheet(
        onDismissRequest = onDismissRequest,
        onDateSelected = { date, _ -> onDateSelected(date ?: "") },
        modifier = modifier,
    )
