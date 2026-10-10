package com.os11.app.di

import android.content.Context
import com.os11.app.data.db.OS11Database
import com.os11.app.data.sync.TrackedWriter
import com.os11.domain.id.IdGenerator
import com.os11.domain.logic.sync.DefaultHlcClock
import com.os11.domain.time.Clock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): OS11Database = OS11Database.buildDatabase(context)

    @Provides
    @Singleton
    fun provideHlcClock(clock: Clock): DefaultHlcClock = DefaultHlcClock(clock, "local-device")

    @Provides
    @Singleton
    fun provideTrackedWriter(
        db: OS11Database,
        hlcClock: DefaultHlcClock,
        clock: Clock,
        idGenerator: IdGenerator,
    ): TrackedWriter = TrackedWriter(db, hlcClock, clock, idGenerator)
}
