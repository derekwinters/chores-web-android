package com.derekwinters.chores.notification

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNotificationManager

/**
 * Behavior: Request POST_NOTIFICATIONS runtime permission, fire local notification on button
 * tap (area: android).
 *
 * NotificationSender covers the "fire local notification" half of this behavior: given
 * permission is granted, posting a notification via NotificationManager. The runtime permission
 * *request* flow (launcher + system dialog) lives in MainActivity and requires instrumentation
 * to exercise end-to-end; it is not covered by this JVM unit test (see deviations in PR notes).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class NotificationSenderTest {

    private lateinit var application: Application
    private lateinit var notificationManager: NotificationManager
    private lateinit var shadowNotificationManager: ShadowNotificationManager

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        notificationManager = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        shadowNotificationManager = shadowOf(notificationManager)
    }

    @Test
    fun sendTestNotification_whenPermissionGranted_postsNotification() {
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        NotificationSender(application).sendTestNotification()

        val activeNotifications = shadowNotificationManager.allNotifications
        assertEquals(1, activeNotifications.size)
    }

    @Test
    fun sendTestNotification_whenPermissionDenied_doesNotPostNotification() {
        shadowOf(application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

        NotificationSender(application).sendTestNotification()

        assertTrue(shadowNotificationManager.allNotifications.isEmpty())
    }
}
