package com.derekwinters.chores.data.network

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Behavior: error messages reuse client.js's per-status-code STATUS_CODE_FALLBACK text
 * (area: network).
 */
class HttpErrorMessagesTest {

    @Test
    fun knownStatusCodes_mapToWebClientFallbackText() {
        assertEquals("Invalid input — check your values", HttpErrorMessages.forStatusCode(400))
        assertEquals("Session expired, please log in", HttpErrorMessages.forStatusCode(401))
        assertEquals("You don't have permission to do that", HttpErrorMessages.forStatusCode(403))
        assertEquals("Not found", HttpErrorMessages.forStatusCode(404))
        assertEquals("Already exists", HttpErrorMessages.forStatusCode(409))
        assertEquals("Invalid input — check your values", HttpErrorMessages.forStatusCode(422))
        assertEquals("Service unavailable, please try again later", HttpErrorMessages.forStatusCode(503))
    }

    @Test
    fun unknownStatusCode_fallsBackToGenericMessage() {
        assertEquals("Something went wrong. Please try again.", HttpErrorMessages.forStatusCode(500))
        assertEquals("Something went wrong. Please try again.", HttpErrorMessages.forStatusCode(418))
    }
}
