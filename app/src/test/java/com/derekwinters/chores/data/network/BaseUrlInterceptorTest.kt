package com.derekwinters.chores.data.network

import com.derekwinters.chores.data.auth.FakeCredentialStore
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Behavior: "OkHttp interceptor rewrites request URL from stored server-URL preference"
 * (area: network).
 */
class BaseUrlInterceptorTest {

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
    fun rewritesSchemeHostAndPort_fromStoredServerUrl() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))
        val credentialStore = FakeCredentialStore(serverUrl = server.url("/").toString())
        val client = OkHttpClient.Builder()
            .addInterceptor(BaseUrlInterceptor(credentialStore))
            .build()

        val response = client.newCall(
            Request.Builder().url("http://placeholder.invalid/v1/chores").build()
        ).execute()

        assertEquals(200, response.code)
        assertEquals("/v1/chores", server.takeRequest().path)
    }

    @Test
    fun leavesRequestUnchanged_whenNoServerUrlStored() {
        server.enqueue(MockResponse().setResponseCode(200))
        val credentialStore = FakeCredentialStore(serverUrl = null)
        val client = OkHttpClient.Builder()
            .addInterceptor(BaseUrlInterceptor(credentialStore))
            .build()

        // Point directly at the test server since there's no stored URL to rewrite to.
        val response = client.newCall(Request.Builder().url(server.url("/v1/chores")).build()).execute()

        assertEquals(200, response.code)
        assertEquals("/v1/chores", server.takeRequest().path)
    }
}
