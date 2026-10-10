package com.os11.app.ui.features.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.os11.app.ui.components.OSButton
import com.os11.app.ui.components.OSButtonVariant
import com.os11.app.ui.components.OSCheckbox
import com.os11.app.ui.components.OSChip
import com.os11.app.ui.components.OSDatePickerSheet
import com.os11.app.ui.components.OSEmptyState
import com.os11.app.ui.components.OSGlassMenu
import com.os11.app.ui.components.OSIconButton
import com.os11.app.ui.components.OSMenuItem
import com.os11.app.ui.components.OSPriority
import com.os11.app.ui.components.OSPriorityBar
import com.os11.app.ui.components.OSProgressBar
import com.os11.app.ui.components.OSProgressRing
import com.os11.app.ui.components.OSSearchField
import com.os11.app.ui.components.OSSegmented
import com.os11.app.ui.components.OSSheet
import com.os11.app.ui.components.OSSnackbarHost
import com.os11.app.ui.components.OSSwitch
import com.os11.app.ui.components.OSTagDot
import com.os11.app.ui.components.OSTagDotVariant
import com.os11.app.ui.components.OSTextField
import com.os11.app.ui.components.OSTimeList
import com.os11.app.ui.theme.OS11Density
import com.os11.app.ui.theme.OS11Theme
import kotlinx.coroutines.launch

