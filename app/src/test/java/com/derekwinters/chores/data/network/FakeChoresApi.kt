package com.derekwinters.chores.data.network

import com.derekwinters.chores.data.network.dto.ChoreDto
import com.derekwinters.chores.data.network.dto.CompleteChoreRequestDto
import com.derekwinters.chores.data.network.dto.LoginRequestDto
import com.derekwinters.chores.data.network.dto.LoginResponseDto
import com.derekwinters.chores.data.network.dto.UserInfoDto

/**
 * In-memory [ChoresApi] test double. Its suspend functions never perform real I/O (no real
 * thread hop), so ViewModel tests driving it through `viewModelScope` on a shared
 * `StandardTestDispatcher` (see MainDispatcherRule) stay fully deterministic under
 * `advanceUntilIdle()` — unlike a real MockWebServer-backed call, whose completion crosses a
 * real background thread. MockWebServer is used instead for the repository/interceptor-level
 * tests, where the suspend call is awaited directly rather than through a fire-and-forget
 * `launch`.
 */
class FakeChoresApi(
    private val loginResult: LoginResponseDto? = null,
    private val loginError: Throwable? = null,
    private val choresResult: List<ChoreDto> = emptyList(),
    private val choresError: Throwable? = null,
    private val completeResult: ChoreDto? = null,
    private val completeError: Throwable? = null
) : ChoresApi {

    var lastCompleteChoreId: Int? = null
        private set
    var lastCompleteRequest: CompleteChoreRequestDto? = null
        private set

    override suspend fun login(request: LoginRequestDto): LoginResponseDto {
        loginError?.let { throw it }
        return loginResult ?: error("FakeChoresApi.loginResult not configured")
    }

    override suspend fun getCurrentUser(): UserInfoDto = UserInfoDto("", false)

    override suspend fun getChores(): List<ChoreDto> {
        choresError?.let { throw it }
        return choresResult
    }

    override suspend fun completeChore(choreId: Int, request: CompleteChoreRequestDto): ChoreDto {
        lastCompleteChoreId = choreId
        lastCompleteRequest = request
        completeError?.let { throw it }
        return completeResult ?: error("FakeChoresApi.completeResult not configured")
    }
}
