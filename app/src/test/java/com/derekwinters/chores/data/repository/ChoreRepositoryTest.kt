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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Integration coverage for behaviors: "Chore list screen: GET /chores, render name/assignee-or-
 * Completer/points/state/next_due" and "Complete-chore action: POST /chores/{id}/complete, with
 * Completer-picker dialog when current_assignee == null" (area: ui, android, network), exercised
 * through the real interceptor/Retrofit stack against MockWebServer.
 */
class ChoreRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var credentialStore: FakeCredentialStore
    private lateinit var repository: ChoreRepository

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
        repository = ChoreRepository(buildTestApi(credentialStore, sessionManager))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun getChores_mapsResponseToDomainChores_withAuthHeader() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                [
                  {"id":1,"name":"Dishes","points":5,"state":"due","next_due":"2026-07-05","current_assignee":"alice","eligible_people":["alice","bob"]},
                  {"id":2,"name":"Trash","points":3,"state":"due","next_due":null,"current_assignee":null,"eligible_people":["alice","bob"]}
                ]
                """.trimIndent()
            )
        )

        val result = repository.getChores()

        assertTrue(result.isSuccess)
        val chores = result.getOrThrow()
        assertEquals(2, chores.size)
        assertEquals("Dishes", chores[0].name)
        assertEquals("alice", chores[0].currentAssignee)
        assertTrue(!chores[0].needsCompleterSelection)
        assertTrue(chores[1].needsCompleterSelection)

        val recorded = server.takeRequest()
        assertEquals("/v1/chores", recorded.path)
        assertEquals("Bearer tok123", recorded.getHeader("Authorization"))
    }

    @Test
    fun getChores_serverError_mapsToFallbackMessage() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))

        val result = repository.getChores()

        assertTrue(result.isFailure)
        assertEquals(
            "Something went wrong. Please try again.",
            (result.exceptionOrNull() as ApiException).message
        )
    }

    @Test
    fun completeChore_withoutCompleter_sendsNullCompletedBy() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"id":1,"name":"Dishes","points":5,"state":"done","next_due":null,"current_assignee":"alice","eligible_people":["alice","bob"]}"""
            )
        )

        val result = repository.completeChore(1)

        assertTrue(result.isSuccess)
        val recorded = server.takeRequest()
        assertEquals("/v1/chores/1/complete", recorded.path)
        assertEquals("""{"completed_by":null}""", recorded.body.readUtf8())
    }

    @Test
    fun completeChore_withCompleter_sendsChosenUsername() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"id":2,"name":"Trash","points":3,"state":"done","next_due":null,"current_assignee":"bob","eligible_people":["alice","bob"]}"""
            )
        )

        val result = repository.completeChore(2, completedBy = "bob")

        assertTrue(result.isSuccess)
        assertEquals("bob", result.getOrThrow().currentAssignee)
        val recorded = server.takeRequest()
        assertEquals("/v1/chores/2/complete", recorded.path)
        assertEquals("""{"completed_by":"bob"}""", recorded.body.readUtf8())
    }
}
