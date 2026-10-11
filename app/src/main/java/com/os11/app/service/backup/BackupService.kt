package com.os11.app.service.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.os11.app.data.db.OS11Database
import com.os11.app.data.db.entities.AreaEntity
import com.os11.app.data.db.entities.GoalEntity
import com.os11.app.data.db.entities.GoalLinkEntity
import com.os11.app.data.db.entities.MilestoneEntity
import com.os11.app.data.db.entities.PomodoroSessionEntity
import com.os11.app.data.db.entities.ProjectEntity
import com.os11.app.data.db.entities.ReminderEntity
import com.os11.app.data.db.entities.SectionEntity
import com.os11.app.data.db.entities.SettingEntity
import com.os11.app.data.db.entities.TagEntity
import com.os11.app.data.db.entities.TaskEntity
import com.os11.app.data.db.entities.VersionHistoryEntity
import com.os11.app.data.db.entities.WorkspaceEntity
import com.os11.app.data.sync.TrackedWriter
import com.os11.app.di.IoDispatcher
import com.os11.domain.id.DeterministicIds
import com.os11.domain.id.IdGenerator
import com.os11.domain.result.AppError
import com.os11.domain.result.AppResult
import com.os11.domain.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("TooGenericExceptionCaught", "LongMethod", "CyclomaticComplexMethod", "LargeClass")
class BackupService @Inject constructor(
    private val db: OS11Database,
    private val trackedWriter: TrackedWriter,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    // --- JSON Export ---

    suspend fun exportToJson(): AppResult<String> = withContext(ioDispatcher) {
        try {
            val workspaces = db.workspaceDao().getAll().map {
                WorkspaceExportDto(
                    id = it.id,
                    name = it.name,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt,
                )
            }

            val areas = db.areaDao().getAll().map {
                AreaExportDto(
                    id = it.id,
                    name = it.name,
                    workspaceId = it.workspaceId,
                    sortOrder = it.sortOrder,
                    isDefault = it.isDefault,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt,
                )
            }

            val projects = db.projectDao().getAll().map {
                ProjectExportDto(
                    id = it.id,
                    name = it.name,
                    workspaceId = it.workspaceId,
                    areaId = it.areaId,
                    status = it.status,
                    sortOrder = it.sortOrder,
                    defaultView = it.defaultView,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt,
                )
            }

            val sections = db.sectionDao().getAll().map {
                SectionExportDto(
                    id = it.id,
                    projectId = it.projectId,
                    name = it.name,
                    sortOrder = it.sortOrder,
                    isCollapsed = it.isCollapsed,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt,
                )
            }

            val tasks = db.taskDao().getAll().map {
                TaskExportDto(
                    id = it.id,
                    title = it.title,
                    notes = it.notes.orEmpty(),
                    workspaceId = it.workspaceId,
                    areaId = it.areaId,
                    projectId = it.projectId,
                    sectionId = it.sectionId,
                    parentTaskId = it.parentTaskId,
                    dueDate = it.dueDate,
                    dueTime = it.dueTime,
                    allDay = it.allDay,
                    recurrenceRule = it.recurrenceRule,
                    recurrenceBasis = it.recurrenceBasis ?: "due_date",
                    priority = it.priority,
                    isStarred = it.isStarred,
                    isCompleted = it.isCompleted,
                    completedAt = it.completedAt,
                    estimatedMinutes = it.estimatedMinutes,
                    sortOrder = it.sortOrder,
                    myDayDate = it.myDayDate,
                    pomodoroCount = it.pomodoroCount,
                    scheduledStartMin = it.scheduledStartMin,
                    scheduledDurationMin = it.scheduledDurationMin,
                    bucket = it.bucket ?: "inbox",
                    waitingOn = it.waitingOn,
                    waitingSince = it.waitingSince,
                    followUpDate = it.followUpDate,
                    reviewedAt = it.reviewedAt,
                    isTrashed = it.isTrashed,
                    trashedAt = it.trashedAt,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt,
                )
            }

            val tags = db.tagDao().getAll().map {
                TagExportDto(
                    id = it.id,
                    name = it.name,
                    color = it.color ?: "#808080",
                    sortOrder = it.sortOrder,
                    createdAt = it.createdAt,
                )
            }

            val taskTags = db.taskTagDao().getAll().map {
                TaskTagExportDto(
                    taskId = it.taskId,
                    tagId = it.tagId,
                )
            }

            val taskDependencies = db.taskDependencyDao().getAllDependencyPairs().map {
                TaskDependencyExportDto(
                    taskId = it.taskId,
                    dependsOnId = it.dependsOnId,
                )
            }

            val milestones = db.milestoneDao().getAll().map {
                MilestoneExportDto(
                    id = it.id,
                    projectId = it.projectId,
                    title = it.title,
                    dueDate = it.dueDate,
                    isCompleted = it.isCompleted,
                    createdAt = it.createdAt,
                )
            }

            val reminders = db.reminderDao().getAll().map {
                ReminderExportDto(
                    id = it.id,
                    taskId = it.taskId,
                    remindAt = it.remindAt,
                    createdAt = it.createdAt,
                )
            }

            val pomodoroSessions = db.pomodoroSessionDao().getAll().map {
                PomodoroSessionExportDto(
                    id = it.id,
                    taskId = it.taskId,
                    type = it.type,
                    durationSeconds = it.durationSeconds,
                    startedAt = it.startedAt,
                    endedAt = it.endedAt,
                    wasCompleted = it.wasCompleted,
                    createdAt = it.createdAt,
                )
            }

            val goals = db.goalDao().getAll().map {
                GoalExportDto(
                    id = it.id,
                    title = it.title,
                    description = it.description,
                    goalType = it.goalType,
                    targetDate = it.targetDate,
                    targetValue = it.targetValue,
                    currentValue = it.currentValue,
                    streakCount = it.streakCount,
                    lastProgressAt = it.lastProgressAt,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt,
                )
            }

            val goalLinks = db.goalLinkDao().getAll().map {
                GoalLinkExportDto(
                    goalId = it.goalId,
                    resourceType = it.resourceType,
                    resourceId = it.resourceId,
                )
            }

            val settings = db.settingDao().getAll().associate {
                it.key to it.value
            }

            val backup = OS11BackupJson(
                version = 1,
                exportedAt = clock.nowIso(),
                source = "os11_android",
                workspaces = workspaces,
                areas = areas,
                projects = projects,
                sections = sections,
                tasks = tasks,
                tags = tags,
                taskTags = taskTags,
                taskDependencies = taskDependencies,
                milestones = milestones,
                reminders = reminders,
                pomodoroSessions = pomodoroSessions,
                goals = goals,
                goalLinks = goalLinks,
                settings = settings,
            )

            val jsonString = json.encodeToString(backup)

            // Record snapshot in version_history
            val history = VersionHistoryEntity(
                id = idGenerator.randomId(),
                entityType = "backup_export",
                entityId = "json",
                versionNumber = 1,
                snapshotData = "Exported ${tasks.size} tasks, ${projects.size} projects, ${areas.size} areas",
                createdAt = clock.nowIso(),
            )
            db.versionHistoryDao().insert(history)

            AppResult.ok(jsonString)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    // --- JSON Import (Mobile & Desktop Schema Normalization) ---

    suspend fun importFromJson(jsonString: String): AppResult<ImportResult> = withContext(ioDispatcher) {
        try {
            val backup = json.decodeFromString<OS11BackupJson>(jsonString)
            val now = clock.nowIso()
            val defaultWorkspaceId = DeterministicIds.defaultWorkspaceId()

            // 1. Workspaces
            if (backup.workspaces.isNotEmpty()) {
                backup.workspaces.forEach { dto ->
                    trackedWriter.writeWorkspace(
                        WorkspaceEntity(
                            id = dto.id,
                            name = dto.name,
                            createdAt = dto.createdAt ?: now,
                            updatedAt = dto.updatedAt ?: now,
                        ),
                    )
                }
            } else {
                trackedWriter.writeWorkspace(
                    WorkspaceEntity(
                        id = defaultWorkspaceId,
                        name = "Personal",
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            }

            // 2. Areas (normalize desktop legacy list_groups)
            val rawAreas = if (backup.areas.isNotEmpty()) {
                backup.areas
            } else {
                backup.legacyListGroups
            }
            val importedAreas = rawAreas.map { dto ->
                trackedWriter.writeArea(
                    AreaEntity(
                        id = dto.id,
                        workspaceId = dto.workspaceId ?: defaultWorkspaceId,
                        name = dto.name,
                        sortOrder = dto.sortOrder,
                        isDefault = dto.isDefault,
                        createdAt = dto.createdAt ?: now,
                        updatedAt = dto.updatedAt ?: now,
                    ),
                )
            }

            // 3. Projects (normalize desktop legacy lists)
            val importedProjects = if (backup.projects.isNotEmpty()) {
                backup.projects.map { dto ->
                    trackedWriter.writeProject(
                        ProjectEntity(
                            id = dto.id,
                            workspaceId = dto.workspaceId ?: defaultWorkspaceId,
                            areaId = dto.areaId,
                            name = dto.name,
                            status = dto.status,
                            sortOrder = dto.sortOrder,
                            defaultView = dto.defaultView,
                            createdAt = dto.createdAt ?: now,
                            updatedAt = dto.updatedAt ?: now,
                        ),
                    )
                }
            } else if (backup.legacyLists.isNotEmpty()) {
                backup.legacyLists.map { dto ->
                    trackedWriter.writeProject(
                        ProjectEntity(
                            id = dto.id,
                            workspaceId = defaultWorkspaceId,
                            areaId = dto.listGroupId,
                            name = dto.name,
                            status = "active",
                            sortOrder = dto.sortOrder,
                            defaultView = dto.defaultView,
                            createdAt = dto.createdAt ?: now,
                            updatedAt = dto.updatedAt ?: now,
                        ),
                    )
                }
            } else {
                emptyList()
            }

            // 4. Sections
            val importedSections = backup.sections.map { dto ->
                trackedWriter.writeSection(
                    SectionEntity(
                        id = dto.id,
                        projectId = dto.projectId,
                        name = dto.name,
                        sortOrder = dto.sortOrder,
                        isCollapsed = dto.isCollapsed,
                        createdAt = dto.createdAt ?: now,
                        updatedAt = dto.updatedAt ?: now,
                    ),
                )
            }

            // 5. Tags
            val importedTags = backup.tags.map { dto ->
                trackedWriter.writeTag(
                    TagEntity(
                        id = dto.id,
                        name = dto.name,
                        color = dto.color,
                        sortOrder = dto.sortOrder,
                        createdAt = dto.createdAt ?: now,
                    ),
                )
            }

            // 6. Tasks (handle desktop list_id -> project_id, description -> notes)
            val importedTasks = backup.tasks.map { dto ->
                val projectId = dto.projectId ?: dto.legacyListId
                val notes = dto.notes.ifEmpty { dto.legacyDescription ?: "" }

                trackedWriter.writeTask(
                    TaskEntity(
                        id = dto.id,
                        title = dto.title,
                        notes = notes,
                        workspaceId = dto.workspaceId ?: defaultWorkspaceId,
                        areaId = dto.areaId,
                        projectId = projectId,
                        sectionId = dto.sectionId,
                        parentTaskId = dto.parentTaskId,
                        dueDate = dto.dueDate,
                        dueTime = dto.dueTime,
                        allDay = dto.allDay,
                        recurrenceRule = dto.recurrenceRule,
                        recurrenceBasis = dto.recurrenceBasis,
                        priority = dto.priority,
                        isStarred = dto.isStarred,
                        isCompleted = dto.isCompleted,
                        completedAt = dto.completedAt,
                        estimatedMinutes = dto.estimatedMinutes,
                        sortOrder = dto.sortOrder,
                        myDayDate = dto.myDayDate,
                        pomodoroCount = dto.pomodoroCount,
                        scheduledStartMin = dto.scheduledStartMin,
                        scheduledDurationMin = dto.scheduledDurationMin,
                        bucket = dto.bucket,
                        waitingOn = dto.waitingOn,
                        waitingSince = dto.waitingSince,
                        followUpDate = dto.followUpDate,
                        reviewedAt = dto.reviewedAt,
                        isTrashed = dto.isTrashed,
                        trashedAt = dto.trashedAt,
                        createdAt = dto.createdAt ?: now,
                        updatedAt = dto.updatedAt ?: now,
                    ),
                )
            }

            // 7. Task Tags
            for (dto in backup.taskTags) {
                trackedWriter.writeTaskTag(dto.taskId, dto.tagId)
            }

            // 8. Task Dependencies
            for (dto in backup.taskDependencies) {
                trackedWriter.writeDependency(dto.taskId, dto.dependsOnId)
            }

            // 9. Milestones
            val importedMilestones = backup.milestones.map { dto ->
                trackedWriter.writeMilestone(
                    MilestoneEntity(
                        id = dto.id,
                        projectId = dto.projectId,
                        title = dto.title,
                        dueDate = dto.dueDate ?: clock.today(),
                        isCompleted = dto.isCompleted,
                        createdAt = dto.createdAt ?: now,
                    ),
                )
            }

            // 10. Reminders
            val importedReminders = backup.reminders.map { dto ->
                trackedWriter.writeReminder(
                    ReminderEntity(
                        id = dto.id,
                        taskId = dto.taskId,
                        remindAt = dto.remindAt,
                        createdAt = dto.createdAt ?: now,
                    ),
                )
            }

            // 11. Pomodoro Sessions
            for (dto in backup.pomodoroSessions) {
                trackedWriter.writePomodoroSession(
                    PomodoroSessionEntity(
                        id = dto.id,
                        taskId = dto.taskId,
                        type = dto.type,
                        durationSeconds = dto.durationSeconds,
                        startedAt = dto.startedAt,
                        endedAt = dto.endedAt ?: dto.completedAt,
                        wasCompleted = dto.wasCompleted || dto.completedAt != null,
                        createdAt = dto.createdAt ?: dto.startedAt,
                    ),
                )
            }

            // 12. Goals & Links
            for (dto in backup.goals) {
                trackedWriter.writeGoal(
                    GoalEntity(
                        id = dto.id,
                        title = dto.title,
                        description = dto.description,
                        goalType = dto.goalType,
                        targetDate = dto.targetDate,
                        targetValue = dto.targetValue,
                        currentValue = dto.currentValue,
                        streakCount = dto.streakCount,
                        lastProgressAt = dto.lastProgressAt,
                        createdAt = dto.createdAt ?: now,
                        updatedAt = dto.updatedAt ?: now,
                    ),
                )
            }
            for (dto in backup.goalLinks) {
                val rType = dto.entityType.ifEmpty { dto.resourceType }
                val rId = dto.entityId.ifEmpty { dto.resourceId }
                if (rType.isNotEmpty() && rId.isNotEmpty()) {
                    trackedWriter.writeGoalLink(
                        GoalLinkEntity(
                            goalId = dto.goalId,
                            resourceType = rType,
                            resourceId = rId,
                        ),
                    )
                }
            }

            // 13. Settings
            for ((k, v) in backup.settings) {
                trackedWriter.writeSetting(k, v)
            }

            // Record snapshot in version_history
            val history = VersionHistoryEntity(
                id = idGenerator.randomId(),
                entityType = "backup_import",
                entityId = "json",
                versionNumber = 1,
                snapshotData =
                "Imported ${importedTasks.size} tasks, " +
                    "${importedProjects.size} projects, ${importedAreas.size} areas",
                createdAt = clock.nowIso(),
            )
            db.versionHistoryDao().insert(history)

            AppResult.ok(
                ImportResult(
                    areasCount = importedAreas.size,
                    projectsCount = importedProjects.size,
                    tasksCount = importedTasks.size,
                    tagsCount = importedTags.size,
                    sectionsCount = importedSections.size,
                    remindersCount = importedReminders.size,
                    milestonesCount = importedMilestones.size,
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    // --- CSV Export (RFC 4180 Compliant) ---

    suspend fun exportToCsv(): AppResult<String> = withContext(ioDispatcher) {
        try {
            val tasks = db.taskDao().getAll()
            val taskTagsMap = db.taskTagDao().getAll().groupBy({ it.taskId }, { it.tagId })
            val tagsMap = db.tagDao().getAll().associate { it.id to it.name }

            val header = listOf(
                "id",
                "title",
                "notes",
                "priority",
                "is_starred",
                "is_completed",
                "completed_at",
                "due_date",
                "due_time",
                "bucket",
                "project_id",
                "area_id",
                "tags",
                "created_at",
                "updated_at",
            ).joinToString(",")

            val rows = tasks.map { task ->
                val tagNames = (taskTagsMap[task.id] ?: emptyList())
                    .mapNotNull { tagsMap[it] }
                    .joinToString(";")

                listOf(
                    task.id,
                    task.title,
                    task.notes.orEmpty(),
                    task.priority.toString(),
                    task.isStarred.toString(),
                    task.isCompleted.toString(),
                    task.completedAt.orEmpty(),
                    task.dueDate.orEmpty(),
                    task.dueTime.orEmpty(),
                    task.bucket.orEmpty(),
                    task.projectId.orEmpty(),
                    task.areaId.orEmpty(),
                    tagNames,
                    task.createdAt,
                    task.updatedAt,
                ).joinToString(",") { escapeCsv(it) }
            }

            val csvContent = buildString {
                append(header)
                append("\r\n")
                for (row in rows) {
                    append(row)
                    append("\r\n")
                }
            }

            // Record snapshot in version_history
            val history = VersionHistoryEntity(
                id = idGenerator.randomId(),
                entityType = "backup_export",
                entityId = "csv",
                versionNumber = 1,
                snapshotData = "Exported ${tasks.size} tasks to CSV",
                createdAt = clock.nowIso(),
            )
            db.versionHistoryDao().insert(history)

            AppResult.ok(csvContent)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    // --- ZIP Backup Archive Creation ---

    suspend fun createBackupZipBytes(): AppResult<ByteArray> = withContext(ioDispatcher) {
        try {
            val jsonResult = exportToJson()
            val jsonString = when (val jsonResult = exportToJson()) {
                is AppResult.Ok -> jsonResult.value
                is AppResult.Err -> return@withContext AppResult.err(jsonResult.error)
            }

            val tasksCount = db.taskDao().getAll().size
            val projectsCount = db.projectDao().getAll().size
            val areasCount = db.areaDao().getAll().size

            val metadata = BackupMetadata(
                backupVersion = 1,
                appVersion = "1.0.0",
                createdAt = clock.nowIso(),
                taskCount = tasksCount,
                projectCount = projectsCount,
                areaCount = areasCount,
            )
            val metadataString = json.encodeToString(metadata)

            val byteOut = ByteArrayOutputStream()
            ZipOutputStream(byteOut).use { zip ->
                // Entry 1: backup.json
                val backupEntry = ZipEntry("backup.json")
                zip.putNextEntry(backupEntry)
                zip.write(jsonString.toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                // Entry 2: metadata.json
                val metaEntry = ZipEntry("metadata.json")
                zip.putNextEntry(metaEntry)
                zip.write(metadataString.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }

            AppResult.ok(byteOut.toByteArray())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    // --- Daily Auto-Backup to SAF Tree URI ---

    suspend fun performAutoBackupToTreeUri(context: Context, treeUriString: String): AppResult<String> =
        withContext(ioDispatcher) {
            try {
                val treeUri = Uri.parse(treeUriString)
                val targetDir = DocumentFile.fromTreeUri(context, treeUri)
                    ?: return@withContext AppResult.err(
                        AppError.Validation("backup", "Target directory invalid: $treeUriString"),
                    )

                if (!targetDir.isDirectory || !targetDir.canWrite()) {
                    return@withContext AppResult.err(
                        AppError.Validation("backup", "Target directory is not writable"),
                    )
                }

                val zipBytes = when (val zipBytesResult = createBackupZipBytes()) {
                    is AppResult.Ok -> zipBytesResult.value
                    is AppResult.Err -> return@withContext AppResult.err(zipBytesResult.error)
                }

                val timestamp = clock.wallMs()
                val fileName = "os11_backup_$timestamp.zip"
                val newFile = targetDir.createFile("application/zip", fileName)
                    ?: return@withContext AppResult.err(
                        AppError.Unexpected(
                            IllegalStateException("Could not create backup file in SAF directory"),
                        ),
                    )

                context.contentResolver.openOutputStream(newFile.uri)?.use { out ->
                    out.write(zipBytes)
                    out.flush()
                } ?: return@withContext AppResult.err(
                    AppError.Unexpected(
                        IllegalStateException("Could not open output stream for SAF file"),
                    ),
                )

                // Prune backups older than 14 entries in this target folder
                pruneOldBackupsInTree(targetDir)

                // Update settings and version_history
                val nowIso = clock.nowIso()
                db.settingDao().upsert(SettingEntity("last_backup_timestamp", nowIso))

                val history = VersionHistoryEntity(
                    id = idGenerator.randomId(),
                    entityType = "auto_backup",
                    entityId = fileName,
                    versionNumber = 1,
                    snapshotData = "Auto-backup created: $fileName (${zipBytes.size} bytes)",
                    createdAt = nowIso,
                )
                db.versionHistoryDao().insert(history)

                AppResult.ok(fileName)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppResult.err(AppError.Unexpected(e))
            }
        }

    private fun pruneOldBackupsInTree(dir: DocumentFile) {
        try {
            val backupFiles = dir.listFiles()
                .filter { it.name?.startsWith("os11_backup_") == true && it.name?.endsWith(".zip") == true }
                .sortedByDescending { it.lastModified() }

            if (backupFiles.size > MAX_BACKUP_RETENTION) {
                val toPrune = backupFiles.drop(MAX_BACKUP_RETENTION)
                for (file in toPrune) {
                    file.delete()
                }
            }
        } catch (@Suppress("SwallowedException") e: Exception) {
            // Non-critical retention cleanup failure
        }
    }

    /**
     * RFC 4180 CSV field escape rule:
     * If the field contains comma, quote, or newline, enclose in double quotes
     * and double every quote inside.
     */
    private fun escapeCsv(value: String): String {
        val needsQuotes = value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')
        return if (needsQuotes) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    companion object {
        const val MAX_BACKUP_RETENTION = 14
    }
}
