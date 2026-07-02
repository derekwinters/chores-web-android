package com.derekwinters.chores.di

import com.derekwinters.chores.data.network.AuthInterceptor
import com.derekwinters.chores.data.network.BaseUrlInterceptor
import com.derekwinters.chores.data.network.ChoresApi
import com.derekwinters.chores.data.network.UnauthorizedInterceptor
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

/**
 * Issue #5 behavior: "Hilt modules for OkHttp/Retrofit/EncryptedSharedPreferences singletons".
 *
 * Retrofit is built against a placeholder base URL because the real server address is
 * user-entered at runtime; [BaseUrlInterceptor] rewrites it per-request (see ADR 0002 for why
 * this is preferred over rebuilding the Retrofit singleton on URL change).
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val PLACEHOLDER_BASE_URL = "http://localhost/"

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        baseUrlInterceptor: BaseUrlInterceptor,
        authInterceptor: AuthInterceptor,
        unauthorizedInterceptor: UnauthorizedInterceptor
    ): OkHttpClient = OkHttpClient.Builder()
        // Order matters: rewrite the URL first, then attach auth, then observe the response.
        .addInterceptor(baseUrlInterceptor)
        .addInterceptor(authInterceptor)
        .addInterceptor(unauthorizedInterceptor)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(PLACEHOLDER_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideChoresApi(retrofit: Retrofit): ChoresApi = retrofit.create(ChoresApi::class.java)
}
