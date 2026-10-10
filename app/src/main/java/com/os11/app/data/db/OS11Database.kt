package com.os11.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.os11.app.data.db.dao.AreaDao
import com.os11.app.data.db.dao.AttachmentDao
import com.os11.app.data.db.dao.CollaborationMemberDao
import com.os11.app.data.db.dao.CommentDao
import com.os11.app.data.db.dao.CommentReactionDao
import com.os11.app.data.db.dao.CountsDao
import com.os11.app.data.db.dao.DeviceDao
import com.os11.app.data.db.dao.GoalDao
import com.os11.app.data.db.dao.GoalLinkDao
import com.os11.app.data.db.dao.LocalIdentityDao
import com.os11.app.data.db.dao.MilestoneDao
import com.os11.app.data.db.dao.ModuleDao
import com.os11.app.data.db.dao.NotificationHistoryDao
import com.os11.app.data.db.dao.PomodoroSessionDao
import com.os11.app.data.db.dao.ProjectDao
import com.os11.app.data.db.dao.ReminderDao
import com.os11.app.data.db.dao.ReminderDeviceStateDao
import com.os11.app.data.db.dao.SearchDao
import com.os11.app.data.db.dao.SectionDao
import com.os11.app.data.db.dao.SettingDao
import com.os11.app.data.db.dao.SyncConflictLogDao
import com.os11.app.data.db.dao.SyncFieldClockDao
import com.os11.app.data.db.dao.SyncLocalStateDao
import com.os11.app.data.db.dao.SyncPeerStateDao
import com.os11.app.data.db.dao.SyncPendingDao
import com.os11.app.data.db.dao.SyncQueueDao
import com.os11.app.data.db.dao.SyncTombstoneDao
import com.os11.app.data.db.dao.TagDao
import com.os11.app.data.db.dao.TaskDao
import com.os11.app.data.db.dao.TaskDependencyDao
import com.os11.app.data.db.dao.TaskHistoryDao
import com.os11.app.data.db.dao.TaskTagDao
import com.os11.app.data.db.dao.UserDao
import com.os11.app.data.db.dao.WorkspaceDao
import com.os11.app.data.db.entities.AreaEntity
import com.os11.app.data.db.entities.AttachmentEntity
import com.os11.app.data.db.entities.CollaborationMemberEntity
import com.os11.app.data.db.entities.CommentEntity
import com.os11.app.data.db.entities.CommentReactionEntity
import com.os11.app.data.db.entities.DeviceEntity
import com.os11.app.data.db.entities.GoalEntity
import com.os11.app.data.db.entities.GoalLinkEntity
import com.os11.app.data.db.entities.LocalIdentityEntity
import com.os11.app.data.db.entities.MilestoneEntity
import com.os11.app.data.db.entities.ModuleEntity
import com.os11.app.data.db.entities.NotificationHistoryEntity
import com.os11.app.data.db.entities.PomodoroSessionEntity
import com.os11.app.data.db.entities.ProjectEntity
import com.os11.app.data.db.entities.ReminderDeviceStateEntity
import com.os11.app.data.db.entities.ReminderEntity
import com.os11.app.data.db.entities.SectionEntity
import com.os11.app.data.db.entities.SettingEntity
import com.os11.app.data.db.entities.SyncConflictLogEntity
import com.os11.app.data.db.entities.SyncFieldClockEntity
import com.os11.app.data.db.entities.SyncLocalStateEntity
import com.os11.app.data.db.entities.SyncPeerStateEntity
import com.os11.app.data.db.entities.SyncPendingEntity
import com.os11.app.data.db.entities.SyncQueueEntity
import com.os11.app.data.db.entities.SyncTombstoneEntity
import com.os11.app.data.db.entities.TagEntity
import com.os11.app.data.db.entities.TaskDependencyEntity
import com.os11.app.data.db.entities.TaskEntity
import com.os11.app.data.db.entities.TaskHistoryEntity
import com.os11.app.data.db.entities.TaskTagEntity
import com.os11.app.data.db.entities.UserEntity
import com.os11.app.data.db.entities.WorkspaceEntity

