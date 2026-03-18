package com.example.matchit.di

import com.example.matchit.data.dataStores.SessionInfoManager
import com.example.matchit.data.deviceSensors.LocationRepository
import com.example.matchit.data.friendships.FriendsDataSource
import com.example.matchit.data.friendships.FriendsRepository
import com.example.matchit.data.friendships.FriendsRepositoryImpl
import com.example.matchit.data.local.db.dao.FriendsDao
import com.example.matchit.data.secureStorage.SecureLoginSessionManager
import com.example.matchit.data.session.DataSources.SessionLocalDataSource

import com.example.matchit.data.session.DataSources.SessionDataSource
import com.example.matchit.data.session.SessionParametersRepository
import com.example.matchit.data.session.SessionRepository
import com.example.matchit.data.userAuthentication.DataSources.LocalSessionCleanup
import com.example.matchit.data.userAuthentication.DataSources.LoginDataSource
import com.example.matchit.data.userAuthentication.DataSources.UserDataLocalDataSource
import com.example.matchit.data.userAuthentication.LoginRepository
import com.example.matchit.data.websockets.WebsocketManager
import com.google.android.gms.location.FusedLocationProviderClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Singleton
    @Provides
    fun provideSessionParametersRepository(userDataSource: UserDataLocalDataSource, sessionLocalDataSource: SessionLocalDataSource): SessionParametersRepository {
        return SessionParametersRepository(userDataSource,  sessionLocalDataSource)
    }

    @Singleton
    @Provides
    fun provideLoginRepository(loginDataSource: LoginDataSource, loginLocalDataSource: UserDataLocalDataSource,
                               loginSessionManager: SecureLoginSessionManager, localSessionCleanup: LocalSessionCleanup): LoginRepository {
        return LoginRepository(loginDataSource, loginLocalDataSource, loginSessionManager, localSessionCleanup)
    }

    @Singleton
    @Provides
    fun provideFriendsRepository(friendsDataSource: FriendsDataSource, friendsDao: FriendsDao): FriendsRepository {
        return FriendsRepositoryImpl(friendsDataSource, friendsDao)
    }

    @Singleton
    @Provides
    fun provideSessionRepository(sessionInfoManager: SessionInfoManager,
                                 sessionDataSource: SessionDataSource,
                                 websocketsManager: WebsocketManager): SessionRepository {
        return SessionRepository(sessionInfoManager, sessionDataSource, websocketsManager)
    }

    @Singleton
    @Provides
    fun provideLocationRepository(fusedLocationProvider: FusedLocationProviderClient): LocationRepository {
        return LocationRepository(fusedLocationProvider)
    }
}