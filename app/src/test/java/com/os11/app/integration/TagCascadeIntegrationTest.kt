package com.os11.app.integration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.os11.app.data.db.DatabaseInitializer
import com.os11.app.data.db.OS11Database
import com.os11.app.data.repository.CountsRepositoryImpl
import com.os11.app.data.repository.TagRepositoryImpl
import com.os11.app.data.repository.TaskRepositoryImpl
import com.os11.app.data.sync.TrackedWriter
import com.os11.app.service.tag.TagService
import com.os11.app.service.task.TaskService
import com.os11.app.service.undo.UndoManager
import com.os11.domain.id.DefaultIdGenerator
import com.os11.domain.id.DeterministicIds
import com.os11.domain.logic.sync.DefaultHlcClock
import com.os11.domain.time.TestClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TagCascadeIntegrationTest {

    private lateinit var db: OS11Database
    private lateinit var testClock: TestClock
    private lateinit var trackedWriter: TrackedWriter
    private lateinit var taskRepository: TaskRepositoryImpl
    private lateinit var tagRepository: TagRepositoryImpl
    private lateinit var tagService: TagService
    private lateinit var taskService: TaskService
    private lateinit var undoManager: UndoManager

    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = OS11Database.buildInMemory(context)
        testClock = TestClock(1_000_000L)
        val hlcClock = DefaultHlcClock(testClock, "tag-cascade-test")
        val idGenerator = DefaultIdGenerator()
        trackedWriter = TrackedWriter(db, hlcClock, testClock, idGenerator)
        DatabaseInitializer.seedDefaults(db, testClock)

        taskRepository = TaskRepositoryImpl(db, trackedWriter, testClock)
        tagRepository = TagRepositoryImpl(db, trackedWriter)
        undoManager = UndoManager()

        taskService = TaskService(
            taskRepository = taskRepository,
            countsRepository = CountsRepositoryImpl(db),
            undoManager = undoManager,
            clock = testClock,
            idGenerator = idGenerator,
            ioDispatcher = Dispatchers.Unconfined,
        )

        tagService = TagService(
            tagRepository = tagRepository,
            taskRepository = taskRepository,
            undoManager = undoManager,
            clock = testClock,
            idGenerator = idGenerator,
            ioDispatcher = Dispatchers.Unconfined,
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun test_hierarchical_tag_rename_cascades_to_child_tags() = runBlocking {
        // Create parent and child tags
        val parent = tagService.getOrCreateTag("work/mobile").getOrThrow()
        val child = tagService.getOrCreateTag("work/mobile/android").getOrThrow()

        // Rename parent tag
        val renameResult = tagService.renameTag(parent.id, "work-v2/mobile")
        assertTrue(renameResult.isOk)

        // Verify parent renamed
        val updatedParent = tagRepository.getById(parent.id)
        assertEquals("work-v2/mobile", updatedParent?.name)

        // Verify child tag path cascaded
        val updatedChild = tagRepository.getById(child.id)
        assertEquals("work-v2/mobile/android", updatedChild?.name)
    }

    @Test
    fun test_tag_merge_repoints_tasks_and_removes_source_without_orphans() = runBlocking {
        val workspaceId = DeterministicIds.defaultWorkspaceId()
        val task1 = taskService.createTask("Task One", workspaceId).getOrThrow()
        val task2 = taskService.createTask("Task Two", workspaceId).getOrThrow()

        val tagA = tagService.getOrCreateTag("urgent").getOrThrow()
        val tagB = tagService.getOrCreateTag("critical").getOrThrow()

        tagService.addTagToTask(task1.id, tagA.id)
        tagService.addTagToTask(task2.id, tagA.id)
        // task2 already has tagB as well
        tagService.addTagToTask(task2.id, tagB.id)

        // Merge tagA into tagB
        val mergeResult = tagService.mergeTags(sourceTagId = tagA.id, targetTagId = tagB.id)
        assertTrue(mergeResult.isOk)

        // Source tagA is removed
        assertNull(tagRepository.getById(tagA.id))

        // Target tagB exists
        assertNotNull(tagRepository.getById(tagB.id))

        // task1 now has tagB
        val task1Tags = tagService.observeTagsForTask(task1.id).first()
        assertTrue(task1Tags.any { it.id == tagB.id })

        // task2 has tagB without duplicate associations
        val task2Tags = tagService.observeTagsForTask(task2.id).first()
        assertEquals(1, task2Tags.count { it.id == tagB.id })

        // No associations point to deleted tagA
        val associationsToA = db.taskTagDao().getTaskIdsForTag(tagA.id)
        assertTrue(associationsToA.isEmpty())
    }

    @Test
    fun test_tag_delete_removes_associations_leaving_tasks_intact() = runBlocking {
        val workspaceId = DeterministicIds.defaultWorkspaceId()
        val task = taskService.createTask("Independent Task", workspaceId).getOrThrow()
        val tag = tagService.getOrCreateTag("disposable").getOrThrow()

        tagService.addTagToTask(task.id, tag.id)
        assertTrue(tagService.observeTagsForTask(task.id).first().any { it.id == tag.id })

        // Delete tag
        val deleteResult = tagService.deleteTag(tag.id)
        assertTrue(deleteResult.isOk)

        // Tag deleted
        assertNull(tagRepository.getById(tag.id))

        // Task still exists
        val loadedTask = taskRepository.getById(task.id)
        assertNotNull(loadedTask)

        // Task has no tags
        val remainingTags = tagService.observeTagsForTask(task.id).first()
        assertTrue(remainingTags.isEmpty())
    }
}
