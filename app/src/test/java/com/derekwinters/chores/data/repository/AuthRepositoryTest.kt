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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Integration coverage for behavior: "Login screen ... calls POST /auth/login, persists token +
 * URL", exercised through the real BaseUrlInterceptor/AuthInterceptor/Retrofit stack against
 * MockWebServer (area: android, ui, network).
 */
class AuthRepositoryTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun login_success_persistsTokenAndUrl_andAuthenticatesSession() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"access_token":"tok123","token_type":"bearer","user":{"username":"alice","is_admin":false}}"""
            )
        )
        val credentialStore = FakeCredentialStore()
        val sessionManager = SessionManager(credentialStore)
        val repository = AuthRepository(buildTestApi(credentialStore, sessionManager), credentialStore, sessionManager)
        val serverUrl = server.url("/").toString()

        val result = repository.login(serverUrl, "alice", "secret")

        assertTrue(result.isSuccess)
        assertEquals("tok123", credentialStore.getToken())
        assertEquals("bearer", credentialStore.getTokenType())
        assertEquals(serverUrl, credentialStore.getServerUrl())
        assertTrue(sessionManager.isAuthenticated.value)

        val recorded = server.takeRequest()
        assertEquals("/v1/auth/login", recorded.path)
        assertTrue(recorded.body.readUtf8().contains("\"username\":\"alice\""))
    }

    @Test
    fun login_invalidCredentials_surfacesBackendDetailMessage_andDoesNotAuthenticate() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(401).setBody("""{"detail":"Invalid username or password"}""")
        )
        val credentialStore = FakeCredentialStore()
        val sessionManager = SessionManager(credentialStore)
        val repository = AuthRepository(buildTestApi(credentialStore, sessionManager), credentialStore, sessionManager)

        val result = repository.login(server.url("/").toString(), "alice", "wrong")

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull() as ApiException
        assertEquals(401, error.statusCode)
        assertEquals("Invalid username or password", error.message)
        assertNull(credentialStore.getToken())
        assertFalse(sessionManager.isAuthenticated.value)
    }
}
