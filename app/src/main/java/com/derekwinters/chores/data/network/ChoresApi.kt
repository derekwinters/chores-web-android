package com.derekwinters.chores.data.network

import com.derekwinters.chores.data.network.dto.ChoreDto
import com.derekwinters.chores.data.network.dto.CompleteChoreRequestDto
import com.derekwinters.chores.data.network.dto.LoginRequestDto
import com.derekwinters.chores.data.network.dto.LoginResponseDto
import com.derekwinters.chores.data.network.dto.UserInfoDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * chores-web backend API surface used by this issue (#5). Paths are relative to the placeholder
 * Retrofit base URL and are rewritten per-request by [BaseUrlInterceptor]; the `v1/` segment
 * matches the backend's `V1_PREFIX` (backend/app/main.py) applied to all routers except
 * status/metrics.
 */
interface ChoresApi {

    @POST("v1/auth/login")
    suspend fun login(@Body request: LoginRequestDto): LoginResponseDto

    /**
     * Provisioned per the issue #5 grilling Impact Areas table alongside login/chores/complete;
     * not currently called from any screen in this issue's narrowed scope.
     */
    @GET("v1/auth/me")
    suspend fun getCurrentUser(): UserInfoDto

    @GET("v1/chores")
    suspend fun getChores(): List<ChoreDto>

    @POST("v1/chores/{id}/complete")
    suspend fun completeChore(
        @Path("id") choreId: Int,
        @Body request: CompleteChoreRequestDto
    ): ChoreDto
}
