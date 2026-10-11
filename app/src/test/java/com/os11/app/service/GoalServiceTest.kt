package com.os11.app.service

import com.os11.app.data.db.entities.GoalEntity
import com.os11.app.data.repository.GoalRepository
import com.os11.app.data.repository.TaskRepository
import com.os11.app.service.goal.GoalService
import com.os11.domain.id.IdGenerator
import com.os11.domain.result.AppResult
import com.os11.domain.time.Clock
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GoalServiceTest {

    private val goalRepository: GoalRepository = mockk(relaxed = true)
    private val taskRepository: TaskRepository = mockk(relaxed = true)
    private val clock: Clock = mockk()
    private val idGenerator: IdGenerator = mockk()

    private lateinit var goalService: GoalService

    @Before
    fun setup() {
        coEvery { clock.today() } returns "2026-10-11"
        coEvery { clock.nowIso() } returns "2026-10-11T10:00:00.000Z"
        coEvery { idGenerator.randomId() } returns "goal-123"

        goalService = GoalService(
            goalRepository = goalRepository,
            taskRepository = taskRepository,
            clock = clock,
            idGenerator = idGenerator,
            ioDispatcher = Dispatchers.Unconfined,
        )
    }

    private fun sampleGoal(
        id: String = "goal-123",
        title: String = "Launch OS11 Android",
        streakCount: Int = 0,
        lastProgressAt: String? = null,
        currentValue: Double = 0.0,
        targetValue: Double = 100.0,
    ) = GoalEntity(
        id = id,
        title = title,
        goalType = "target",
        targetValue = targetValue,
        currentValue = currentValue,
        streakCount = streakCount,
        lastProgressAt = lastProgressAt,
        createdAt = "2026-10-11T08:00:00.000Z",
        updatedAt = "2026-10-11T08:00:00.000Z",
    )

    @Test
    fun `createGoal rejects blank title`() = runTest {
        val result = goalService.createGoal(title = "   ")
        assertTrue(result is AppResult.Err)
    }

    @Test
    fun `createGoal saves valid goal`() = runTest {
        val captured = slot<GoalEntity>()
        coEvery { goalRepository.save(capture(captured)) } answers { captured.captured }

        val result = goalService.createGoal(title = "Run Marathon", targetValue = 42.0)

        assertTrue(result is AppResult.Ok)
        assertEquals("goal-123", captured.captured.id)
        assertEquals("Run Marathon", captured.captured.title)
        assertEquals(42.0, captured.captured.targetValue, 0.001)
        assertEquals(0, captured.captured.streakCount)
    }

    @Test
    fun `recordProgress starts streak at 1 on first progress`() = runTest {
        val goal = sampleGoal(streakCount = 0, lastProgressAt = null)
        coEvery { goalRepository.getById("goal-123") } returns goal

        val captured = slot<GoalEntity>()
        coEvery { goalRepository.save(capture(captured)) } answers { captured.captured }

        val result = goalService.recordProgress("goal-123", incrementValue = 5.0)

        assertTrue(result is AppResult.Ok)
        assertEquals(1, captured.captured.streakCount)
        assertEquals(5.0, captured.captured.currentValue, 0.001)
        assertEquals("2026-10-11T10:00:00.000Z", captured.captured.lastProgressAt)
    }

    @Test
    fun `recordProgress on same day keeps streak count`() = runTest {
        val goal = sampleGoal(streakCount = 3, lastProgressAt = "2026-10-11T08:00:00.000Z")
        coEvery { goalRepository.getById("goal-123") } returns goal

        val captured = slot<GoalEntity>()
        coEvery { goalRepository.save(capture(captured)) } answers { captured.captured }

        goalService.recordProgress("goal-123", incrementValue = 2.0)

        assertEquals(3, captured.captured.streakCount)
    }

    @Test
    fun `recordProgress on consecutive day increments streak`() = runTest {
        val goal = sampleGoal(streakCount = 3, lastProgressAt = "2026-10-10T20:00:00.000Z")
        coEvery { goalRepository.getById("goal-123") } returns goal

        val captured = slot<GoalEntity>()
        coEvery { goalRepository.save(capture(captured)) } answers { captured.captured }

        goalService.recordProgress("goal-123", incrementValue = 1.0)

        assertEquals(4, captured.captured.streakCount)
    }

    @Test
    fun `recordProgress after missed day resets streak to 1`() = runTest {
        val goal = sampleGoal(streakCount = 5, lastProgressAt = "2026-10-08T12:00:00.000Z")
        coEvery { goalRepository.getById("goal-123") } returns goal

        val captured = slot<GoalEntity>()
        coEvery { goalRepository.save(capture(captured)) } answers { captured.captured }

        goalService.recordProgress("goal-123", incrementValue = 1.0)

        assertEquals(1, captured.captured.streakCount)
    }

    @Test
    fun `linkTask and unlinkTask call repository`() = runTest {
        goalService.linkTask("goal-123", "task-abc")
        coVerify { goalRepository.addLink("goal-123", "task", "task-abc") }

        goalService.unlinkTask("goal-123", "task-abc")
        coVerify { goalRepository.removeLink("goal-123", "task", "task-abc") }
    }

    @Test
    fun `linkProject and unlinkProject call repository`() = runTest {
        goalService.linkProject("goal-123", "proj-xyz")
        coVerify { goalRepository.addLink("goal-123", "project", "proj-xyz") }

        goalService.unlinkProject("goal-123", "proj-xyz")
        coVerify { goalRepository.removeLink("goal-123", "project", "proj-xyz") }
    }
}
