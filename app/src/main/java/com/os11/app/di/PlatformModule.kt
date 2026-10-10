package com.os11.app.di

import com.os11.app.platform.notifications.NotificationPlatformDispatcher
import com.os11.app.platform.notifications.SystemNotificationPlatformDispatcher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PlatformModule {

    @Binds
    @Singleton
    abstract fun bindNotificationPlatformDispatcher(
        impl: SystemNotificationPlatformDispatcher,
    ): NotificationPlatformDispatcher
}
