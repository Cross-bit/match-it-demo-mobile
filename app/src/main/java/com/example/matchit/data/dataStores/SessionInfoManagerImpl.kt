package com.example.matchit.data.dataStores

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import javax.inject.Inject


class SessionInfoManagerImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
): SessionInfoManager {

    companion object {
        const val MOVIES_INITIALISED = "MOVIES_INITIALISED"
        const val RESTAURANTS_INITIALISED = "RESTAURANTS_INITIALISED"
        const val BOARDGAMES_INITIALISED = "BOARDGAMES_INITIALISED"
    }

    override suspend fun setMovieInitialized(initialized: Boolean) {
        this.setInitialisation(MOVIES_INITIALISED, initialized);
    }

    override suspend fun isMovieInitialized(): Boolean {
        return isInitialized(MOVIES_INITIALISED);
    }

    override suspend fun setRestaurantInitialized(initialized: Boolean) {
        this.setInitialisation(RESTAURANTS_INITIALISED, initialized);
    }

    override suspend fun isRestaurantInitialized(): Boolean {
        return isInitialized(RESTAURANTS_INITIALISED);
    }

    private suspend fun setInitialisation(key: String, initialized: Boolean) {
        val dataStoreKey = stringPreferencesKey(key)
        dataStore.edit { preferences ->
            preferences[dataStoreKey] = if (initialized) "1" else "0"
        }
    }

    private suspend fun isInitialized(key: String) : Boolean {
        val dataStoreKey = stringPreferencesKey(key)
        val preferences = dataStore.data.first()

        return preferences[dataStoreKey] != null && preferences[dataStoreKey] == "1"
    }

}