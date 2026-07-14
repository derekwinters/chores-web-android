package com.derekwinters.chores.di

import com.derekwinters.chores.data.auth.CredentialStore
import com.derekwinters.chores.data.auth.EncryptedCredentialStore
import com.derekwinters.chores.data.local.ConnectionStatusStore
import com.derekwinters.chores.data.local.NotificationSettingsStore
import com.derekwinters.chores.data.local.PostedNotificationsStore
import com.derekwinters.chores.data.local.SharedPrefsConnectionStatusStore
import com.derekwinters.chores.data.local.SharedPrefsNotificationSettingsStore
import com.derekwinters.chores.data.local.SharedPrefsVersionCheckCache
import com.derekwinters.chores.data.local.VersionCheckCache
import com.derekwinters.chores.notifications.NotificationRescheduler
import com.derekwinters.chores.notifications.WorkManagerNotificationRescheduler
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

    /** Issue #35: caches the client-side GitHub-releases version check. */
    @Binds
    @Singleton
    abstract fun bindVersionCheckCache(impl: SharedPrefsVersionCheckCache): VersionCheckCache

    /** Issue #43/#44: last-successful-backend-contact timestamp for the offline alert. */
    @Binds
    @Singleton
    abstract fun bindConnectionStatusStore(impl: SharedPrefsConnectionStatusStore): ConnectionStatusStore

    /**
     * Issue #43: once-per-item posted-ids record. Bound to the same
     * [SharedPrefsConnectionStatusStore] singleton as [bindConnectionStatusStore], so both share
     * one prefs file.
     */
    @Binds
    @Singleton
    abstract fun bindPostedNotificationsStore(impl: SharedPrefsConnectionStatusStore): PostedNotificationsStore

    /**
     * Issue #44: device-local notification settings (poll interval, offline-alert enabled +
     * threshold). Its own prefs file, distinct from the connection-status store above.
     */
    @Binds
    @Singleton
    abstract fun bindNotificationSettingsStore(impl: SharedPrefsNotificationSettingsStore): NotificationSettingsStore

    /** Issue #44: re-arms the poll worker when the user changes the interval. */
    @Binds
    @Singleton
    abstract fun bindNotificationRescheduler(impl: WorkManagerNotificationRescheduler): NotificationRescheduler
}
