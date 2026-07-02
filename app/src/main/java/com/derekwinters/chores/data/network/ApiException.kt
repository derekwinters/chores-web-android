package com.derekwinters.chores.data.network

/**
 * Thrown by repositories for any failed API call. [message] is either the backend's `detail`
 * text or the [HttpErrorMessages] fallback for [statusCode] (issue #5 grilling: reuse
 * client.js's per-status-code fallback text). [statusCode] is -1 for network-level failures
 * (no response received at all).
 */
class ApiException(
    val statusCode: Int,
    override val message: String
) : Exception(message)
