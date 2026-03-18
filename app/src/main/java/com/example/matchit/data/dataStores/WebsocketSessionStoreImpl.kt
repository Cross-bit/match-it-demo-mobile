package com.example.matchit.data.dataStores

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.matchit.data.websockets.WebsocketSessionStore
import kotlinx.coroutines.flow.first
import javax.inject.Inject


class WebsocketSessionStoreImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : WebsocketSessionStore
{

    companion object {
        private const val KEY_WS_SESSION = "WS_SESSION_ID"
        private const val KEY_WS_SEQUENCE = "WS_SEQUENCE"
    }

    override suspend fun getOrCreateSessionId(): String {
        val sessionKey = stringPreferencesKey(KEY_WS_SESSION)
        val prefs = dataStore.data.first()

        val existing = prefs[sessionKey]
        if (existing != null) return existing

        val newId = generateId()

        dataStore.edit { preferences ->
            preferences[sessionKey] = newId
        }

        return newId
    }

    override suspend fun nextSequence(): Long {
        val seqKey = stringPreferencesKey(KEY_WS_SEQUENCE)
        val prefs = dataStore.data.first()

        val stored = prefs[seqKey]
        val current = stored?.toLongOrNull() ?: 0L
        val next = current + 1

        dataStore.edit { preferences ->
            preferences[seqKey] = next.toString()
        }

        return current
    }

    override suspend fun reset() {
        val sessionKey = stringPreferencesKey(KEY_WS_SESSION)
        val seqKey = stringPreferencesKey(KEY_WS_SEQUENCE)

        dataStore.edit { preferences ->
            preferences.remove(sessionKey)
            preferences.remove(seqKey)
        }
    }

    private fun generateId(): String =
        java.util.UUID.randomUUID()
            .toString()
            .replace("-", "")
            .take(12)
}