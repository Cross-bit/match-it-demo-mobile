package com.example.matchit

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.webkit.WebView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.example.matchit.data.notifications.localNotifications.NotificationSettings
import com.example.matchit.data.notifications.localNotifications.LocalNotificationsService
import com.example.matchit.data.notifications.localNotifications.LocalNotificationsService.Companion.friendsRequestNotificationChannel
import com.example.matchit.data.notifications.localNotifications.LocalNotificationsService.Companion.generalUpdatesChannel
import com.example.matchit.data.notifications.localNotifications.LocalNotificationsService.Companion.matchingSessionInviteChannel
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MatchItApp : Application() {

    override fun onCreate() {
        super.onCreate()
        enforceDayModeOnly()
        createNotificationChannels()
    }

    private fun enforceDayModeOnly() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }

    /**
     * Creates local notification channel for push notifications if not exists yet.
     */
    private fun createNotificationChannels() {

        val notificationManager = ContextCompat.getSystemService(
            this,
            NotificationManager::class.java
        ) as NotificationManager?

        // Check if the Android Version is greater than Oreo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // not necessary check but we still
            notificationManager?.let {
                createChannel(matchingSessionInviteChannel, notificationManager)
                createChannel(friendsRequestNotificationChannel, notificationManager)
                createChannel(generalUpdatesChannel, notificationManager)
            }
        }
    }

    private fun createChannel(
        channelSettings: NotificationSettings,
        notificationManager: NotificationManager
    ) {
        val existingChannel =
            notificationManager.getNotificationChannel(channelSettings.channelId)

        if (existingChannel == null) {
            val notificationChannel = NotificationChannel(
                channelSettings.channelId,
                getString(channelSettings.channelName),
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.description =
                getString(channelSettings.descriptionText)

            notificationManager.createNotificationChannel(notificationChannel)
        }
    }
}