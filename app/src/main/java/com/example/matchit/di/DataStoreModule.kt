package com.example.matchit.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.example.matchit.data.dataStores.SessionInfoManager
import com.example.matchit.data.dataStores.SessionInfoManagerImpl
import com.example.matchit.data.remote.client.AuthenticatedApiCaller
import com.example.matchit.data.remote.client.LoginSessionManager
import com.example.matchit.data.remote.client.RequestClientFactory
import com.example.matchit.data.remote.client.RetrofitSafeApiCaller
import com.example.matchit.data.secureStorage.SecureLoginSessionManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

private const val USER_PREFERENCES_NAME = "authentication_preferences"

@InstallIn(SingletonComponent::class)
@Module
object DataStoreModule {

    @Singleton
    @Provides
    fun providePreferencesDataStore(@ApplicationContext appContext: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler(
                produceNewData =  { emptyPreferences() }
            ),
            produceFile = { appContext.preferencesDataStoreFile(USER_PREFERENCES_NAME) },
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            migrations = listOf(SharedPreferencesMigration(appContext, USER_PREFERENCES_NAME))
        )
    }

    @Singleton
    @Provides
    fun providesSecureLoginSessionManager(
        @ApplicationContext appContext: Context
    ): SecureLoginSessionManager {
        return SecureLoginSessionManager(appContext);
    }

    @Singleton
    @Provides
    fun provideSessionInfoManager(preferencesDataStore: DataStore<Preferences>): SessionInfoManager {
        return SessionInfoManagerImpl(preferencesDataStore)
    }


    @Singleton
    @Provides
    fun provideRetrofitSafeApiCaller(requestClientFactory: RequestClientFactory): RetrofitSafeApiCaller {
        return RetrofitSafeApiCaller(requestClientFactory)
    }

    @Provides
    fun provideReAuthenticationHandler(loginSessionManager: SecureLoginSessionManager,
                                       apiCaller: RetrofitSafeApiCaller,
                                       requestClientFactory: RequestClientFactory): AuthenticatedApiCaller {
        return AuthenticatedApiCaller(loginSessionManager, apiCaller, requestClientFactory)
    }
}
