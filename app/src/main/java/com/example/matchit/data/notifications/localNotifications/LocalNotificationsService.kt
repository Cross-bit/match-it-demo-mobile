package com.example.matchit.data.notifications.localNotifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.matchit.BuildConfig.FRIENDS_REQ_CHANNEL_ID
import com.example.matchit.BuildConfig.GENERAL_UPDATES_CHANNEL_ID
import com.example.matchit.BuildConfig.MATCHING_SESSION_INVITE_CHANNEL_ID
import com.example.matchit.R
import com.example.matchit.data.broadcastReceivers.NotificationDismissedReceiver
import javax.inject.Inject

data class NotificationSettings(
    val channelId: String,
    val channelName: Int,
    val descriptionText: Int,
    val notificationId: Int,
)

data class NotificationData(
    val title: String,
    val message: String,
)

const val ACTION_NOTIFICATION_DISMISS = "com.example.matchit.ACTION_NOTIFICATION_DISMISS"
const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
const val EXTRA_SESSION_UUID = "extra_session_uuid"

class LocalNotificationsService
@Inject constructor(private val context: Context) {

    companion object {
        val matchingSessionInviteChannel = NotificationSettings(
            MATCHING_SESSION_INVITE_CHANNEL_ID,
            R.string.session_invitations_channel_name,
            R.string.session_invitations_channel_description,
            0
        )

        val friendsRequestNotificationChannel = NotificationSettings(
            FRIENDS_REQ_CHANNEL_ID,
            R.string.friends_requests_channel_name,
            R.string.friends_requests_channel_description,
            1
        )

        val generalUpdatesChannel = NotificationSettings(
            GENERAL_UPDATES_CHANNEL_ID,
            R.string.general_updates_channel_name,
            R.string.general_updates_channel_description,
            2
        )
    }

    fun clearNotification(notificationId: Int) {
        val notificationManager = ContextCompat.getSystemService(
            context,
            NotificationManager::class.java
        )
        notificationManager?.cancel(notificationId)
    }

    fun showNotification(
        data: NotificationData,
        settings: NotificationSettings,
        intent: Intent? = null,
        extras: Map<String, String> = emptyMap()
    ) {
        val builder = NotificationCompat.Builder(context, settings.channelId)
            .setSmallIcon(R.drawable.ic_activity_selection)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000))
            .setOnlyAlertOnce(true)
            .setContentTitle(data.title)
            .setContentText(data.message)

        intent?.let { addIntentToNotification(it, builder) }

        // delete / dismiss intent (swipe away)
        val dismissIntent = Intent(context, NotificationDismissedReceiver::class.java).apply {
            action = ACTION_NOTIFICATION_DISMISS
            putExtra(EXTRA_NOTIFICATION_ID, settings.notificationId)
            extras.forEach { (key, value) ->
                putExtra(key, value)
            }
        }

        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            settings.notificationId,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        builder.setDeleteIntent(dismissPendingIntent)

        val notificationManager = ContextCompat.getSystemService(
            context,
            NotificationManager::class.java
        )

        notificationManager?.notify(settings.notificationId, builder.build())
            ?: Log.e("notificationError", "notification manager not initialised")
    }

    private fun addIntentToNotification(intent: Intent, builder: NotificationCompat.Builder) {
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        builder.setContentIntent(pendingIntent)
    }
}