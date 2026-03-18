package com.example.matchit.di

import android.content.Context
import com.example.matchit.data.notifications.localNotifications.LocalNotificationsService
import com.example.matchit.data.notifications.pushNotifications.NotificationDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NotificationModule {

    @Singleton
    @Provides
    fun provideNotificationDispatcher(): NotificationDispatcher {
        return NotificationDispatcher()
    }

    @Provides
    fun notificationsService(@ApplicationContext context: Context): LocalNotificationsService {
        return LocalNotificationsService(context)
    }
}
