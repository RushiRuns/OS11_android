package com.os11.app.di

import com.os11.domain.id.DefaultIdGenerator
import com.os11.domain.id.IdGenerator
import com.os11.domain.time.Clock
import com.os11.domain.time.SystemClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DomainModule {

    @Provides
    @Singleton
    fun provideClock(): Clock = SystemClock()

    @Provides
    @Singleton
    fun provideIdGenerator(): IdGenerator = DefaultIdGenerator()
}
