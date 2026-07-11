package com.derekwinters.chores.di

import com.derekwinters.chores.data.network.AuthInterceptor
import com.derekwinters.chores.data.network.BaseUrlInterceptor
import com.derekwinters.chores.data.network.ChoresApi
import com.derekwinters.chores.data.network.GitHubApi
import com.derekwinters.chores.data.network.UnauthorizedInterceptor
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

/**
 * Qualifies the [Retrofit]/[OkHttpClient] bindings for the fixed `api.github.com` host (issue
 * #35's client-side own-version check). Kept separate from the unqualified [ChoresApi] bindings
 * below, which point at the user-entered self-hosted backend and carry backend-specific
 * interceptors ([BaseUrlInterceptor], [AuthInterceptor]) that must NOT apply to a fixed
 * third-party host.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GitHubRetrofit

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
    private const val GITHUB_BASE_URL = "https://api.github.com/"

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

    /**
     * Issue #35: a plain client with no interceptors — [BaseUrlInterceptor] would rewrite this
     * request's host away from `api.github.com` to the user-entered backend, and
     * [AuthInterceptor] would attach a chores-backend bearer token GitHub has no use for.
     */
    @Provides
    @Singleton
    @GitHubRetrofit
    fun provideGitHubOkHttpClient(): OkHttpClient = OkHttpClient.Builder().build()

    @Provides
    @Singleton
    @GitHubRetrofit
    fun provideGitHubRetrofit(
        @GitHubRetrofit okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit = Retrofit.Builder()
        .baseUrl(GITHUB_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideGitHubApi(@GitHubRetrofit retrofit: Retrofit): GitHubApi = retrofit.create(GitHubApi::class.java)
}