@Database(
    entities = [
        LocalIdentityEntity::class,
        WorkspaceEntity::class,
        AreaEntity::class,
        ProjectEntity::class,
        SectionEntity::class,
        TaskEntity::class,
        TaskDependencyEntity::class,
        MilestoneEntity::class,
        TagEntity::class,
        TaskTagEntity::class,
        ReminderEntity::class,
        ReminderDeviceStateEntity::class,
        AttachmentEntity::class,
        CommentEntity::class,
        CommentReactionEntity::class,
        PomodoroSessionEntity::class,
        GoalEntity::class,
        GoalLinkEntity::class,
        NotificationHistoryEntity::class,
        TaskHistoryEntity::class,
        SettingEntity::class,
        ModuleEntity::class,
        UserEntity::class,
        DeviceEntity::class,
        CollaborationMemberEntity::class,
        SyncQueueEntity::class,
        SyncFieldClockEntity::class,
        SyncTombstoneEntity::class,
        SyncLocalStateEntity::class,
        SyncPeerStateEntity::class,
        SyncPendingEntity::class,
        SyncConflictLogEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@Suppress("TooManyFunctions")
internal abstract class OS11Database : RoomDatabase() {

    abstract fun localIdentityDao(): LocalIdentityDao
    abstract fun workspaceDao(): WorkspaceDao
    abstract fun areaDao(): AreaDao
    abstract fun projectDao(): ProjectDao
    abstract fun sectionDao(): SectionDao
    abstract fun taskDao(): TaskDao
    abstract fun taskDependencyDao(): TaskDependencyDao
    abstract fun milestoneDao(): MilestoneDao
    abstract fun tagDao(): TagDao
    abstract fun taskTagDao(): TaskTagDao
    abstract fun reminderDao(): ReminderDao
    abstract fun reminderDeviceStateDao(): ReminderDeviceStateDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun commentDao(): CommentDao
    abstract fun commentReactionDao(): CommentReactionDao
    abstract fun pomodoroSessionDao(): PomodoroSessionDao
    abstract fun goalDao(): GoalDao
    abstract fun goalLinkDao(): GoalLinkDao
    abstract fun notificationHistoryDao(): NotificationHistoryDao
    abstract fun taskHistoryDao(): TaskHistoryDao
    abstract fun settingDao(): SettingDao
    abstract fun moduleDao(): ModuleDao
    abstract fun userDao(): UserDao
    abstract fun deviceDao(): DeviceDao
    abstract fun collaborationMemberDao(): CollaborationMemberDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun syncFieldClockDao(): SyncFieldClockDao
    abstract fun syncTombstoneDao(): SyncTombstoneDao
    abstract fun syncLocalStateDao(): SyncLocalStateDao
    abstract fun syncPeerStateDao(): SyncPeerStateDao
    abstract fun syncPendingDao(): SyncPendingDao
    abstract fun syncConflictLogDao(): SyncConflictLogDao
    abstract fun countsDao(): CountsDao
    abstract fun searchDao(): SearchDao

    companion object {
        const val DATABASE_NAME = "os11.db"

        fun buildDatabase(context: Context, databaseName: String = DATABASE_NAME): OS11Database {
            val builder = Room.databaseBuilder(context, OS11Database::class.java, databaseName)
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                .setDriver(BundledSQLiteDriver())
                .addCallback(OS11DatabaseCallback())
            return builder.build()
        }

        fun buildInMemory(context: Context, useBundledDriver: Boolean = false): OS11Database {
            val builder = Room.inMemoryDatabaseBuilder(context, OS11Database::class.java)
                .addCallback(OS11DatabaseCallback())
            if (useBundledDriver) {
                builder.setDriver(BundledSQLiteDriver())
            }
            return builder.build()
        }
    }
}
