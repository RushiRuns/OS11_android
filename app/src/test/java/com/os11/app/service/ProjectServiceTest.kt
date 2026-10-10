package com.os11.app.service

import com.os11.app.data.db.dao.SlimTask
import com.os11.app.data.db.entities.MilestoneEntity
import com.os11.app.data.db.entities.ProjectEntity
import com.os11.app.data.db.entities.SectionEntity
import com.os11.app.data.db.entities.TaskEntity
import com.os11.app.data.repository.DependencyRepository
import com.os11.app.data.repository.MilestoneRepository
import com.os11.app.data.repository.ProjectRepository
import com.os11.app.data.repository.SectionRepository
import com.os11.app.data.repository.TaskRepository
import com.os11.app.service.project.ProjectService
import com.os11.app.service.undo.UndoManager
import com.os11.domain.id.DefaultIdGenerator
import com.os11.domain.result.AppError
import com.os11.domain.result.AppResult
import com.os11.domain.time.TestClock
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProjectServiceTest {

    private val projectRepository: ProjectRepository = mockk(relaxed = true)
    private val sectionRepository: SectionRepository = mockk(relaxed = true)
    private val milestoneRepository: MilestoneRepository = mockk(relaxed = true)
    private val dependencyRepository: DependencyRepository = mockk(relaxed = true)
    private val taskRepository: TaskRepository = mockk(relaxed = true)
    private val undoManager = UndoManager()
    private val clock = TestClock(1_000_000L)
    private val idGenerator = DefaultIdGenerator()

    private lateinit var projectService: ProjectService

    @Before
    fun setup() {
        projectService = ProjectService(
            projectRepository = projectRepository,
            sectionRepository = sectionRepository,
            milestoneRepository = milestoneRepository,
            dependencyRepository = dependencyRepository,
            taskRepository = taskRepository,
            undoManager = undoManager,
            clock = clock,
            idGenerator = idGenerator,
            ioDispatcher = Dispatchers.Unconfined,
        )
    }

    @Test
    fun `createProject rejects blank name`() = runBlocking {
        val result = projectService.createProject("   ", "ws-1")
        assertTrue(result.isErr)
        assertTrue((result as AppResult.Err).error is AppError.Validation)
    }

    @Test
    fun `createProject successfully creates and saves project`() = runBlocking {
        val projectSlot = slot<ProjectEntity>()
        coEvery { projectRepository.save(capture(projectSlot)) } answers { projectSlot.captured }

        val result = projectService.createProject("Mobile Redesign", "ws-1")
        assertTrue(result.isOk)
        val created = (result as AppResult.Ok).value
        assertEquals("Mobile Redesign", created.name)
        assertEquals("ws-1", created.workspaceId)
    }

    @Test
    fun `updateDefaultView preserves and stores table view without overwriting`() = runBlocking {
        val existing = ProjectEntity(
            id = "proj-1",
            workspaceId = "ws-1",
            name = "Project",
            defaultView = "list",
            createdAt = clock.nowIso(),
            updatedAt = clock.nowIso(),
        )
        coEvery { projectRepository.getById("proj-1") } returns existing
        val savedSlot = slot<ProjectEntity>()
        coEvery { projectRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        val result = projectService.updateDefaultView("proj-1", "table")
        assertTrue(result.isOk)
        assertEquals("table", savedSlot.captured.defaultView)
    }

    @Test
    fun `archiveProject sets status to archived and undo restores previous status`() = runBlocking {
        val existing = ProjectEntity(
            id = "proj-1",
            workspaceId = "ws-1",
            name = "Project",
            status = "active",
            createdAt = clock.nowIso(),
            updatedAt = clock.nowIso(),
        )
        coEvery { projectRepository.getById("proj-1") } returns existing
        val savedSlot = slot<ProjectEntity>()
        coEvery { projectRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        val result = projectService.archiveProject("proj-1")
        assertTrue(result.isOk)
        assertEquals("archived", savedSlot.captured.status)

        // Perform undo
        val token = (result as AppResult.Ok).value
        val undoResult = undoManager.undo(token.id)
        assertTrue(undoResult.isOk)
        assertEquals("active", savedSlot.captured.status)
    }

    @Test
    fun `createSection rejects blank name`() = runBlocking {
        val result = projectService.createSection("proj-1", "  ")
        assertTrue(result.isErr)
        assertTrue((result as AppResult.Err).error is AppError.Validation)
    }

    @Test
    fun `createSection successfully creates and saves section`() = runBlocking {
        coEvery { sectionRepository.getByProject("proj-1") } returns emptyList()
        val slot = slot<SectionEntity>()
        coEvery { sectionRepository.save(capture(slot)) } answers { slot.captured }

        val result = projectService.createSection("proj-1", "Design Phase")
        assertTrue(result.isOk)
        val section = (result as AppResult.Ok).value
        assertEquals("Design Phase", section.name)
        assertEquals("proj-1", section.projectId)
        assertEquals(10.0, section.sortOrder, 0.001)
    }

    @Test
    fun `deleteSection unlinks tasks and allows undo`() = runBlocking {
        val section = SectionEntity(
            id = "sec-1",
            projectId = "proj-1",
            name = "Sprint 1",
            createdAt = clock.nowIso(),
            updatedAt = clock.nowIso(),
        )
        val task = TaskEntity(
            id = "task-1",
            workspaceId = "ws-1",
            projectId = "proj-1",
            sectionId = "sec-1",
            title = "Task",
            createdAt = clock.nowIso(),
            updatedAt = clock.nowIso(),
        )
        coEvery { sectionRepository.getById("sec-1") } returns section
        coEvery { taskRepository.getTasksBySection("sec-1") } returns listOf(task)
        val taskSlot = slot<TaskEntity>()
        coEvery { taskRepository.saveTask(capture(taskSlot)) } answers { taskSlot.captured }

        val result = projectService.deleteSection("sec-1")
        assertTrue(result.isOk)

        // Verifies task was unlinked
        coVerify { sectionRepository.delete("sec-1") }
        assertNull(taskSlot.captured.sectionId)

        // Perform undo
        val token = (result as AppResult.Ok).value
        val undoResult = undoManager.undo(token.id)
        assertTrue(undoResult.isOk)
        coVerify { sectionRepository.save(section) }
        assertEquals("sec-1", taskSlot.captured.sectionId)
    }

    @Test
    fun `addDependency rejects task depending on itself`() = runBlocking {
        val result = projectService.addDependency("task-1", "task-1")
        assertTrue(result.isErr)
        assertTrue((result as AppResult.Err).error is AppError.Validation)
    }

    @Test
    fun `addDependency detects and rejects cycle`() = runBlocking {
        // Existing: task-2 depends on task-1
        coEvery { dependencyRepository.getAllDependencyPairs() } returns listOf("task-2" to "task-1")

        // Proposed: task-1 depends on task-2 -> would create cycle 1 -> 2 -> 1
        val result = projectService.addDependency("task-1", "task-2")
        assertTrue(result.isErr)
        val error = (result as AppResult.Err).error as AppError.Validation
        assertEquals("dependency", error.field)
        assertTrue(error.reason.contains("circular dependency loop"))
    }

    @Test
    fun `addDependency succeeds when graph is acyclic`() = runBlocking {
        coEvery { dependencyRepository.getAllDependencyPairs() } returns listOf("task-2" to "task-1")

        // Proposed: task-3 depends on task-2 (acyclic: 3 -> 2 -> 1)
        val result = projectService.addDependency("task-3", "task-2")
        assertTrue(result.isOk)
        coVerify { dependencyRepository.addDependency("task-3", "task-2") }
    }

    @Test
    fun `createMilestone validates title and saves milestone`() = runBlocking {
        val blankResult = projectService.createMilestone("proj-1", " ", "2026-11-01")
        assertTrue(blankResult.isErr)

        val slot = slot<MilestoneEntity>()
        coEvery { milestoneRepository.save(capture(slot)) } answers { slot.captured }

        val result = projectService.createMilestone("proj-1", "Alpha Launch", "2026-11-01")
        assertTrue(result.isOk)
        val milestone = (result as AppResult.Ok).value
        assertEquals("Alpha Launch", milestone.title)
        assertEquals("2026-11-01", milestone.dueDate)
    }

    @Test
    fun `exportProjectToCsv exports correctly formatted CSV`() = runBlocking {
        val project = ProjectEntity(
            id = "proj-1",
            workspaceId = "ws-1",
            name = "Project X",
            status = "active",
            createdAt = clock.nowIso(),
            updatedAt = clock.nowIso(),
        )
        val task = SlimTask(
            id = "t-1",
            title = "Task, with comma \"and quotes\"",
            workspaceId = "ws-1",
            areaId = null,
            projectId = "proj-1",
            sectionId = "sec-1",
            parentTaskId = null,
            dueDate = "2026-10-15",
            dueTime = null,
            allDay = true,
            recurrenceRule = null,
            recurrenceBasis = null,
            priority = 2,
            isStarred = false,
            isCompleted = false,
            completedAt = null,
            estimatedMinutes = null,
            sortOrder = 1.0,
            myDayDate = null,
            pomodoroCount = 0,
            scheduledStartMin = null,
            scheduledDurationMin = null,
            bucket = null,
            waitingOn = null,
            waitingSince = null,
            followUpDate = null,
            isTrashed = false,
            createdAt = "2026-10-10T12:00:00Z",
            updatedAt = "2026-10-10T12:00:00Z",
        )
        val section = SectionEntity(
            id = "sec-1",
            projectId = "proj-1",
            name = "Backlog",
            createdAt = clock.nowIso(),
            updatedAt = clock.nowIso(),
        )

        coEvery { projectRepository.getById("proj-1") } returns project
        coEvery { taskRepository.observeByProjectSlim("proj-1") } returns flowOf(listOf(task))
        coEvery { sectionRepository.getByProject("proj-1") } returns listOf(section)

        val result = projectService.exportProjectToCsv("proj-1")
        assertTrue(result.isOk)
        val csv = (result as AppResult.Ok).value
        assertTrue(csv.contains("Project: Project X"))
        assertTrue(csv.contains("ID,Title,Section,Priority,Due Date,Due Time,Completed,Completed At,Created At"))
        assertTrue(csv.contains("\"Task, with comma \"\"and quotes\"\"\""))
        assertTrue(csv.contains("\"Backlog\""))
    }
}
