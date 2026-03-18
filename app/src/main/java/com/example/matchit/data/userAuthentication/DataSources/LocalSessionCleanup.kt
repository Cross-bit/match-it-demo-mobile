package com.example.matchit.data.userAuthentication.DataSources

import com.example.matchit.data.local.db.MatchItDatabase
import com.example.matchit.data.remote.client.LoginSessionManager
import javax.inject.Inject


/**
 * Helper class to Clears all local session data by deleting tokens and wiping the database after logout or account deletion.
 */
class LocalSessionCleanup @Inject constructor(
    private val loginSessionManager: LoginSessionManager,
    private val db: MatchItDatabase
) {

    suspend fun clear() {
        loginSessionManager.cleanTokens() // user has no access/refresh token ==> logged out
        db.clearAllTables() // we wipe out content of all tables
    }
}