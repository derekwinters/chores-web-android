package com.derekwinters.chores.data.repository

import com.derekwinters.chores.data.auth.FakeCredentialStore
import com.derekwinters.chores.data.auth.SessionManager
import com.derekwinters.chores.data.network.ApiException
import com.derekwinters.chores.data.network.buildTestApi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Integration coverage for issue #43's notification endpoints (`GET /v1/notifications`,
 * `POST /v1/notifications/{id}/ack`), exercised through the real interceptor/Retrofit stack
 * against MockWebServer — the same style as [ChoreRepositoryTest].
 */
class NotificationRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var credentialStore: FakeCredentialStore
    private lateinit var repository: NotificationRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        credentialStore = FakeCredentialStore(
            serverUrl = server.url("/").toString(),
            token = "tok123",
            tokenType = "Bearer"
        )
        val sessionManager = SessionManager(credentialStore)
        repository = NotificationRepository(buildTestApi(credentialStore, sessionManager))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun getNotifications_mapsResponseToDomain_withDefaultQueryAndAuthHeader() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                [
                  {"id":1,"person_id":7,"type":"chore_due","chore_id":42,"title":"Dishes due","body":"Please do the dishes","created_at":"2026-07-14T08:00:00Z","delivered_at":"2026-07-14T09:00:00Z","acknowledged_at":null,"dismissed_at":null},
                  {"id":2,"person_id":7,"type":"chore_due","chore_id":null,"title":"Trash due","body":"Take out the trash","created_at":"2026-07-14T08:05:00Z"}
                ]
                """.trimIndent()
            )
        )

        val result = repository.getNotifications()

        assertTrue(result.isSuccess)
        val notifications = result.getOrThrow()
        assertEquals(2, notifications.size)
        assertEquals(1, notifications[0].id)
        assertEquals(7, notifications[0].personId)
        assertEquals("chore_due", notifications[0].type)
        assertEquals(42, notifications[0].choreId)
        assertEquals("Dishes due", notifications[0].title)
        assertEquals("2026-07-14T09:00:00Z", notifications[0].deliveredAt)
        assertNull(notifications[1].choreId)
        assertNull(notifications[1].deliveredAt)
        assertTrue(notifications[1].isActionable)

        val recorded = server.takeRequest()
        // Default query params: since omitted (null), include_dismissed=false.
        assertEquals("/v1/notifications?include_dismissed=false", recorded.path)
        assertEquals("Bearer tok123", recorded.getHeader("Authorization"))
    }

    @Test
    fun getNotifications_serverError_mapsToFallbackMessage() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))

        val result = repository.getNotifications()

        assertTrue(result.isFailure)
        assertEquals(
            "Something went wrong. Please try again.",
            (result.exceptionOrNull() as ApiException).message
        )
    }

    @Test
    fun acknowledge_postsToAckPath() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"id":5,"person_id":7,"type":"chore_due","chore_id":42,"title":"Dishes due","body":"Please do the dishes","created_at":"2026-07-14T08:00:00Z","acknowledged_at":"2026-07-14T10:00:00Z"}"""
            )
        )

        val result = repository.acknowledge(5)

        assertTrue(result.isSuccess)
        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/v1/notifications/5/ack", recorded.path)
        assertEquals("Bearer tok123", recorded.getHeader("Authorization"))
    }

    @Test
    fun acknowledge_serverError_mapsToFailure() = runTest {
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"detail":"Notification not found"}"""))

        val result = repository.acknowledge(999)

        assertTrue(result.isFailure)
        assertEquals(
            "Notification not found",
            (result.exceptionOrNull() as ApiException).message
        )
    }
}