/**
 * Debug-only Component Gallery screen.
 *
 * Renders every base component across every state for visual and accessibility audit.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ComponentGalleryScreen(modifier: Modifier = Modifier) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Interactive states
    var checkboxChecked by remember { mutableStateOf(false) }
    var switchChecked by remember { mutableStateOf(false) }
    var textValue by remember { mutableStateOf("") }
    var searchValue by remember { mutableStateOf("") }
    var selectedDensity by remember { mutableStateOf(OS11Density.Comfortable) }
    var selectedTime by remember { mutableStateOf("09:00") }

    // Dialog / Sheet states
    var showSheet by remember { mutableStateOf(false) }
    var showGlassMenu by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = spacing.s4, vertical = spacing.s6),
            verticalArrangement = Arrangement.spacedBy(spacing.s6),
        ) {
            // Header
            Column(verticalArrangement = Arrangement.spacedBy(spacing.s1)) {
                Text(
                    text = "Component Gallery",
                    style = type.viewTitle.copy(color = colors.textPrimary),
                )
                Text(
                    text = "Visual review for all design system primitives",
                    style = type.meta.copy(color = colors.textSecondary),
                )
            }

            // 1. Buttons
            GallerySection(title = "Buttons & Icon Buttons") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                ) {
                    OSButton(onClick = {}, variant = OSButtonVariant.Primary) {
                        Text("Primary")
                    }
                    OSButton(onClick = {}, variant = OSButtonVariant.Secondary) {
                        Text("Secondary")
                    }
                    OSButton(onClick = {}, variant = OSButtonVariant.Danger) {
                        Text("Danger")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                ) {
                    OSButton(onClick = {}, enabled = false) {
                        Text("Disabled")
                    }
                    OSIconButton(onClick = {}, contentDescription = "Add") {
                        Icon(Icons.Default.Add, contentDescription = null, tint = colors.accent)
                    }
                }
            }

            // 2. Checkboxes
            GallerySection(title = "Checkboxes (Spring Animation & 48dp Hit Area)") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.s4),
                ) {
                    OSCheckbox(
                        checked = checkboxChecked,
                        onCheckedChange = { checkboxChecked = it },
                    )
                    Text(
                        text = if (checkboxChecked) "Completed (Spring fired)" else "Pending",
                        style = type.body.copy(color = colors.textPrimary),
                    )
                }
            }

            // 3. Inputs
            GallerySection(title = "Text Fields & Search Fields") {
                OSTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    placeholder = "Type task title...",
                    modifier = Modifier.fillMaxWidth(),
                )
                OSSearchField(
                    query = searchValue,
                    onQueryChange = { searchValue = it },
                    onSearch = {},
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // 4. Priorities & Tags
            GallerySection(title = "Priority Left Borders & Tag Dots") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.s4),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.height(32.dp)) {
                        OSPriorityBar(priority = OSPriority.Low)
                    }
                    Box(modifier = Modifier.height(32.dp)) {
                        OSPriorityBar(priority = OSPriority.Medium)
                    }
                    Box(modifier = Modifier.height(32.dp)) {
                        OSPriorityBar(priority = OSPriority.High)
                    }
                    Box(modifier = Modifier.height(32.dp)) {
                        OSPriorityBar(priority = OSPriority.Critical)
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    OSTagDot(color = colors.tags.blue, variant = OSTagDotVariant.TaskRow)
                    OSTagDot(color = colors.tags.green, variant = OSTagDotVariant.TaskRow)
                    OSTagDot(color = colors.tags.yellow, variant = OSTagDotVariant.TaskRow)
                    OSTagDot(color = colors.tags.red, variant = OSTagDotVariant.TaskRow)
                }
            }

            // 5. Chips
            GallerySection(title = "Chips & Filters") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                ) {
                    OSChip(label = "Normal", onClick = {})
                    OSChip(label = "Selected", selected = true, onClick = {})
                    OSChip(label = "Overdue", tintColor = colors.danger, onClick = {})
                    OSChip(label = "Star", tintColor = colors.star, onClick = {})
                }
            }

            // 6. Switches & Segmented
            GallerySection(title = "Switches & Segmented") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.s4),
                ) {
                    OSSwitch(
                        checked = switchChecked,
                        onCheckedChange = { switchChecked = it },
                    )
                    Text(
                        text = if (switchChecked) "Switch ON" else "Switch OFF",
                        style = type.body.copy(color = colors.textPrimary),
                    )
                }

                OSSegmented(
                    options = listOf(OS11Density.Compact, OS11Density.Comfortable, OS11Density.Cozy),
                    selectedOption = selectedDensity,
                    onOptionSelected = { selectedDensity = it },
                    labelProvider = { it.name },
                )
            }

            // 7. Progress
            GallerySection(title = "Progress Ring & Bar (Direct Canvas)") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.s6),
                ) {
                    OSProgressRing(progress = 0.25f)
                    OSProgressRing(progress = 0.70f)
                    OSProgressRing(progress = 1.0f)
                }
                OSProgressBar(progress = 0.65f)
            }

            // 8. Triggers for Sheets, Menus & Snackbar
            GallerySection(title = "Sheets, Glass Menus & Undo Snackbar") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                ) {
                    OSButton(
                        onClick = { showSheet = true },
                        variant = OSButtonVariant.Secondary,
                    ) {
                        Text("Show Sheet")
                    }
                    OSButton(
                        onClick = { showDatePicker = true },
                        variant = OSButtonVariant.Secondary,
                    ) {
                        Text("Set When")
                    }
                    OSButton(
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Task completed",
                                    actionLabel = "Undo",
                                )
                            }
                        },
                        variant = OSButtonVariant.Primary,
                    ) {
                        Text("Trigger Undo")
                    }
                }

                if (showGlassMenu) {
                    OSGlassMenu(
                        items = listOf(
                            OSMenuItem(title = "Dashboard", onClick = { showGlassMenu = false }),
                            OSMenuItem(title = "Settings", onClick = { showGlassMenu = false }),
                        ),
                    )
                } else {
                    OSButton(
                        onClick = { showGlassMenu = true },
                        variant = OSButtonVariant.Ghost,
                    ) {
                        Text("Toggle Glass Menu")
                    }
                }
            }

            // 9. Time List
            GallerySection(title = "15-Minute Time Interval Selector") {
                OSTimeList(
                    selectedTime = selectedTime,
                    onTimeSelected = { selectedTime = it },
                )
            }

            // 10. Empty State
            GallerySection(title = "Empty State") {
                OSEmptyState(
                    title = "Nothing to show",
                    description = "This is a preview of the shared empty state component.",
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = colors.accent,
                        )
                    },
                )
            }

            // 11. Task Row at Densities
            GallerySection(title = "Task Row (Density variants: 44dp, 52dp, 60dp)") {
                com.os11.app.ui.features.task.TaskRow(
                    task = com.os11.app.ui.features.task.TaskUiModel(
                        id = "gallery-1",
                        title = "Sample task with due date & project",
                        dueDate = "Tomorrow",
                        formattedDueDate = "Tomorrow",
                        projectName = "Launch OS11",
                        priority = 3,
                        subtaskProgress = "2/5",
                        hasNotes = true,
                    ),
                    onCompletedChange = {},
                    onClick = {},
                    onLongClick = {},
                    onStarClick = {},
                )
            }

            // 12. Note Editor
            GallerySection(title = "Note Editor") {
                com.os11.app.ui.components.note.NoteEditor(
                    initialText = "<h2>Sprint Notes</h2><p>Sanitized with Jsoup on save " +
                        "per <strong>ADR-A11</strong>.</p>",
                    onNotesChanged = {},
                )
            }
        }

        // Active Sheet Modals
        if (showSheet) {
            OSSheet(onDismissRequest = { showSheet = false }) {
                Column(modifier = Modifier.padding(spacing.s4)) {
                    Text(
                        text = "OSSheet Preview",
                        style = type.panelTitle.copy(color = colors.textPrimary),
                    )
                    Spacer(modifier = Modifier.height(spacing.s2))
                    Text(
                        text = "Modal sheet with 12dp top corners, custom drag handle, and token colors.",
                        style = type.body.copy(color = colors.textSecondary),
                    )
                }
            }
        }

        if (showDatePicker) {
            OSDatePickerSheet(
                onDismissRequest = { showDatePicker = false },
                onDateSelected = {},
            )
        }

        // Snackbar host on top
        OSSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun GallerySection(title: String, content: @Composable () -> Unit) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.s3),
    ) {
        Text(
            text = title.uppercase(),
            style = type.sectionLabel.copy(color = colors.textTertiary),
        )
        content()
    }
}
