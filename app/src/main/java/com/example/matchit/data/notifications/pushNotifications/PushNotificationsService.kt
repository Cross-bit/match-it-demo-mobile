package com.example.matchit.data.notifications.pushNotifications

import android.content.Intent
import android.util.Log
import com.example.matchit.R
import com.example.matchit.data.app.AppVisibilityTracker
import com.example.matchit.data.notifications.localNotifications.EXTRA_SESSION_UUID
import com.example.matchit.data.notifications.localNotifications.LocalNotificationsService
import com.example.matchit.data.notifications.localNotifications.NotificationData
import com.example.matchit.data.notifications.pushNotifications.model.InviteMemberToSessionDTO
import com.example.matchit.data.notifications.pushNotifications.model.NewFriendRequestDTO
import com.example.matchit.data.notifications.pushNotifications.model.DataFCMType
import com.example.matchit.data.remote.client.Resource
import com.example.matchit.data.session.SessionRepository
import com.example.matchit.ui.MainActivity
import com.example.matchit.ui.matchingSession.SessionActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PushNotificationsService : FirebaseMessagingService()
{
    @Inject
    lateinit var notificationDispatcher: NotificationDispatcher

    @Inject
    lateinit var notificationsService: LocalNotificationsService

    @Inject
    lateinit var sessionRepository: SessionRepository

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        CoroutineScope(Dispatchers.IO).launch {
            // we got notification type of FCM
            message.notification?.let { notification ->
                notificationsService.showNotification(
                    NotificationData(notification.title.orEmpty(), notification.body.orEmpty()),
                    LocalNotificationsService.generalUpdatesChannel
                )
                notificationDispatcher.postSimpleNotificationEvent(
                    NotificationBody(notification.body.orEmpty(), notification.title.orEmpty())
                )
            }

            // we got data type of FCM
            handleDataMessage(message.data)
        }
    }

    private fun handleDataMessage(data: Map<String, String>) {

        Log.i("fcm", "fcm data message received!");

        data["type"]?.let { type ->
            when (type) {
                DataFCMType.INVITE_MEMBER.toString() -> {
                    try {
                        val jsonStr = data["payload"] ?: return;
                        val dataMessage = Gson().fromJson(jsonStr, InviteMemberToSessionDTO::class.java)

                        scope.launch {

                            val isValid = when(val isSessionCreated = sessionRepository.checkSessionIsCreated(dataMessage.sessionUUID)) {
                                is Resource.Success -> isSessionCreated.data
                                else -> false
                            }

                            if (!isValid) {
                                Log.i("FCM", "Invite ${dataMessage.sessionUUID} is outdated, ignoring")
                                return@launch
                            }

                            // if valid post invite
                            notificationDispatcher.postInvitedToSessionEvent(dataMessage)

                            if (AppVisibilityTracker.isAppInForeground()) return@launch

                            handleSessionInvitationNotification(
                                dataMessage,
                                DataFCMType.INVITE_MEMBER
                            )
                        }
                    }
                    catch (error: Error) {
                        Log.e("fcmError", "Error decoding invitation data.")
                    }
                }
                DataFCMType.FRIEND_REQUEST_NOTIFICATION.toString() -> {
                    val jsonStr = data["payload"] ?: return;
                    val dataMessage = Gson().fromJson(jsonStr, NewFriendRequestDTO::class.java)

                    notificationDispatcher.postNewFriendRequestNotificationEvent(dataMessage);

                    val inviteTitle = getString(R.string.friend_request_title)
                    val invitationMessage = String.format(getString(R.string.friend_request_message), dataMessage.friendName)

                    val intent = Intent(this, MainActivity::class.java)

                    notificationsService.showNotification(
                        NotificationData(inviteTitle, invitationMessage),
                        LocalNotificationsService.friendsRequestNotificationChannel,
                        intent
                    )

                }
                DataFCMType.DUMMY_MESSAGE.toString() -> {
                    // just a random dummy message... e.g. sent from the ping("keep ready connection") FCM service - log it to the console
                    val jsonStr = data["payload"] ?: "";
                    Log.i("fcm", jsonStr);
                }
                else -> {
                    Log.e("fcmError", "Unknown data message type recieved type: [${type}]")
                }
            }
        }
    }

    private fun handleSessionInvitationNotification(
        data: InviteMemberToSessionDTO,
        type: DataFCMType
    ) {
        if (AppVisibilityTracker.isAppInForeground()) return

        val inviteTitle = getString(R.string.session_invite_title)
        val invitationMessage = getString(
            R.string.session_invite_notification_message,
            data.sessionCreator.name
        )

        val intent = Intent(this, SessionActivity::class.java)

        notificationsService.showNotification(
            NotificationData(inviteTitle, invitationMessage),
            LocalNotificationsService.matchingSessionInviteChannel,
            intent,
            extras = mapOf(
                EXTRA_SESSION_UUID to data.sessionUUID
            )
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel() // Clean up coroutines
    }
}