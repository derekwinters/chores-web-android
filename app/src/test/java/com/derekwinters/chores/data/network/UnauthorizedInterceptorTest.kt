package com.derekwinters.chores.data.network

import com.derekwinters.chores.data.auth.FakeCredentialStore
import com.derekwinters.chores.data.auth.SessionManager
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Behavior: "Global 401 handling: clear token, navigate to Login" (area: network, android).
 * Navigation itself is ChoresApp's job (see ChoresAppTest); this covers the interceptor's half:
 * clearing the token and flipping SessionManager.isAuthenticated.
 */
class UnauthorizedInterceptorTest {

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
    fun on401_clearsTokenAndFlipsIsAuthenticatedToFalse() {
        server.enqueue(MockResponse().setResponseCode(401))
        val credentialStore = FakeCredentialStore(token = "abc123", tokenType = "Bearer")
        val sessionManager = SessionManager(credentialStore)
        val client = OkHttpClient.Builder().addInterceptor(UnauthorizedInterceptor(sessionManager)).build()

        client.newCall(Request.Builder().url(server.url("/v1/chores")).build()).execute()

        assertNull(credentialStore.getToken())
        assertTrue(!sessionManager.isAuthenticated.value)
    }

    @Test
    fun onSuccess_leavesSessionUntouched() {
        server.enqueue(MockResponse().setResponseCode(200))
        val credentialStore = FakeCredentialStore(token = "abc123", tokenType = "Bearer")
        val sessionManager = SessionManager(credentialStore)
        val client = OkHttpClient.Builder().addInterceptor(UnauthorizedInterceptor(sessionManager)).build()

        client.newCall(Request.Builder().url(server.url("/v1/chores")).build()).execute()

        assertTrue(sessionManager.isAuthenticated.value)
    }
}
