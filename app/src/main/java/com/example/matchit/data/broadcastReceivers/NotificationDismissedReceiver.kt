package com.example.matchit.data.broadcastReceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.matchit.data.notifications.localNotifications.EXTRA_NOTIFICATION_ID
import com.example.matchit.data.notifications.localNotifications.ACTION_NOTIFICATION_DISMISS
import com.example.matchit.data.notifications.localNotifications.EXTRA_SESSION_UUID
import com.example.matchit.data.notifications.localNotifications.LocalNotificationsService
import com.example.matchit.data.notifications.pushNotifications.NotificationDispatcher
import com.example.matchit.data.session.SessionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationDismissedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var sessionRepository: SessionRepository

    @Inject
    lateinit var notificationDispatcher: NotificationDispatcher

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_NOTIFICATION_DISMISS) return

        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        Log.d("NotificationDismissedReceiver", "Dismissed id: $notificationId")

        when (notificationId) {

            // session invite dismissed
            LocalNotificationsService.matchingSessionInviteChannel.notificationId -> {
                Log.d("NotificationDismissedReceiver", "Session invite dismissed")

                // try to reject invite
                CoroutineScope(Dispatchers.IO).launch {
                    val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
                    val sessionUUID = intent.getStringExtra(EXTRA_SESSION_UUID)

                    when (notificationId) {
                        LocalNotificationsService.matchingSessionInviteChannel.notificationId -> {
                            sessionUUID?.let { uuid ->
                                sessionRepository.rejectSessionInvitation(uuid)
                                // we have to explicitly clean it here
                                notificationDispatcher.invalidateSessionInvite()
                            }
                        }
                    }
                }
            }

            // friend request dismissed
            LocalNotificationsService.friendsRequestNotificationChannel.notificationId -> {
                Log.d("NotificationDismissedReceiver", "Friend request dismissed")
            }

            // general update dismissed
            LocalNotificationsService.generalUpdatesChannel.notificationId -> {
                Log.d("NotificationDismissedReceiver", "General update dismissed")
            }

            else -> {
                Log.d("NotificationDismissedReceiver", "Unknown notification dismissed")
            }
        }
    }
}