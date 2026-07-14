package com.derekwinters.chores.data.repository

import com.derekwinters.chores.data.model.Notification
import com.derekwinters.chores.data.model.toDomain
import com.derekwinters.chores.data.network.ChoresApi
import com.derekwinters.chores.data.network.dto.NotificationPreferencesDto
import com.derekwinters.chores.data.network.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Issue #43: wraps the two notification endpoints (`GET /v1/notifications`,
 * `POST /v1/notifications/{id}/ack`) via [safeApiCall], following the same pattern as
 * [ChoreRepository] / [LogRepository]. Consumed by
 * [com.derekwinters.chores.notifications.NotificationPollWorker] (list) and the notification-tap
 * handler in `MainActivity` (ack).
 */
@Singleton
class NotificationRepository @Inject constructor(
    private val api: ChoresApi
) {
    /**
     * Fetches the caller's notifications and maps them to the domain [Notification] model. The
     * poll worker calls this with the defaults ([includeDismissed] = false) so dismissed rows are
     * excluded server-side; it still defensively skips any acked/dismissed item before posting.
     *
     * @param since only notifications created strictly after this ISO-8601 instant (null = all).
     */
    suspend fun getNotifications(
        since: String? = null,
        includeDismissed: Boolean = false
    ): Result<List<Notification>> =
        safeApiCall { api.getNotifications(since, includeDismissed) }
            .map { dtos -> dtos.map { it.toDomain() } }

    /**
     * Acknowledges one notification (notification-tap user action). Idempotent server-side, so a
     * double-tap or a replayed intent is harmless.
     */
    suspend fun acknowledge(notificationId: Int): Result<Unit> =
        safeApiCall { api.ackNotification(notificationId) }

    /**
     * Issue #44: the caller's per-type notification preferences (`GET /v1/notifications/
     * preferences`). The map key is a notification type (v1: `"chore_due"`), the value its
     * enabled state; every known type is present in the response.
     */
    suspend fun getPreferences(): Result<NotificationPreferencesDto> =
        safeApiCall { api.getNotificationPreferences() }

    /**
     * Issue #44: persist the caller's per-type preferences (`PUT /v1/notifications/preferences`).
     * Returns the server's resulting map (unknown keys dropped, every known type present).
     */
    suspend fun updatePreferences(
        preferences: NotificationPreferencesDto
    ): Result<NotificationPreferencesDto> =
        safeApiCall { api.updateNotificationPreferences(preferences) }
}
