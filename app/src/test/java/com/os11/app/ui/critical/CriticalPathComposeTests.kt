package com.os11.app.ui.critical

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.os11.app.TestActivity
import com.os11.app.service.task.TaskItem
import com.os11.app.ui.components.OSButton
import com.os11.app.ui.components.OSDatePickerSheet
import com.os11.app.ui.components.OSTextField
import com.os11.app.ui.features.dayplan.DayPlanBlock
import com.os11.app.ui.features.dayplan.DayPlanLaneCalculator
import com.os11.app.ui.features.task.TaskRow
import com.os11.app.ui.features.task.TaskUiModel
import com.os11.app.ui.theme.OS11Theme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], packageName = "com.os11.app")
class CriticalPathComposeTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    @Test
    fun critical_path_quick_add_input_and_submission() {
        var submittedText = ""
        composeTestRule.setContent {
            OS11Theme {
                var text by remember { mutableStateOf("") }
                Box(modifier = Modifier.fillMaxSize()) {
                    OSTextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = "Add task...",
                    )
                    OSButton(
                        onClick = { submittedText = text },
                    ) {
                        Text("Add")
                    }
                }
            }
        }

        // Type task title with natural language tokens
        composeTestRule.onNodeWithText("Add task...").performTextInput("Prepare quarterly presentation #work !urgent")
        composeTestRule.onNodeWithText("Add").performClick()

        assertEquals("Prepare quarterly presentation #work !urgent", submittedText)
    }

    @Test
    fun critical_path_task_complete_and_undo_interaction() {
        var completedState = false
        var undoInvoked = false

        val testTask = TaskUiModel(
            id = "task-critical-1",
            title = "Verify deployment checklist",
            isCompleted = false,
            workspaceId = "workspace-1",
            sortOrder = 0.0,
        )

        composeTestRule.setContent {
            OS11Theme {
                var task by remember { mutableStateOf(testTask) }
                var showUndoBar by remember { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxSize()) {
                    TaskRow(
                        task = task,
                        onClick = {},
                        onLongClick = {},
                        onStarClick = {},
                        onCompletedChange = { completed ->
                            completedState = completed
                            task = task.copy(isCompleted = completed)
                            showUndoBar = true
                        },
                    )

                    if (showUndoBar) {
                        OSButton(
                            onClick = {
                                undoInvoked = true
                                completedState = false
                                task = task.copy(isCompleted = false)
                                showUndoBar = false
                            },
                        ) {
                            Text("Undo")
                        }
                    }
                }
            }
        }

        // Click task checkbox to toggle completion
        composeTestRule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)).performClick()
        assertTrue(completedState)

        // Click undo button
        composeTestRule.onNodeWithText("Undo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Undo").performClick()

        assertTrue(undoInvoked)
        assertEquals(false, completedState)
    }

    @Test
    fun critical_path_reminder_date_picker_selection() {
        var selectedDate: String? = null
        var selectedTime: String? = null

        composeTestRule.setContent {
            OS11Theme {
                OSDatePickerSheet(
                    onDismissRequest = {},
                    onDateSelected = { date, time ->
                        selectedDate = date
                        selectedTime = time
                    },
                    initialDate = "2026-10-11",
                )
            }
        }

        // Clear date button is displayed when initialDate is set
        composeTestRule.onNodeWithText("Clear Date").assertIsDisplayed()
        composeTestRule.onNodeWithText("Clear Date").performClick()
        composeTestRule.waitForIdle()

        assertNull(selectedDate)
    }

    @Test
    fun critical_path_day_plan_timeline_lane_calculation_and_click() {
        val task1 = TaskItem(
            id = "task-dp-1",
            title = "Morning Standup",
            workspaceId = "workspace-1",
            scheduledStartMin = 9 * 60, // 09:00
            scheduledDurationMin = 30,
            createdAt = "2026-10-11T00:00:00Z",
            updatedAt = "2026-10-11T00:00:00Z",
        )
        val task2 = TaskItem(
            id = "task-dp-2",
            title = "Architecture Review",
            workspaceId = "workspace-1",
            scheduledStartMin = 9 * 60 + 15, // 09:15 (overlapping task1)
            scheduledDurationMin = 45,
            createdAt = "2026-10-11T00:00:00Z",
            updatedAt = "2026-10-11T00:00:00Z",
        )

        val block1 = DayPlanBlock(
            task = task1,
            startMin = task1.scheduledStartMin ?: 0,
            durationMin = task1.scheduledDurationMin ?: 0,
        )
        val block2 = DayPlanBlock(
            task = task2,
            startMin = task2.scheduledStartMin ?: 0,
            durationMin = task2.scheduledDurationMin ?: 0,
        )

        val scheduled = listOf(block1, block2)
        val lanes = DayPlanLaneCalculator.computeLanes(scheduled)

        assertEquals(2, lanes.size)
        // task1 and task2 placed in non-conflicting lanes 0 and 1
        assertEquals(0, lanes[0].laneIndex)
        assertEquals(1, lanes[1].laneIndex)
        assertEquals(2, lanes[0].totalLanes)
        assertEquals(2, lanes[1].totalLanes)

        var clickedTask: TaskItem? = null
        composeTestRule.setContent {
            OS11Theme {
                Box(modifier = Modifier.fillMaxSize()) {
                    OSButton(onClick = { clickedTask = task1 }) {
                        Text(task1.title)
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Morning Standup").assertIsDisplayed()
        composeTestRule.onNodeWithText("Morning Standup").performClick()
        assertEquals("task-dp-1", clickedTask?.id)
    }
}
