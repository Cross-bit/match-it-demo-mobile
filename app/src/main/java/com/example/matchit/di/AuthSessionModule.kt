package com.example.matchit.di

import com.example.matchit.data.remote.client.LoginSessionManager
import com.example.matchit.data.secureStorage.SecureLoginSessionManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent



/**
 * Authentication
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthSessionModule {

    @Binds
    abstract fun bindLoginSessionManager(
        impl: SecureLoginSessionManager
    ): LoginSessionManager
}