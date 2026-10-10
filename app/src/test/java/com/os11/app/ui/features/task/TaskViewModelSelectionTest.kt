package com.os11.app.ui.features.task

import com.os11.app.service.tag.TagService
import com.os11.app.service.task.TaskService
import com.os11.domain.time.TestClock
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModelSelectionTest {

    private val taskService: TaskService = mockk(relaxed = true)
    private val tagService: TagService = mockk(relaxed = true)
    private val clock = TestClock(1_000_000L)
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: TaskViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = TaskViewModel(taskService = taskService, tagService = tagService, clock = clock)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `toggleSelect activates selection mode and updates selected set`() = runTest {
        assertFalse(viewModel.isSelectionMode.value)
        assertEquals(emptySet<String>(), viewModel.selectedTaskIds.value)

        viewModel.toggleSelect("task-1")

        assertTrue(viewModel.isSelectionMode.value)
        assertEquals(setOf("task-1"), viewModel.selectedTaskIds.value)

        viewModel.toggleSelect("task-2")
        assertEquals(setOf("task-1", "task-2"), viewModel.selectedTaskIds.value)

        viewModel.toggleSelect("task-1")
        assertEquals(setOf("task-2"), viewModel.selectedTaskIds.value)
    }

    @Test
    fun `selectAll and toggleSelectAll work properly`() = runTest {
        val allIds = listOf("task-1", "task-2", "task-3")

        viewModel.selectAll(allIds)
        assertTrue(viewModel.isSelectionMode.value)
        assertEquals(allIds.toSet(), viewModel.selectedTaskIds.value)

        viewModel.toggleSelectAll(allIds)
        assertEquals(emptySet<String>(), viewModel.selectedTaskIds.value)
    }

    @Test
    fun `clearSelection resets selection mode and selected set`() = runTest {
        viewModel.toggleSelect("task-1")
        assertTrue(viewModel.isSelectionMode.value)

        viewModel.clearSelection()
        assertFalse(viewModel.isSelectionMode.value)
        assertTrue(viewModel.selectedTaskIds.value.isEmpty())
    }

    @Test
    fun `bulkComplete invokes taskService and clears selection`() = runTest {
        viewModel.toggleSelect("task-1")
        viewModel.toggleSelect("task-2")

        viewModel.bulkComplete(true)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { taskService.bulkComplete(listOf("task-1", "task-2"), true) }
        assertFalse(viewModel.isSelectionMode.value)
        assertTrue(viewModel.selectedTaskIds.value.isEmpty())
    }

    @Test
    fun `bulkTrash invokes taskService and clears selection`() = runTest {
        viewModel.toggleSelect("task-1")

        viewModel.bulkTrash()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { taskService.bulkTrash(listOf("task-1")) }
        assertFalse(viewModel.isSelectionMode.value)
    }

    @Test
    fun `reorderTask invokes taskService with new sortOrder`() = runTest {
        viewModel.reorderTask("task-1", 4.5)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { taskService.reorderTask("task-1", 4.5) }
    }
}
