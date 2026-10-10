package com.os11.app.di

import com.os11.app.platform.security.KeystoreSecretStore
import com.os11.app.platform.security.SecretStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {

    @Binds
    @Singleton
    abstract fun bindSecretStore(impl: KeystoreSecretStore): SecretStore
}
