package com.example.matchit.data.secureStorage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.matchit.data.remote.client.LoginSessionManager
import javax.inject.Inject

class SecureLoginSessionManager @Inject constructor(
    context: Context
) : LoginSessionManager {

    companion object {
        private const val FILE_NAME = "secure_prefs"
        private const val AUTH_TOKEN = "auth_token"
        private const val REFRESH_TOKEN = "refresh_token"
    }

    private val sharedPreferences: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override suspend fun saveAuthToken(token: String) {
        sharedPreferences.edit().putString(AUTH_TOKEN, token).apply()
    }

    override suspend fun getAuthToken(): String? {
        return sharedPreferences.getString(AUTH_TOKEN, null)
    }

    override suspend fun saveRefreshToken(token: String) {
        sharedPreferences.edit().putString(REFRESH_TOKEN, token).apply()
    }

    override suspend fun getRefreshToken(): String? {
        return sharedPreferences.getString(REFRESH_TOKEN, null)
    }

    override suspend fun cleanTokens() {
        sharedPreferences.edit()
            .remove(AUTH_TOKEN)
            .remove(REFRESH_TOKEN)
            .apply()
    }
}