package com.derekwinters.chores.data.network

import java.io.IOException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException

@Serializable
private data class ErrorDetailDto(val detail: String? = null)

private val errorJson = Json { ignoreUnknownKeys = true }

/**
 * Runs [block] and converts any failure into an [ApiException], preferring the backend's
 * `{"detail": "..."}` body (FastAPI's default error shape) and falling back to
 * [HttpErrorMessages] by status code, matching client.js's error-handling strategy.
 */
suspend fun <T> safeApiCall(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: HttpException) {
    val detail = e.response()?.errorBody()?.string()
        ?.let { runCatching { errorJson.decodeFromString<ErrorDetailDto>(it) }.getOrNull() }
        ?.detail
    Result.failure(ApiException(e.code(), detail ?: HttpErrorMessages.forStatusCode(e.code())))
} catch (e: IOException) {
    Result.failure(ApiException(-1, HttpErrorMessages.NETWORK_ERROR))
}
