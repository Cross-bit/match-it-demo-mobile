package com.example.matchit.di

import com.example.matchit.data.dataStores.WebsocketSessionStoreImpl
import com.example.matchit.data.secureStorage.SecureLoginSessionManager
import com.example.matchit.data.userAuthentication.DataSources.LoginDataSource
import com.example.matchit.data.websockets.WebsocketManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WebsocktesModule {
    @Singleton
    @Provides
    fun provideWebsocketManage(loginDataSource: LoginDataSource, websocketSessionStoreImpl: WebsocketSessionStoreImpl, loginSessionManager: SecureLoginSessionManager ) : WebsocketManager {
        return WebsocketManager(loginDataSource, loginSessionManager, websocketSessionStoreImpl)
    }
}
