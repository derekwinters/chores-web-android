package com.derekwinters.chores.di

import com.derekwinters.chores.data.auth.CredentialStore
import com.derekwinters.chores.data.auth.EncryptedCredentialStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Issue #5 behavior: "Hilt modules for OkHttp/Retrofit/EncryptedSharedPreferences singletons".
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class StorageModule {

    @Binds
    @Singleton
    abstract fun bindCredentialStore(impl: EncryptedCredentialStore): CredentialStore
}
