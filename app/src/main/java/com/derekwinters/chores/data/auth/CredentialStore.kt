package com.derekwinters.chores.data.auth

/**
 * Persists the auth token and user-entered server URL (issue #5, behavior: Login screen
 * persists token + URL). Backed by EncryptedSharedPreferences in production
 * (see [EncryptedCredentialStore] / di.StorageModule); kept as an interface so interceptors and
 * ViewModels can be tested against a fake without touching the Android Keystore.
 */
interface CredentialStore {
    fun getServerUrl(): String?
    fun setServerUrl(serverUrl: String)
    fun getToken(): String?
    fun getTokenType(): String?
    fun saveToken(token: String, tokenType: String)
    fun clearToken()
    fun hasSession(): Boolean
}
