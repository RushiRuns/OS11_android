package com.os11.app.ui.features.onboarding

import androidx.lifecycle.SavedStateHandle
import com.os11.app.data.db.entities.LocalIdentityEntity
import com.os11.app.data.db.entities.TaskEntity
import com.os11.app.service.settings.ProfilePreset
import com.os11.app.service.settings.SettingsService
import com.os11.app.service.tag.TagItem
import com.os11.app.service.tag.TagService
import com.os11.app.service.task.TaskService
import com.os11.domain.result.AppResult
import com.os11.domain.time.TestClock
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val settingsService: SettingsService = mockk(relaxed = true)
    private val taskService: TaskService = mockk(relaxed = true)
    private val tagService: TagService = mockk(relaxed = true)
    private val testClock = TestClock(1_000_000L)
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: OnboardingViewModel

    private var activePreset = "standard"
    private var isCompleted = false
    private var localIdentity = LocalIdentityEntity(
        id = "identity-1",
        displayName = "My Android",
        avatarEmoji = "📱",
        createdAt = "2026-10-11T00:00:00Z",
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        activePreset = "standard"
        isCompleted = false
        localIdentity = LocalIdentityEntity(
            id = "identity-1",
            displayName = "My Android",
            avatarEmoji = "📱",
            createdAt = "2026-10-11T00:00:00Z",
        )

        coEvery { settingsService.getLocalIdentity() } answers { localIdentity }
        coEvery { settingsService.updateLocalIdentity(any(), any()) } answers {
            localIdentity = localIdentity.copy(
                displayName = firstArg(),
                avatarEmoji = secondArg(),
            )
            AppResult.ok(Unit)
        }
        coEvery { settingsService.applyProfilePreset(any()) } answers {
            activePreset = firstArg()
            AppResult.ok(Unit)
        }
        every { settingsService.observeActiveProfilePreset() } answers {
            flowOf(activePreset)
        }
        coEvery { settingsService.setOnboardingCompleted(any()) } answers {
            isCompleted = firstArg()
            AppResult.ok(Unit)
        }
        coEvery { settingsService.isOnboardingCompleted() } answers { isCompleted }

        val dummyTask = TaskEntity(
            id = "sample-task-1",
            workspaceId = "workspace-1",
            title = "Sample",
            sortOrder = 0.0,
            createdAt = "2026-10-11T00:00:00Z",
            updatedAt = "2026-10-11T00:00:00Z",
        )
        coEvery {
            taskService.createTask(
                title = any(),
                workspaceId = any(),
                dueDate = any(),
                dueTime = any(),
                priority = any(),
            )
        } returns AppResult.ok(dummyTask)

        val dummyTag = TagItem(
            id = "tag-1",
            name = "focus",
            color = "#5E6AD2",
        )
        coEvery { tagService.getOrCreateTag(any()) } returns AppResult.ok(dummyTag)
        coEvery { tagService.addTagToTask(any(), any()) } returns AppResult.ok(Unit)

        savedStateHandle = SavedStateHandle()
        viewModel = OnboardingViewModel(
            settingsService = settingsService,
            taskService = taskService,
            tagService = tagService,
            clock = testClock,
            savedStateHandle = savedStateHandle,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initial_state_has_step_zero_and_parsed_sample_task() {
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(0, state.currentStep)
        assertEquals(ProfilePreset.Standard, state.selectedPreset)
        assertFalse(state.isCompleted)
        assertNotNull(state.parsedResult)
        assertTrue(state.parsedResult.tags.contains("focus"))
    }

    @Test
    fun select_preset_applies_preset_to_settings_service() = runBlocking {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.selectPreset(ProfilePreset.Minimal)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(ProfilePreset.Minimal, viewModel.uiState.value.selectedPreset)
        assertEquals("minimal", activePreset)
        coVerify { settingsService.applyProfilePreset("minimal") }
    }

    @Test
    fun step_navigation_persists_identity_and_retains_step() = runBlocking {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.updateDisplayName("Alex's Machine")
        viewModel.selectEmoji("⚡")

        // Step 0 -> Step 1
        viewModel.nextStep()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.currentStep)

        // Step 1 -> Step 2
        viewModel.nextStep()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.currentStep)

        assertEquals("Alex's Machine", localIdentity.displayName)
        assertEquals("⚡", localIdentity.avatarEmoji)

        // Step 2 -> Step 1 -> Step 0
        viewModel.previousStep()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.currentStep)
        viewModel.previousStep()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.currentStep)
    }

    @Test
    fun update_quick_add_text_updates_parsed_nlp_tokens() {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.updateQuickAddText("Ship release build tomorrow 4pm #ship !critical")
        val parsed = viewModel.uiState.value.parsedResult
        assertTrue(parsed.tags.contains("ship"))
        assertEquals(4, parsed.priority)
    }

    @Test
    fun complete_onboarding_creates_task_and_marks_completed() = runBlocking {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.updateDisplayName("Jane")
        viewModel.selectEmoji("🚀")
        viewModel.updateQuickAddText("Finish sprint demo #prod !high")

        var finishedCallbackCalled = false
        viewModel.completeOnboarding(createStarterTask = true) {
            finishedCallbackCalled = true
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(finishedCallbackCalled)
        assertTrue(viewModel.uiState.value.isCompleted)
        assertTrue(isCompleted)
        coVerify { settingsService.setOnboardingCompleted(true) }
        coVerify {
            taskService.createTask(
                title = "Finish sprint demo",
                workspaceId = any(),
                dueDate = any(),
                dueTime = any(),
                priority = 3,
            )
        }
    }

    @Test
    fun complete_onboarding_skip_task_does_not_create_task() = runBlocking {
        testDispatcher.scheduler.advanceUntilIdle()
        var finishedCallbackCalled = false
        viewModel.completeOnboarding(createStarterTask = false) {
            finishedCallbackCalled = true
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(finishedCallbackCalled)
        assertTrue(isCompleted)
        coVerify(exactly = 0) {
            taskService.createTask(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            )
        }
    }
}
