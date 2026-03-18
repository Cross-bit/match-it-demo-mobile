package com.example.matchit.data.notifications.pushNotifications

import android.util.Log
import com.example.matchit.data.notifications.pushNotifications.model.InviteMemberToSessionDTO
import com.example.matchit.data.notifications.pushNotifications.model.NewFriendRequestDTO

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow


/**
 * Dispatches notification/data of FCM messaging service across the app
 */
class NotificationDispatcher {

    private val _currentSessionInvite =
        MutableStateFlow<InviteMemberToSessionDTO?>(null)

    /**
     * Holds currently active invitation to the session
     */
    val currentInvite: StateFlow<InviteMemberToSessionDTO?> = _currentSessionInvite

    /**
     * Notifies app about newly incoming session invite
     */
    private val _inviteEvent = MutableSharedFlow<InviteMemberToSessionDTO>(
        replay = 0,
        extraBufferCapacity = 1)

    val inviteEvent: SharedFlow<InviteMemberToSessionDTO> = _inviteEvent.asSharedFlow()

    /** Simple notification event */
    private val _simpleNotificationEvent = MutableStateFlow<NotificationBody?>(null)
    val simpleNotificationEvent: StateFlow<NotificationBody?> = _simpleNotificationEvent

    /** Raised when friend sends us new friend request */
    private val _newFriendRequest = MutableStateFlow<NewFriendRequestDTO?>(null)
    val newFriendRequest: StateFlow<NewFriendRequestDTO?> = _newFriendRequest

    fun postSimpleNotificationEvent(notification: NotificationBody) {
        _simpleNotificationEvent.value = notification
    }

    fun postNewFriendRequestNotificationEvent(request: NewFriendRequestDTO) {
        _newFriendRequest.value = request
    }

    fun invalidateFriendshipInvite() {
        _newFriendRequest.value = null
    }

    fun invalidateSessionInvite() {
        _currentSessionInvite.value = null
    }

    fun postInvitedToSessionEvent(invite: InviteMemberToSessionDTO?) {
        if (_currentSessionInvite.value != null // we want only one invite at the time
            || invite == null)
        {
            Log.w("InviteDispatcher", "Invite ignored – another invite already active")
            return
        }

        _currentSessionInvite.value = invite
        _inviteEvent.tryEmit(invite)
    }
}