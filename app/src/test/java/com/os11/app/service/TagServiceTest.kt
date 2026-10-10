package com.os11.app.service

import com.os11.app.data.db.entities.TagEntity
import com.os11.app.data.repository.TagRepository
import com.os11.app.data.repository.TaskRepository
import com.os11.app.service.tag.TagService
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

class TagServiceTest {

    private val tagRepository: TagRepository = mockk(relaxed = true)
    private val taskRepository: TaskRepository = mockk(relaxed = true)
    private val undoManager = UndoManager()
    private val clock = TestClock(1_000_000L)
    private val idGenerator = DefaultIdGenerator()

    private lateinit var tagService: TagService

    @Before
    fun setup() {
        tagService = TagService(
            tagRepository = tagRepository,
            taskRepository = taskRepository,
            undoManager = undoManager,
            clock = clock,
            idGenerator = idGenerator,
            ioDispatcher = Dispatchers.Unconfined,
        )
    }

    @Test
    fun `getOrCreateTag rejects blank tag name`() = runBlocking {
        val result = tagService.getOrCreateTag("   ")
        assertTrue(result.isErr)
        assertTrue((result as AppResult.Err).error is AppError.Validation)
    }

    @Test
    fun `getOrCreateTag returns existing tag if found`() = runBlocking {
        val existing = TagEntity(
            id = "tag-1",
            name = "android",
            color = "blue",
            createdAt = clock.nowIso(),
        )
        coEvery { tagRepository.getByName("android") } returns existing
        coEvery { tagRepository.getTaskCountForTag("tag-1") } returns 5

        val result = tagService.getOrCreateTag("android")
        assertTrue(result.isOk)
        val tagItem = (result as AppResult.Ok).value
        assertEquals("tag-1", tagItem.id)
        assertEquals("android", tagItem.name)
        assertEquals(5, tagItem.taskCount)
    }

    @Test
    fun `createTag with nested path resolves parent hierarchy`() = runBlocking {
        val parent = TagEntity(
            id = "tag-work",
            name = "work",
            color = "blue",
            createdAt = clock.nowIso(),
        )
        coEvery { tagRepository.getByName("work") } returns parent
        coEvery { tagRepository.getByName("work/client") } returns null
        coEvery { tagRepository.save(any()) } answers { firstArg() }

        val result = tagService.createTag("work/client")
        assertTrue(result.isOk)
        val created = (result as AppResult.Ok).value
        assertEquals("work/client", created.name)
        assertEquals("tag-work", created.parentTagId)
    }

    @Test
    fun `renameTag cascades prefix change to child tags`() = runBlocking {
        val parent = TagEntity(
            id = "tag-work",
            name = "work",
            color = "blue",
            createdAt = clock.nowIso(),
        )
        val child = TagEntity(
            id = "tag-client",
            name = "work/client",
            parentTagId = "tag-work",
            createdAt = clock.nowIso(),
        )

        coEvery { tagRepository.getById("tag-work") } returns parent
        coEvery { tagRepository.getByName("job") } returns null
        coEvery { tagRepository.getChildTags("tag-work") } returns listOf(child)
        coEvery { tagRepository.save(any()) } answers { firstArg() }

        val result = tagService.renameTag("tag-work", "job")
        assertTrue(result.isOk)
        val renamed = (result as AppResult.Ok).value
        assertEquals("job", renamed.name)

        coVerify { tagRepository.save(match { it.id == "tag-work" && it.name == "job" }) }
        coVerify { tagRepository.save(match { it.id == "tag-client" && it.name == "job/client" }) }
    }

    @Test
    fun `recolorTag updates color token`() = runBlocking {
        val tag = TagEntity(
            id = "tag-1",
            name = "urgent",
            color = "blue",
            createdAt = clock.nowIso(),
        )
        coEvery { tagRepository.getById("tag-1") } returns tag
        coEvery { tagRepository.save(any()) } answers { firstArg() }

        val result = tagService.recolorTag("tag-1", "red")
        assertTrue(result.isOk)
        assertEquals("red", (result as AppResult.Ok).value.color)
        coVerify { tagRepository.save(match { it.id == "tag-1" && it.color == "red" }) }
    }

    @Test
    fun `mergeTags repoints tasks, repoints children, deletes source, and supports undo`() = runBlocking {
        val source = TagEntity(id = "tag-src", name = "src", createdAt = clock.nowIso())
        val target = TagEntity(id = "tag-tgt", name = "tgt", createdAt = clock.nowIso())
        val child = TagEntity(
            id = "tag-child",
            name = "src/nested",
            parentTagId = "tag-src",
            createdAt = clock.nowIso(),
        )

        coEvery { tagRepository.getById("tag-src") } returns source
        coEvery { tagRepository.getById("tag-tgt") } returns target
        coEvery { tagRepository.getTaskIdsForTag("tag-src") } returns listOf("task-1", "task-both")
        coEvery { tagRepository.getTaskIdsForTag("tag-tgt") } returns listOf("task-both", "task-2")
        coEvery { tagRepository.getChildTags("tag-src") } returns listOf(child)

        val mergeResult = tagService.mergeTags("tag-src", "tag-tgt")
        assertTrue(mergeResult.isOk)
        val token = (mergeResult as AppResult.Ok).value

        // For task-1 (only in source): added to target, removed from source
        coVerify { tagRepository.addTagToTask("task-1", "tag-tgt") }
        coVerify { tagRepository.removeTagFromTask("task-1", "tag-src") }

        // For task-both (in both): removed from source, target already had it
        coVerify { tagRepository.removeTagFromTask("task-both", "tag-src") }

        // Child tag repointed to target
        coVerify { tagRepository.save(match { it.id == "tag-child" && it.parentTagId == "tag-tgt" }) }

        // Source tag deleted - leaving zero orphan rows
        coVerify { tagRepository.delete("tag-src") }

        // Now perform UNDO:
        val undoResult = undoManager.undo(token.id)
        assertTrue(undoResult.isOk)

        // Source tag restored
        coVerify { tagRepository.save(source) }

        // Task associations restored
        coVerify { tagRepository.removeTagFromTask("task-1", "tag-tgt") }
        coVerify { tagRepository.addTagToTask("task-1", "tag-src") }
        coVerify { tagRepository.addTagToTask("task-both", "tag-src") }

        // Child tag parent restored to source
        coVerify { tagRepository.save(match { it.id == "tag-child" && it.parentTagId == "tag-src" }) }
    }
}
