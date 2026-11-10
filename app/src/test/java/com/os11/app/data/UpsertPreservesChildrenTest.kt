package com.os11.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.os11.app.data.db.DatabaseInitializer
import com.os11.app.data.db.OS11Database
import com.os11.app.data.db.entities.AreaEntity
import com.os11.app.data.db.entities.AttachmentEntity
import com.os11.app.data.db.entities.CommentEntity
import com.os11.app.data.db.entities.GoalEntity
import com.os11.app.data.db.entities.GoalLinkEntity
import com.os11.app.data.db.entities.MilestoneEntity
import com.os11.app.data.db.entities.ProjectEntity
import com.os11.app.data.db.entities.ReminderEntity
import com.os11.app.data.db.entities.SectionEntity
import com.os11.app.data.db.entities.TagEntity
import com.os11.app.data.db.entities.TaskEntity
import com.os11.app.data.sync.TrackedWriter
import com.os11.domain.id.DefaultIdGenerator
import com.os11.domain.id.DeterministicIds
import com.os11.domain.logic.sync.DefaultHlcClock
import com.os11.domain.time.TestClock
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

/**
 * Regression tests for a data-loss bug in the tracked write path.
 *
 * Upserts used `INSERT OR REPLACE`, which SQLite executes as DELETE + INSERT. With foreign keys enforced,
 * that DELETE fired `ON DELETE CASCADE` / `SET NULL` on every child row, so simply editing a parent row
 * (completing a task, renaming a tag, renaming a project) deleted or detached its children.
 *
 * Every test creates a parent with children, edits the parent through [TrackedWriter], and asserts that
 * the children are still there and still attached.
 */
@RunWith(RobolectricTestRunner::class)
@Suppress("TooManyFunctions")
class UpsertPreservesChildrenTest {

