package com.example.matchit.data.notifications.localNotifications


import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.matchit.R
import javax.inject.Inject

/*class NotificationsFactory
@Inject constructor(val context: Context)
{

    /**
     * Creates notification for new matching session invitation
     */
    fun createSessionInviteNotification() {

        // positive reply intent
        /*val intent = Intent(context, SessionActivity::class.java)

        // negative dismissIntent //TODO: factor this out ... probably some kind of ... factory would be nice
        val dismissIntent = Intent(context, NotificationDismissedReceiver::class.java).apply {
            action = NotificationDismissedReceiver.ACTION_NOTIFICATION_DISMISS
            putExtra(NotificationDismissedReceiver.EXTRA_NOTIFICATION_TYPE, type.value)
        }

        val dismissPendingIntent = PendingIntent.getBroadcast(this, 0, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)*/

    }

    /**
     * Creates notification for new pending request
     */
    fun createNewConnectionInviteNotification() {

    }

    fun showNotification(
        title: String,
        message: String,
        intent: Intent? = null,
        dismissedIntent: PendingIntent?  = null
    ) {

        val builder: NotificationCompat.Builder = NotificationCompat.Builder(
            context,
            LocalNotificationsService.matchingSessionInviteChannel.channelId
        )
            .setSmallIcon(R.drawable.ic_activity_selection)
            .setAutoCancel(true)
            .setVibrate(
                longArrayOf(
                    1000, 1000, 1000,
                    1000, 1000
                )
            )
            .setOnlyAlertOnce(true)
            .setContentTitle(title)
            .setContentText(message)

        // set intent if provided

        intent?.let {
            addIntentToNotification(it, builder)
        }

        dismissedIntent?.let {
            builder.setDeleteIntent(dismissedIntent)
        }

        val notificationManager = ContextCompat.getSystemService(
            context,
            NotificationManager::class.java
        ) as NotificationManager?

        // if there is already ours notification we cancel it first
        notificationManager?.cancel(LocalNotificationsService.matchingSessionInviteChannel.notificationId)

        notificationManager?.let {
            notificationManager.notify(LocalNotificationsService.matchingSessionInviteChannel.notificationId, builder.build())
        }
            ?: Log.e("notificationError", "notification manager not initialised")
    }

    private fun addIntentToNotification(intent: Intent, builder: NotificationCompat.Builder) {
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        builder.setContentIntent(pendingIntent)
    }

}*/