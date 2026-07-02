package com.derekwinters.chores.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [CredentialStore] backed by [EncryptedSharedPreferences] (issue #5 / ADR 0002: the 365-day
 * JWT and the user-entered server URL are persisted encrypted-at-rest, not in plain
 * SharedPreferences).
 */
@Singleton
class EncryptedCredentialStore @Inject constructor(
    @ApplicationContext context: Context
) : CredentialStore {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun getServerUrl(): String? = prefs.getString(KEY_SERVER_URL, null)

    override fun setServerUrl(serverUrl: String) {
        prefs.edit().putString(KEY_SERVER_URL, serverUrl).apply()
    }

    override fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    override fun getTokenType(): String? = prefs.getString(KEY_TOKEN_TYPE, null)

    override fun saveToken(token: String, tokenType: String) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_TOKEN_TYPE, tokenType)
            .apply()
    }

    override fun clearToken() {
        // Server URL is intentionally kept so the login screen can pre-fill it after a 401.
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_TOKEN_TYPE)
            .apply()
    }

    override fun hasSession(): Boolean = !getToken().isNullOrBlank()

    private companion object {
        const val PREFS_FILE_NAME = "chores_secure_prefs"
        const val KEY_SERVER_URL = "server_url"
        const val KEY_TOKEN = "auth_token"
        const val KEY_TOKEN_TYPE = "token_type"
    }
}
