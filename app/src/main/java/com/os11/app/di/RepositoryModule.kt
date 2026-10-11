package com.os11.app.di

import com.os11.app.data.db.OS11Database
import com.os11.app.data.repository.AreaRepository
import com.os11.app.data.repository.AreaRepositoryImpl
import com.os11.app.data.repository.AttachmentRepository
import com.os11.app.data.repository.AttachmentRepositoryImpl
import com.os11.app.data.repository.CommentRepository
import com.os11.app.data.repository.CommentRepositoryImpl
import com.os11.app.data.repository.CountsRepository
import com.os11.app.data.repository.CountsRepositoryImpl
import com.os11.app.data.repository.DashboardRepository
import com.os11.app.data.repository.DashboardRepositoryImpl
import com.os11.app.data.repository.DependencyRepository
import com.os11.app.data.repository.DependencyRepositoryImpl
import com.os11.app.data.repository.GoalRepository
import com.os11.app.data.repository.GoalRepositoryImpl
import com.os11.app.data.repository.HistoryRepository
import com.os11.app.data.repository.HistoryRepositoryImpl
import com.os11.app.data.repository.MilestoneRepository
import com.os11.app.data.repository.MilestoneRepositoryImpl
import com.os11.app.data.repository.ModuleRepository
import com.os11.app.data.repository.ModuleRepositoryImpl
import com.os11.app.data.repository.NotificationRepository
import com.os11.app.data.repository.NotificationRepositoryImpl
import com.os11.app.data.repository.PomodoroRepository
import com.os11.app.data.repository.PomodoroRepositoryImpl
import com.os11.app.data.repository.ProjectRepository
import com.os11.app.data.repository.ProjectRepositoryImpl
import com.os11.app.data.repository.ReminderRepository
import com.os11.app.data.repository.ReminderRepositoryImpl
import com.os11.app.data.repository.SearchRepository
import com.os11.app.data.repository.SearchRepositoryImpl
import com.os11.app.data.repository.SectionRepository
import com.os11.app.data.repository.SectionRepositoryImpl
import com.os11.app.data.repository.SettingsRepository
import com.os11.app.data.repository.SettingsRepositoryImpl
import com.os11.app.data.repository.TagRepository
import com.os11.app.data.repository.TagRepositoryImpl
import com.os11.app.data.repository.TaskRepository
import com.os11.app.data.repository.TaskRepositoryImpl
import com.os11.app.data.repository.WorkspaceRepository
import com.os11.app.data.repository.WorkspaceRepositoryImpl
import com.os11.app.data.sync.TrackedWriter
import com.os11.domain.time.Clock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
@Suppress("TooManyFunctions")
object RepositoryModule {

    @Provides
    @Singleton
    fun provideTaskRepository(db: OS11Database, trackedWriter: TrackedWriter, clock: Clock): TaskRepository =
        TaskRepositoryImpl(db, trackedWriter, clock)

    @Provides
    @Singleton
    fun provideWorkspaceRepository(db: OS11Database, trackedWriter: TrackedWriter): WorkspaceRepository =
        WorkspaceRepositoryImpl(db, trackedWriter)

    @Provides
    @Singleton
    fun provideAreaRepository(db: OS11Database, trackedWriter: TrackedWriter): AreaRepository =
        AreaRepositoryImpl(db, trackedWriter)

    @Provides
    @Singleton
    fun provideProjectRepository(db: OS11Database, trackedWriter: TrackedWriter): ProjectRepository =
        ProjectRepositoryImpl(db, trackedWriter)

    @Provides
    @Singleton
    fun provideSectionRepository(db: OS11Database, trackedWriter: TrackedWriter): SectionRepository =
        SectionRepositoryImpl(db, trackedWriter)

    @Provides
    @Singleton
    fun provideTagRepository(db: OS11Database, trackedWriter: TrackedWriter): TagRepository =
        TagRepositoryImpl(db, trackedWriter)

    @Provides
    @Singleton
    fun provideDependencyRepository(db: OS11Database, trackedWriter: TrackedWriter): DependencyRepository =
        DependencyRepositoryImpl(db, trackedWriter)

    @Provides
    @Singleton
    fun provideMilestoneRepository(db: OS11Database, trackedWriter: TrackedWriter): MilestoneRepository =
        MilestoneRepositoryImpl(db, trackedWriter)

    @Provides
    @Singleton
    fun provideReminderRepository(db: OS11Database, trackedWriter: TrackedWriter): ReminderRepository =
        ReminderRepositoryImpl(db, trackedWriter)

    @Provides
    @Singleton
    fun provideAttachmentRepository(db: OS11Database, trackedWriter: TrackedWriter): AttachmentRepository =
        AttachmentRepositoryImpl(db, trackedWriter)

    @Provides
    @Singleton
    fun provideCommentRepository(db: OS11Database, trackedWriter: TrackedWriter): CommentRepository =
        CommentRepositoryImpl(db, trackedWriter)

    @Provides
    @Singleton
    fun providePomodoroRepository(db: OS11Database, trackedWriter: TrackedWriter): PomodoroRepository =
        PomodoroRepositoryImpl(db, trackedWriter)

    @Provides
    @Singleton
    fun provideGoalRepository(db: OS11Database, trackedWriter: TrackedWriter): GoalRepository =
        GoalRepositoryImpl(db, trackedWriter)

    @Provides
    @Singleton
    fun provideSettingsRepository(db: OS11Database, trackedWriter: TrackedWriter): SettingsRepository =
        SettingsRepositoryImpl(db, trackedWriter)

    @Provides
    @Singleton
    fun provideModuleRepository(db: OS11Database, trackedWriter: TrackedWriter): ModuleRepository =
        ModuleRepositoryImpl(db, trackedWriter)

    @Provides
    @Singleton
    fun provideNotificationRepository(db: OS11Database, trackedWriter: TrackedWriter): NotificationRepository =
        NotificationRepositoryImpl(db, trackedWriter)

    @Provides
    @Singleton
    fun provideHistoryRepository(db: OS11Database, trackedWriter: TrackedWriter): HistoryRepository =
        HistoryRepositoryImpl(db, trackedWriter)

    @Provides
    @Singleton
    fun provideSearchRepository(db: OS11Database): SearchRepository = SearchRepositoryImpl(db)

    @Provides
    @Singleton
    fun provideCountsRepository(db: OS11Database): CountsRepository = CountsRepositoryImpl(db)

    @Provides
    @Singleton
    fun provideDashboardRepository(db: OS11Database): DashboardRepository = DashboardRepositoryImpl(db)
}
