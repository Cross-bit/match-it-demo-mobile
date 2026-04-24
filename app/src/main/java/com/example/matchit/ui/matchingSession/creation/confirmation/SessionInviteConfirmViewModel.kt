package com.example.matchit.ui.matchingSession.creation.confirmation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.matchit.R
import com.example.matchit.data.model.session.SessionType
import com.example.matchit.data.notifications.localNotifications.LocalNotificationsService
import com.example.matchit.data.notifications.localNotifications.LocalNotificationsService.Companion.matchingSessionInviteChannel
import com.example.matchit.data.notifications.pushNotifications.NotificationDispatcher
import com.example.matchit.data.notifications.pushNotifications.model.InviteMemberToSessionDTO
import com.example.matchit.data.notifications.pushNotifications.model.SessionTerminatedDTO
import com.example.matchit.data.remote.client.ApiErrors.ApiError
import com.example.matchit.data.remote.client.Resource
import com.example.matchit.data.session.SessionRepository
import com.example.matchit.data.websockets.WebSocketEvent
import com.example.matchit.data.websockets.WebsocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


interface InviteConfirmationStrategy {
        suspend fun acceptSessionInvite() : Resource<Boolean, ApiError<*>>
}

@HiltViewModel
class SessionInviteConfirmViewModel @Inject constructor(
        private val sessionRepository: SessionRepository,
        private val notificationDispatcher: NotificationDispatcher,
        private val websocketManager: WebsocketManager,
        private val localNotificationsService: LocalNotificationsService
) : ViewModel() {

        private var invitedSessionUUID: String? = null
        private var isAccepting: Boolean = false

        private val _sessionCreatorName = MutableStateFlow<String?>(null)
        val sessionCreatorName: StateFlow<String?> = _sessionCreatorName

        private val _sessionType = MutableStateFlow<SessionType?>(null)
        val sessionType: StateFlow<SessionType?> = _sessionType

        private val _sessionStartedEvent = MutableSharedFlow<Int>(
                replay = 0,
                extraBufferCapacity = 1
        )
        val sessionStartedEvent = _sessionStartedEvent.asSharedFlow()

        private val _sessionTerminatedEvent = MutableSharedFlow<Int>(
                replay = 0,
                extraBufferCapacity = 1
        )
        val sessionTerminatedEvent = _sessionTerminatedEvent.asSharedFlow()

        private var confirmStrategy: InviteConfirmationStrategy? = null
        private var sessionData: InviteMemberToSessionDTO? = null

        private val _sessionConnectionMessageEvent = MutableSharedFlow<Int>()
        val sessionConnectionMessageEvent = _sessionConnectionMessageEvent.asSharedFlow()

        private val _confirmRejectEvent = MutableSharedFlow<Unit>(
                replay = 0,
                extraBufferCapacity = 1
        )
        val closeConfirmEvent = _confirmRejectEvent.asSharedFlow()

        init { initialize() }

        private fun initialize() {

                val invite = notificationDispatcher.currentInvite.value ?: return // rest makes no sense --> skip

                invitedSessionUUID = invite.sessionUUID
                _sessionCreatorName.value = invite.sessionCreator.name

                _sessionType.value = invite.sessionType
                sessionRepository.setLastSessionType(invite.sessionType);

                viewModelScope.launch {
                        websocketManager.messagesEvents.collect { event ->
                                when(event) {
                                        is WebSocketEvent.SessionStarted -> {
                                                if (!isAccepting) return@collect
                                                if (event.dto.sessionInfo.sessionUUID != invitedSessionUUID) return@collect

                                                isAccepting = false
                                                invitedSessionUUID = null

                                                notificationDispatcher.invalidateSessionInvite()
                                                _sessionStartedEvent.tryEmit(R.string.session_started)
                                        }
                                        is WebSocketEvent.SessionTerminated -> {
                                                if (event.dto.sessionUUID != invitedSessionUUID) return@collect

                                                isAccepting = false
                                                invitedSessionUUID = null

                                                notificationDispatcher.invalidateSessionInvite()
                                                val message = when(event.dto.reason) {
                                                    SessionTerminatedDTO.TerminationReason.SESSION_CREATION_TIMEOUT -> R.string.session_creation_timeout_reached
                                                    SessionTerminatedDTO.TerminationReason.SESSION_MAX_DURATION -> R.string.session_duration_timeout_reached
                                                    SessionTerminatedDTO.TerminationReason.UNEXPECTED -> R.string.session_invitation_closed
                                                    else -> R.string.session_invitation_failed
                                                }
                                                _sessionTerminatedEvent.emit(message)
                                        }
                                        else -> Unit
                                }
                        }
                }
        }

        /**
         * Should be raised anytime view is created
         */
        fun initOnViewCreated() {
                // connect client to the websocket server
                viewModelScope.launch {
                        sessionRepository.connectToWS()
                }

                localNotificationsService.clearNotification(matchingSessionInviteChannel.notificationId)
        }

        fun acceptSessionInvite() {
                val sessionUUID = invitedSessionUUID ?: return

                viewModelScope.launch {

                        _sessionConnectionMessageEvent.emit(R.string.connected_to_session)

                        val result = sessionRepository.confirmSessionInvitation(sessionUUID)

                        if (result is Resource.Success) {
                                confirmStrategy?.acceptSessionInvite()
                                // reset the state
                                isAccepting = true
                                notificationDispatcher.invalidateSessionInvite()
                        } else {
                                _sessionTerminatedEvent.emit(R.string.session_connection_failed)
                                Log.e("sessionConfirmationError", "Session invite confirmation failed")
                        }

                }
        }

        fun rejectSessionInvite() {
                val sessionUUID = invitedSessionUUID ?: return

                viewModelScope.launch {
                        val result = sessionRepository.rejectSessionInvitation(sessionUUID)

                        isAccepting = false
                        invitedSessionUUID = null
                        notificationDispatcher.invalidateSessionInvite()

                        _confirmRejectEvent.tryEmit(Unit)

                        if (result is Resource.Error || result is Resource.GeneralError) {
                                Log.e("sessionReject", "Reject failed for session $sessionUUID")
                        }
                }
        }

}