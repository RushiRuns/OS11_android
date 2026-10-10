package com.os11.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.os11.app.data.db.DatabaseInitializer
import com.os11.app.data.db.OS11Database
import com.os11.app.data.db.entities.ProjectEntity
import com.os11.app.data.db.entities.TagEntity
import com.os11.app.data.db.entities.TaskEntity
import com.os11.app.data.repository.AreaRepositoryImpl
import com.os11.app.data.repository.CountsRepositoryImpl
import com.os11.app.data.repository.ProjectRepositoryImpl
import com.os11.app.data.repository.SearchRepositoryImpl
import com.os11.app.data.repository.TagRepositoryImpl
import com.os11.app.data.repository.TaskRepositoryImpl
import com.os11.app.data.sync.TrackedWriter
import com.os11.domain.id.DefaultIdGenerator
import com.os11.domain.id.DeterministicIds
import com.os11.domain.logic.sync.DefaultHlcClock
import com.os11.domain.time.TestClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class RepositoryTest {

    private lateinit var db: OS11Database
    private lateinit var testClock: TestClock
    private lateinit var hlcClock: DefaultHlcClock
    private lateinit var trackedWriter: TrackedWriter

    private lateinit var taskRepository: TaskRepositoryImpl
    private lateinit var areaRepository: AreaRepositoryImpl
    private lateinit var projectRepository: ProjectRepositoryImpl
    private lateinit var tagRepository: TagRepositoryImpl
    private lateinit var countsRepository: CountsRepositoryImpl
    private lateinit var searchRepository: SearchRepositoryImpl

    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = OS11Database.buildInMemory(context)
        testClock = TestClock(1_000_000L)
        hlcClock = DefaultHlcClock(testClock, "device01")
        trackedWriter = TrackedWriter(db, hlcClock, testClock, DefaultIdGenerator())
        DatabaseInitializer.seedDefaults(db, testClock)

        taskRepository = TaskRepositoryImpl(db, trackedWriter, testClock)
        areaRepository = AreaRepositoryImpl(db, trackedWriter)
        projectRepository = ProjectRepositoryImpl(db, trackedWriter)
        tagRepository = TagRepositoryImpl(db, trackedWriter)
        countsRepository = CountsRepositoryImpl(db)
        searchRepository = SearchRepositoryImpl(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun task_repository_slim_projections_and_inbox_filtering() = runBlocking {
        val workspaceId = DeterministicIds.defaultWorkspaceId()

        // 1. Inbox task (null area, null project, null parent, null due date)
        val inboxTask = TaskEntity(
            id = UUID.randomUUID().toString(),
            title = "Inbox item",
            notes = "<p>Super secret heavy notes</p>",
            workspaceId = workspaceId,
            sortOrder = 1.0,
            createdAt = testClock.nowIso(),
            updatedAt = testClock.nowIso(),
        )
        taskRepository.saveTask(inboxTask)

        // 2. Project task
        val projectId = UUID.randomUUID().toString()
        projectRepository.save(
            ProjectEntity(
                id = projectId,
                workspaceId = workspaceId,
                name = "Test Project",
                createdAt = testClock.nowIso(),
                updatedAt = testClock.nowIso(),
            ),
        )
        val projectTask = TaskEntity(
            id = UUID.randomUUID().toString(),
            title = "Project item",
            workspaceId = workspaceId,
            projectId = projectId,
            sortOrder = 2.0,
            createdAt = testClock.nowIso(),
            updatedAt = testClock.nowIso(),
        )
        taskRepository.saveTask(projectTask)

        // Verify observeInboxSlim contains only inboxTask
        val inboxList = taskRepository.observeInboxSlim().first()
        assertEquals(1, inboxList.size)
        assertEquals(inboxTask.id, inboxList[0].id)
        assertEquals("Inbox item", inboxList[0].title)

        // Verify observeByProjectSlim contains projectTask
        val projectList = taskRepository.observeByProjectSlim(projectId).first()
        assertEquals(1, projectList.size)
        assertEquals(projectTask.id, projectList[0].id)
    }

    @Test
    fun tag_repository_batched_loading_for_visible_windows() = runBlocking {
        val tag1 = tagRepository.save(
            TagEntity(id = UUID.randomUUID().toString(), name = "work", createdAt = testClock.nowIso()),
        )
        val tag2 = tagRepository.save(
            TagEntity(id = UUID.randomUUID().toString(), name = "urgent", createdAt = testClock.nowIso()),
        )

        val workspaceId = DeterministicIds.defaultWorkspaceId()
        val task1Id = UUID.randomUUID().toString()
        val task2Id = UUID.randomUUID().toString()
        val task3Id = UUID.randomUUID().toString()

        taskRepository.saveTask(
            TaskEntity(
                id = task1Id,
                title = "Task 1",
                workspaceId = workspaceId,
                createdAt = testClock.nowIso(),
                updatedAt = testClock.nowIso(),
            ),
        )
        taskRepository.saveTask(
            TaskEntity(
                id = task2Id,
                title = "Task 2",
                workspaceId = workspaceId,
                createdAt = testClock.nowIso(),
                updatedAt = testClock.nowIso(),
            ),
        )
        taskRepository.saveTask(
            TaskEntity(
                id = task3Id,
                title = "Task 3",
                workspaceId = workspaceId,
                createdAt = testClock.nowIso(),
                updatedAt = testClock.nowIso(),
            ),
        )

        tagRepository.addTagToTask(task1Id, tag1.id)
        tagRepository.addTagToTask(task1Id, tag2.id)
        tagRepository.addTagToTask(task2Id, tag1.id)

        // Batched loading across a window of task IDs
        val batchedTaskTags = tagRepository.getTagsForTasks(listOf(task1Id, task2Id, task3Id))
        assertEquals(3, batchedTaskTags.size)

        val tagsForTask1 = batchedTaskTags.filter { it.taskId == task1Id }.map { it.tagId }
        assertTrue(tagsForTask1.contains(tag1.id))
        assertTrue(tagsForTask1.contains(tag2.id))

        val tagsForTask2 = batchedTaskTags.filter { it.taskId == task2Id }.map { it.tagId }
        assertEquals(1, tagsForTask2.size)
        assertEquals(tag1.id, tagsForTask2[0])
    }

    @Test
    @Suppress("LongMethod")
    fun counts_repository_computes_browse_badges_in_single_query() = runBlocking {
        val workspaceId = DeterministicIds.defaultWorkspaceId()
        val today = "2026-10-10"

        val projectId = UUID.randomUUID().toString()
        projectRepository.save(
            ProjectEntity(
                id = projectId,
                workspaceId = workspaceId,
                name = "Test Project",
                createdAt = testClock.nowIso(),
                updatedAt = testClock.nowIso(),
            ),
        )

        // Task in Inbox
        taskRepository.saveTask(
            TaskEntity(
                id = UUID.randomUUID().toString(),
                title = "Inbox item",
                workspaceId = workspaceId,
                createdAt = testClock.nowIso(),
                updatedAt = testClock.nowIso(),
            ),
        )

        // Task in Today
        taskRepository.saveTask(
            TaskEntity(
                id = UUID.randomUUID().toString(),
                title = "My Day item",
                workspaceId = workspaceId,
                projectId = projectId,
                myDayDate = today,
                createdAt = testClock.nowIso(),
                updatedAt = testClock.nowIso(),
            ),
        )

        // Task in Important
        taskRepository.saveTask(
            TaskEntity(
                id = UUID.randomUUID().toString(),
                title = "Important item",
                workspaceId = workspaceId,
                projectId = projectId,
                isStarred = true,
                createdAt = testClock.nowIso(),
                updatedAt = testClock.nowIso(),
            ),
        )

        // Task Completed
        taskRepository.saveTask(
            TaskEntity(
                id = UUID.randomUUID().toString(),
                title = "Done item",
                workspaceId = workspaceId,
                isCompleted = true,
                createdAt = testClock.nowIso(),
                updatedAt = testClock.nowIso(),
            ),
        )

        // Task Trashed
        taskRepository.saveTask(
            TaskEntity(
                id = UUID.randomUUID().toString(),
                title = "Trash item",
                workspaceId = workspaceId,
                isTrashed = true,
                trashedAt = testClock.nowIso(),
                createdAt = testClock.nowIso(),
                updatedAt = testClock.nowIso(),
            ),
        )

        val counts = countsRepository.observeBrowseCounts(today).first()
        assertEquals(1, counts.inboxCount)
        assertEquals(1, counts.myDayCount)
        assertEquals(1, counts.importantCount)
        assertEquals(1, counts.completedCount)
        assertEquals(1, counts.trashCount)
    }
}
