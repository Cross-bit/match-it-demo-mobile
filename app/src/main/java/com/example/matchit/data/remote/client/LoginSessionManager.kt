package com.example.matchit.data.remote.client

interface LoginSessionManager {
    /**
     * Saves the authorization (access) token to local storage.
     *
     * @param token access token received after successful login
     */
    suspend fun saveAuthToken(token: String)

    /**
     * Loads the authorization (access) token from local storage.
     *
     * @return access token, or null if it has not been stored yet
     */
    suspend fun getAuthToken(): String?

    /**
     * Removes all stored authentication data
     * (both access and refresh tokens).
     */
    suspend fun cleanTokens()

    /**
     * Saves the refresh token to local storage.
     *
     * @param token refresh token received from the server
     */
    suspend fun saveRefreshToken(token: String)

    /**
     * Loads the refresh token from local storage.
     *
     * @return refresh token, or null if it has not been stored yet
     */
    suspend fun getRefreshToken(): String?
}