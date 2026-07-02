package com.derekwinters.chores.data.auth

/** In-memory [CredentialStore] test double — no Android Keystore / Robolectric shadow needed. */
class FakeCredentialStore(
    private var serverUrl: String? = null,
    private var token: String? = null,
    private var tokenType: String? = null
) : CredentialStore {
    override fun getServerUrl(): String? = serverUrl
    override fun setServerUrl(serverUrl: String) { this.serverUrl = serverUrl }
    override fun getToken(): String? = token
    override fun getTokenType(): String? = tokenType
    override fun saveToken(token: String, tokenType: String) {
        this.token = token
        this.tokenType = tokenType
    }
    override fun clearToken() {
        token = null
        tokenType = null
    }
    override fun hasSession(): Boolean = !token.isNullOrBlank()
}
