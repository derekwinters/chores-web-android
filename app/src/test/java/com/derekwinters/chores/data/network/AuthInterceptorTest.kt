package com.derekwinters.chores.data.network

import com.derekwinters.chores.data.auth.FakeCredentialStore
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Behavior: "OkHttp interceptor attaches Authorization: Bearer <token> header"
 * (area: network).
 */
class AuthInterceptorTest {

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
    fun attachesAuthorizationHeader_whenTokenPresent() {
        server.enqueue(MockResponse().setResponseCode(200))
        val credentialStore = FakeCredentialStore(token = "abc123", tokenType = "Bearer")
        val client = OkHttpClient.Builder().addInterceptor(AuthInterceptor(credentialStore)).build()

        client.newCall(Request.Builder().url(server.url("/v1/chores")).build()).execute()

        assertEquals("Bearer abc123", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun omitsAuthorizationHeader_whenNoTokenStored() {
        server.enqueue(MockResponse().setResponseCode(200))
        val credentialStore = FakeCredentialStore(token = null)
        val client = OkHttpClient.Builder().addInterceptor(AuthInterceptor(credentialStore)).build()

        client.newCall(Request.Builder().url(server.url("/v1/auth/login")).build()).execute()

        assertNull(server.takeRequest().getHeader("Authorization"))
    }
}
