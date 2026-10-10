package com.os11.app.service

import com.os11.app.data.db.entities.ProjectEntity
import com.os11.app.data.repository.AreaRepository
import com.os11.app.data.repository.ProjectRepository
import com.os11.app.service.area.AreaService
import com.os11.app.service.project.ProjectService
import com.os11.app.service.undo.UndoManager
import com.os11.domain.id.DefaultIdGenerator
import com.os11.domain.result.AppError
import com.os11.domain.result.AppResult
import com.os11.domain.time.TestClock
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProjectAndAreaServiceTest {

    private val projectRepository: ProjectRepository = mockk(relaxed = true)
    private val areaRepository: AreaRepository = mockk(relaxed = true)
    private val undoManager = UndoManager()
    private val clock = TestClock(1_000_000L)
    private val idGenerator = DefaultIdGenerator()

    private lateinit var projectService: ProjectService
    private lateinit var areaService: AreaService

    @Before
    fun setup() {
        projectService = ProjectService(
            projectRepository = projectRepository,
            undoManager = undoManager,
            clock = clock,
            idGenerator = idGenerator,
            ioDispatcher = Dispatchers.Unconfined,
        )
        areaService = AreaService(
            areaRepository = areaRepository,
            undoManager = undoManager,
            clock = clock,
            idGenerator = idGenerator,
            ioDispatcher = Dispatchers.Unconfined,
        )
    }

    @Test
    fun `createProject rejects blank name and saves valid project`() = runBlocking {
        val blankResult = projectService.createProject("   ", "workspace-1")
        assertTrue(blankResult.isErr)
        assertTrue((blankResult as AppResult.Err).error is AppError.Validation)

        coEvery { projectRepository.save(any()) } answers { firstArg() }
        val okResult = projectService.createProject("Mobile App", "workspace-1")
        assertTrue(okResult.isOk)
        assertEquals("Mobile App", (okResult as AppResult.Ok).value.name)
    }

    @Test
    fun `createArea rejects blank name and saves valid area`() = runBlocking {
        val blankResult = areaService.createArea("   ", "workspace-1")
        assertTrue(blankResult.isErr)
        assertTrue((blankResult as AppResult.Err).error is AppError.Validation)

        coEvery { areaRepository.save(any()) } answers { firstArg() }
        val okResult = areaService.createArea("Work", "workspace-1")
        assertTrue(okResult.isOk)
        assertEquals("Work", (okResult as AppResult.Ok).value.name)
    }

    @Test
    fun `deleteProject registers undo token and restores project on undo`() = runBlocking {
        val project = ProjectEntity(
            id = "proj-1",
            name = "Sprint",
            workspaceId = "workspace-1",
            createdAt = clock.nowIso(),
            updatedAt = clock.nowIso(),
        )
        coEvery { projectRepository.getById("proj-1") } returns project

        val result = projectService.deleteProject("proj-1")
        assertTrue(result.isOk)
        val token = (result as AppResult.Ok).value

        coVerify { projectRepository.delete("proj-1") }

        undoManager.undo(token.id)
        coVerify { projectRepository.save(project) }
    }
}
