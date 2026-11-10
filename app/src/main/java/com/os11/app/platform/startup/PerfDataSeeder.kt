package com.os11.app.platform.startup

import android.util.Log
import com.os11.app.data.db.OS11Database
import com.os11.app.data.db.entities.AreaEntity
import com.os11.app.data.db.entities.ProjectEntity
import com.os11.app.data.db.entities.SectionEntity
import com.os11.app.data.db.entities.TagEntity
import com.os11.app.data.db.entities.TaskEntity
import com.os11.app.data.sync.TrackedWriter
import com.os11.domain.id.DeterministicIds
import com.os11.domain.id.IdGenerator
import com.os11.domain.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Fills the separate "perf" build with realistic volume (about 2,000 tasks) so speed can be measured with
 * real-sized data instead of the one or two tasks a fresh install has.
 *
 * It is only called when `BuildConfig.PERF_SEED` is true, which is the case for the `perf` build type alone,
 * so debug and release builds never run it. Every write goes through [TrackedWriter], so write costs are real.
 * Seeding is skipped if the marker tag already exists. Progress and timing are logged under the `PerfSeed` tag.
 */
@Singleton
@Suppress("MagicNumber", "LongMethod")
class PerfDataSeeder @Inject constructor(
    private val db: OS11Database,
    private val writer: TrackedWriter,
    private val clock: Clock,
    private val ids: IdGenerator,
) {
    suspend fun seedIfNeeded() {
        if (db.tagDao().getByName(MARKER_TAG) != null) {
            Log.i(TAG, "already seeded, skipping")
            return
        }
        val startedMs = System.currentTimeMillis()
        val random = Random(SEED)
        val workspaceId = DeterministicIds.defaultWorkspaceId()
        val today = LocalDate.now()
        Log.i(TAG, "seeding started")

        writer.writeTag(TagEntity(id = ids.randomId(), name = MARKER_TAG, createdAt = clock.nowIso()))
        val tagIds = (1..TAG_COUNT).map { n ->
            writer.writeTag(TagEntity(id = ids.randomId(), name = "label-$n", createdAt = clock.nowIso())).id
        }

        val projects = mutableListOf<ProjectEntity>()
        val sectionsByProject = mutableMapOf<String, List<SectionEntity>>()
        AREA_NAMES.forEachIndexed { areaIndex, areaName ->
            val area = writer.writeArea(
                AreaEntity(
                    id = ids.randomId(),
                    workspaceId = workspaceId,
                    name = areaName,
                    sortOrder = areaIndex.toDouble(),
                    createdAt = clock.nowIso(),
                    updatedAt = clock.nowIso(),
                ),
            )
            repeat(PROJECTS_PER_AREA) { projectIndex ->
                val project = writer.writeProject(
                    ProjectEntity(
                        id = ids.randomId(),
                        workspaceId = workspaceId,
                        areaId = area.id,
                        name = "$areaName project ${projectIndex + 1}",
                        sortOrder = projectIndex.toDouble(),
                        createdAt = clock.nowIso(),
                        updatedAt = clock.nowIso(),
                    ),
                )
                projects += project
                sectionsByProject[project.id] = SECTION_NAMES.mapIndexed { sectionIndex, sectionName ->
                    writer.writeSection(
                        SectionEntity(
                            id = ids.randomId(),
                            projectId = project.id,
                            name = sectionName,
                            sortOrder = sectionIndex.toDouble(),
                            createdAt = clock.nowIso(),
                            updatedAt = clock.nowIso(),
                        ),
                    )
                }
            }
        }

        val parents = mutableListOf<TaskEntity>()
        repeat(TOP_LEVEL_TASKS) { i ->
            val project = if (random.nextInt(100) < INBOX_PERCENT) null else projects.pick(random)
            val section = project?.let { sectionsByProject.getValue(it.id).pick(random) }
            val done = random.nextInt(100) < DONE_PERCENT
            val dueDate = if (random.nextInt(100) < DUE_PERCENT) {
                today.plusDays(random.nextLong(-20L, 45L)).toString()
            } else {
                null
            }
            val task = writer.writeTask(
                TaskEntity(
                    id = ids.randomId(),
                    title = "${VERBS.pick(random)} ${NOUNS.pick(random)} #${i + 1}",
                    notes = if (random.nextInt(100) < NOTES_PERCENT) note(random) else null,
                    workspaceId = workspaceId,
                    areaId = project?.areaId,
                    projectId = project?.id,
                    sectionId = section?.id,
                    dueDate = dueDate,
                    priority = priority(random),
                    isStarred = random.nextInt(100) < STARRED_PERCENT,
                    isCompleted = done,
                    completedAt = if (done) clock.nowIso() else null,
                    sortOrder = i.toDouble(),
                    myDayDate = if (i % MY_DAY_EVERY == 0) today.toString() else null,
                    createdAt = clock.nowIso(),
                    updatedAt = clock.nowIso(),
                ),
            )
            if (!done) parents += task
            if (random.nextInt(100) < TAGGED_PERCENT) {
                tagIds.shuffled(random).take(1 + random.nextInt(3)).forEach { writer.writeTaskTag(task.id, it) }
            }
            if ((i + 1) % PROGRESS_EVERY == 0) Log.i(TAG, "${i + 1} of $TOP_LEVEL_TASKS tasks written")
        }

        var subtasks = 0
        parents.take(PARENTS_WITH_SUBTASKS).forEach { parent ->
            repeat(2 + random.nextInt(2)) { n ->
                writer.writeTask(
                    TaskEntity(
                        id = ids.randomId(),
                        title = "${VERBS.pick(random)} ${NOUNS.pick(random)} (step ${n + 1})",
                        workspaceId = workspaceId,
                        areaId = parent.areaId,
                        projectId = parent.projectId,
                        sectionId = parent.sectionId,
                        parentTaskId = parent.id,
                        sortOrder = n.toDouble(),
                        createdAt = clock.nowIso(),
                        updatedAt = clock.nowIso(),
                    ),
                )
                subtasks++
            }
        }

        val tookMs = System.currentTimeMillis() - startedMs
        val total = TOP_LEVEL_TASKS + subtasks
        Log.i(TAG, "seeding done: $total tasks, ${projects.size} projects, $TAG_COUNT tags in $tookMs ms")
    }

    private fun <T> List<T>.pick(random: Random): T = this[random.nextInt(size)]

    private fun priority(random: Random): Int = when (random.nextInt(10)) {
        in 0..5 -> 0
        in 6..7 -> 1
        8 -> 2
        else -> 3
    }

    private fun note(random: Random): String {
        val body = (1..4).joinToString(" ") { NOTE_SENTENCES.pick(random) }
        return "<p>$body</p>"
    }

    private companion object {
        const val TAG = "PerfSeed"
        const val MARKER_TAG = "perf-seed-marker"
        const val SEED = 42
        const val TAG_COUNT = 40
        const val PROJECTS_PER_AREA = 4
        const val TOP_LEVEL_TASKS = 1700
        const val PARENTS_WITH_SUBTASKS = 120
        const val INBOX_PERCENT = 20
        const val DONE_PERCENT = 12
        const val DUE_PERCENT = 35
        const val NOTES_PERCENT = 25
        const val STARRED_PERCENT = 8
        const val TAGGED_PERCENT = 40
        const val MY_DAY_EVERY = 40
        const val PROGRESS_EVERY = 250

        val AREA_NAMES = listOf("Work", "Personal", "Health", "Learning", "Home", "Finance")
        val SECTION_NAMES = listOf("Backlog", "In progress", "Waiting")
        val VERBS = listOf("Review", "Draft", "Call", "Email", "Plan", "Fix", "Book", "Read", "Update", "Send")
        val NOUNS = listOf(
            "budget", "report", "invoice", "proposal", "roadmap", "contract", "slides", "schedule",
            "backlog", "notes", "estimate", "checklist", "agenda", "feedback", "summary",
        )
        val NOTE_SENTENCES = listOf(
            "Remember to check the latest figures before the review.",
            "Share a short summary with the team once this is done.",
            "Waiting on a reply, so follow up if nothing arrives by Friday.",
            "Keep this brief and link the supporting document.",
            "Double check the dates and confirm with the other side.",
        )
    }
}
