package com.example.matchit.di

import android.content.Context
import com.example.matchit.data.chat.DataSources.ChatRemoteDataSource
import com.example.matchit.data.friendships.FriendsDataSource
import com.example.matchit.data.remote.client.AuthenticatedApiCaller
import com.example.matchit.data.remote.client.RequestClientFactory
import com.example.matchit.data.remote.client.RetrofitSafeApiCaller
import com.example.matchit.data.secureStorage.SecureLoginSessionManager

import com.example.matchit.data.session.DataSources.SessionDataSource
import com.example.matchit.data.users.UsersRemoteDataSource
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataSourceModule {

    @Singleton
    @Provides
    fun provideFriendsDataSource(
        reAuthenticationHandler: AuthenticatedApiCaller,
        requestClientFactory: RequestClientFactory
    ): FriendsDataSource {
        return FriendsDataSource( reAuthenticationHandler, requestClientFactory)
    }

    @Singleton
    @Provides
    fun providesSessionDataSource(
        authenticatedApiCaller: AuthenticatedApiCaller,
    ): SessionDataSource {
        return SessionDataSource(authenticatedApiCaller);
    }


    @Singleton
    @Provides
    fun providesChatRemoteDataSource(
        authenticatedApiCaller: AuthenticatedApiCaller,
    ): ChatRemoteDataSource {
        return ChatRemoteDataSource(authenticatedApiCaller);
    }

    @Singleton
    @Provides
    fun providesUsersRemoteDataSource(authenticatedApiCaller: AuthenticatedApiCaller): UsersRemoteDataSource {
        return UsersRemoteDataSource(authenticatedApiCaller);
    }

    @Singleton
    fun providesAuthenticatedApiCaller(
        loginSessionManager: SecureLoginSessionManager,
        apiCaller: RetrofitSafeApiCaller,
        requestClientFactory: RequestClientFactory
    ): AuthenticatedApiCaller
    {
        return AuthenticatedApiCaller(loginSessionManager,apiCaller,requestClientFactory);
    }

}