    private lateinit var db: OS11Database
    private lateinit var clock: TestClock
    private lateinit var writer: TrackedWriter
    private val workspaceId = DeterministicIds.defaultWorkspaceId()

    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = OS11Database.buildInMemory(context)
        clock = TestClock(1_000_000L)
        writer = TrackedWriter(db, DefaultHlcClock(clock, "device01"), clock, DefaultIdGenerator())
        DatabaseInitializer.seedDefaults(db, clock)
    }

    @After
    fun tearDown() {
        db.close()
    }

    // --- helpers ---

    private fun newId() = UUID.randomUUID().toString()

    private fun task(
        title: String,
        parentTaskId: String? = null,
        areaId: String? = null,
        projectId: String? = null,
        sectionId: String? = null,
    ) = TaskEntity(
        id = newId(),
        title = title,
        workspaceId = workspaceId,
        areaId = areaId,
        projectId = projectId,
        sectionId = sectionId,
        parentTaskId = parentTaskId,
        sortOrder = 1.0,
        createdAt = clock.nowIso(),
        updatedAt = clock.nowIso(),
    )

    private suspend fun editTask(id: String): TaskEntity {
        clock.advanceBy(1_000L)
        val current = db.taskDao().getById(id)!!
        return writer.writeTask(current.copy(title = current.title + " (edited)", updatedAt = clock.nowIso()))
    }

    private suspend fun newArea(): AreaEntity = writer.writeArea(
        AreaEntity(
            id = newId(),
            workspaceId = workspaceId,
            name = "Work",
            createdAt = clock.nowIso(),
            updatedAt = clock.nowIso(),
        ),
    )

    private suspend fun newSection(projectId: String): SectionEntity = writer.writeSection(
        SectionEntity(
            id = newId(),
            projectId = projectId,
            name = "Backlog",
            createdAt = clock.nowIso(),
            updatedAt = clock.nowIso(),
        ),
    )

    private suspend fun newProject(areaId: String?): ProjectEntity = writer.writeProject(
        ProjectEntity(
            id = newId(),
            workspaceId = workspaceId,
            areaId = areaId,
            name = "Launch",
            createdAt = clock.nowIso(),
            updatedAt = clock.nowIso(),
        ),
    )

    // --- sanity: the test database enforces foreign keys, otherwise every test below could pass falsely ---

    @Test
    fun foreign_keys_are_enforced_in_the_test_database() = runBlocking {
        val parent = writer.writeTask(task("parent"))
        val child = writer.writeTask(task("child", parentTaskId = parent.id))

        db.taskDao().deleteById(parent.id)

        assertNull("FK cascade must be active for these tests to mean anything", db.taskDao().getById(child.id))
    }

    // --- editing a task ---

    @Test
    fun editing_a_task_keeps_its_subtasks() = runBlocking {
        val parent = writer.writeTask(task("parent"))
        writer.writeTask(task("child", parentTaskId = parent.id))

        editTask(parent.id)

        assertEquals("subtasks must survive an edit of their parent", 1, db.taskDao().getSubtasks(parent.id).size)
    }

    @Test
    fun editing_a_task_keeps_its_tags() = runBlocking {
        val parent = writer.writeTask(task("parent"))
        val tag = writer.writeTag(TagEntity(id = newId(), name = "work", createdAt = clock.nowIso()))
        writer.writeTaskTag(parent.id, tag.id)

        editTask(parent.id)

        assertEquals(listOf(tag.id), db.taskTagDao().getTagIdsForTask(parent.id))
    }

    @Test
    fun editing_a_task_keeps_its_reminders() = runBlocking {
        val parent = writer.writeTask(task("parent"))
        writer.writeReminder(
            ReminderEntity(
                id = newId(),
                taskId = parent.id,
                remindAt = "2026-12-01T09:00:00Z",
                createdAt = clock.nowIso(),
            ),
        )

        editTask(parent.id)

        assertEquals(1, db.reminderDao().getByTask(parent.id).size)
    }

    @Test
    fun editing_a_task_keeps_its_attachments() = runBlocking {
        val parent = writer.writeTask(task("parent"))
        writer.writeAttachment(
            AttachmentEntity(
                id = newId(),
                taskId = parent.id,
                filename = "a.txt",
                originalName = "a.txt",
                mimeType = "text/plain",
                sizeBytes = 10L,
                createdAt = clock.nowIso(),
            ),
        )

        editTask(parent.id)

        assertEquals(1, db.attachmentDao().getByTask(parent.id).size)
    }

    @Test
    fun editing_a_task_keeps_its_dependencies() = runBlocking {
        val parent = writer.writeTask(task("parent"))
        val other = writer.writeTask(task("other"))
        writer.writeDependency(parent.id, other.id)

        editTask(parent.id)
        editTask(other.id)

        assertEquals(listOf(other.id), db.taskDependencyDao().getDependenciesForTask(parent.id))
    }

    @Test
    fun editing_a_task_keeps_its_comments() = runBlocking {
        val parent = writer.writeTask(task("parent"))
        val comment = writer.writeComment(
            CommentEntity(
                id = newId(),
                taskId = parent.id,
                authorName = "me",
                body = "hello",
                createdAt = clock.nowIso(),
                updatedAt = clock.nowIso(),
            ),
        )

        editTask(parent.id)

        assertNotNull("comment must survive an edit of its task", db.commentDao().getById(comment.id))
    }

    // --- editing a tag ---

    @Test
    fun renaming_a_tag_keeps_it_on_every_task() = runBlocking {
        val tag = writer.writeTag(TagEntity(id = newId(), name = "work", createdAt = clock.nowIso()))
        val first = writer.writeTask(task("one"))
        val second = writer.writeTask(task("two"))
        writer.writeTaskTag(first.id, tag.id)
        writer.writeTaskTag(second.id, tag.id)

        clock.advanceBy(1_000L)
        writer.writeTag(db.tagDao().getById(tag.id)!!.copy(name = "work-renamed"))

        assertEquals(setOf(first.id, second.id), db.taskTagDao().getTaskIdsForTag(tag.id).toSet())
    }

    @Test
    fun renaming_a_tag_keeps_its_child_tags_attached() = runBlocking {
        val parentTag = writer.writeTag(TagEntity(id = newId(), name = "work", createdAt = clock.nowIso()))
        val childTag = writer.writeTag(
            TagEntity(id = newId(), name = "work/mobile", parentTagId = parentTag.id, createdAt = clock.nowIso()),
        )

        clock.advanceBy(1_000L)
        writer.writeTag(db.tagDao().getById(parentTag.id)!!.copy(name = "job"))

        assertEquals(parentTag.id, db.tagDao().getById(childTag.id)?.parentTagId)
    }

    // --- editing a project / section ---

    @Test
    fun editing_a_project_keeps_its_sections_and_milestones() = runBlocking {
        val project = newProject(areaId = null)
        newSection(project.id)
        val milestone = writer.writeMilestone(
            MilestoneEntity(
                id = newId(),
                projectId = project.id,
                title = "Beta",
                dueDate = "2026-12-31",
                createdAt = clock.nowIso(),
            ),
        )

        clock.advanceBy(1_000L)
        writer.writeProject(db.projectDao().getById(project.id)!!.copy(name = "Launch v2", updatedAt = clock.nowIso()))

        assertEquals("sections must survive a project edit", 1, db.sectionDao().getByProject(project.id).size)
        assertNotNull("milestones must survive a project edit", db.milestoneDao().getById(milestone.id))
    }

    @Test
    fun editing_a_project_keeps_its_tasks_in_it() = runBlocking {
        val project = newProject(areaId = null)
        val inProject = writer.writeTask(task("planned work", projectId = project.id))

        clock.advanceBy(1_000L)
        writer.writeProject(db.projectDao().getById(project.id)!!.copy(name = "Launch v2", updatedAt = clock.nowIso()))

        assertEquals(project.id, db.taskDao().getById(inProject.id)?.projectId)
    }

    @Test
    fun editing_a_section_keeps_its_tasks_in_it() = runBlocking {
        val project = newProject(areaId = null)
        val section = newSection(project.id)
        val inSection = writer.writeTask(task("in section", projectId = project.id, sectionId = section.id))

        clock.advanceBy(1_000L)
        writer.writeSection(db.sectionDao().getById(section.id)!!.copy(name = "Backlog 2", updatedAt = clock.nowIso()))

        assertEquals(section.id, db.taskDao().getById(inSection.id)?.sectionId)
    }

    // --- editing an area / workspace ---

    @Test
    fun editing_an_area_keeps_its_projects_and_tasks_in_it() = runBlocking {
        val area = newArea()
        val project = newProject(areaId = area.id)
        val looseTask = writer.writeTask(task("loose in area", areaId = area.id))

        clock.advanceBy(1_000L)
        writer.writeArea(db.areaDao().getById(area.id)!!.copy(name = "Job", updatedAt = clock.nowIso()))

        assertEquals("project must stay in its area", area.id, db.projectDao().getById(project.id)?.areaId)
        assertEquals("task must stay in its area", area.id, db.taskDao().getById(looseTask.id)?.areaId)
    }

    @Test
    fun editing_the_workspace_keeps_its_areas_projects_and_tasks() = runBlocking {
        val area = newArea()
        val project = newProject(areaId = area.id)
        val any = writer.writeTask(task("anything", areaId = area.id, projectId = project.id))

        clock.advanceBy(1_000L)
        val workspace = db.workspaceDao().getById(workspaceId)!!
        writer.writeWorkspace(workspace.copy(name = "Renamed", updatedAt = clock.nowIso()))

        assertNotNull("area must survive a workspace edit", db.areaDao().getById(area.id))
        assertNotNull("project must survive a workspace edit", db.projectDao().getById(project.id))
        assertNotNull("task must survive a workspace edit", db.taskDao().getById(any.id))
    }

    // --- editing a goal / comment ---

    @Test
    fun editing_a_goal_keeps_its_links() = runBlocking {
        val linkedTask = writer.writeTask(task("linked"))
        val goal = writer.writeGoal(
            GoalEntity(
                id = newId(),
                title = "Ship it",
                goalType = "outcome",
                createdAt = clock.nowIso(),
                updatedAt = clock.nowIso(),
            ),
        )
        writer.writeGoalLink(GoalLinkEntity(goalId = goal.id, resourceType = "task", resourceId = linkedTask.id))

        clock.advanceBy(1_000L)
        writer.writeGoal(db.goalDao().getById(goal.id)!!.copy(title = "Ship it v2", updatedAt = clock.nowIso()))

        assertEquals("goal links must survive a goal edit", 1, db.goalLinkDao().getAll().size)
    }

    @Test
    fun editing_a_comment_keeps_its_replies() = runBlocking {
        val parentTask = writer.writeTask(task("commented"))
        val comment = writer.writeComment(
            CommentEntity(
                id = newId(),
                taskId = parentTask.id,
                authorName = "me",
                body = "first",
                createdAt = clock.nowIso(),
                updatedAt = clock.nowIso(),
            ),
        )
        val reply = writer.writeComment(
            CommentEntity(
                id = newId(),
                taskId = parentTask.id,
                authorName = "me",
                body = "reply",
                parentCommentId = comment.id,
                createdAt = clock.nowIso(),
                updatedAt = clock.nowIso(),
            ),
        )

        clock.advanceBy(1_000L)
        val current = db.commentDao().getById(comment.id)!!
        writer.writeComment(current.copy(body = "first (edited)", updatedAt = clock.nowIso()))

        assertNotNull("replies must survive an edit of the comment they answer", db.commentDao().getById(reply.id))
    }
}
