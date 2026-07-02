package com.derekwinters.chores.data.network

/**
 * Per-status-code fallback error text, ported from chores-web's `STATUS_CODE_FALLBACK` map
 * (frontend/src/api/client.js) so error messages read the same on both clients (issue #5
 * grilling: "Error messages reuse the same per-status-code fallback text as client.js's
 * STATUS_CODE_FALLBACK").
 *
 * Used when the server response has no parseable `detail` field (FastAPI's default error
 * body shape, `{"detail": "..."}`) to fall back on.
 */
object HttpErrorMessages {

    private val STATUS_CODE_FALLBACK = mapOf(
        400 to "Invalid input — check your values",
        401 to "Session expired, please log in",
        403 to "You don't have permission to do that",
        404 to "Not found",
        409 to "Already exists",
        422 to "Invalid input — check your values"
    )

    private const val DEFAULT_FALLBACK = "Something went wrong. Please try again."

    const val NETWORK_ERROR = "Unable to reach the server. Check your connection and server URL."

    fun forStatusCode(code: Int): String = STATUS_CODE_FALLBACK[code] ?: when (code) {
        503 -> "Service unavailable, please try again later"
        else -> DEFAULT_FALLBACK
    }
}
