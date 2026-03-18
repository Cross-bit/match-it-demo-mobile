package com.example.matchit.ui.matchingSession

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.matchit.R
import com.example.matchit.data.model.session.SessionType
import com.example.matchit.data.notifications.localNotifications.LocalNotificationsService
import com.example.matchit.data.notifications.localNotifications.LocalNotificationsService.Companion.matchingSessionInviteChannel
import com.example.matchit.data.notifications.pushNotifications.NotificationDispatcher
import com.example.matchit.data.remote.client.Resource
import com.example.matchit.data.session.SessionRepository
import com.example.matchit.data.websockets.WebsocketManager
import com.example.matchit.ui.matchingSession.common.SessionForm
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel  @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val notificationDispatcher: NotificationDispatcher,
    private val localNotificationsService: LocalNotificationsService,
    private val websocketManager: WebsocketManager
    ) : ViewModel() {

    private val _sessionIsInitialisedEvent = MutableLiveData<ActivityOpenResultState>()
    val sessionIsInitialisedEvent: LiveData<ActivityOpenResultState> = _sessionIsInitialisedEvent

    private val _activeForm = MutableStateFlow<SessionForm?>(null)
    val activeForm: StateFlow<SessionForm?> = _activeForm



    fun setActiveForm(form: SessionForm) {
        _activeForm.value = form
    }

    fun clearActiveForm(form: SessionForm) {
        if (_activeForm.value === form) {
            _activeForm.value = null
        }
    }

    private val _openInviteConfirmEvent = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val openInviteConfirmEvent = _openInviteConfirmEvent.asSharedFlow()

    private val _closeSessionEvent = MutableSharedFlow<Int>(
        replay = 0,
        extraBufferCapacity = 1
    )

    val closeSessionActivityEvent = _closeSessionEvent.asSharedFlow()

    init {

        /**
         * Session invitation came and so we opened it
         */
        viewModelScope.launch {
            val invite = notificationDispatcher.currentInvite.value
                ?: return@launch

            val res = sessionRepository.checkSessionIsCreated(invite.sessionUUID)

            if (res is Resource.Success && res.data) {
                sessionRepository.setIsInSession(true)
                _openInviteConfirmEvent.tryEmit(Unit)
            } else {
                notificationDispatcher.invalidateSessionInvite()
                _closeSessionEvent.tryEmit(R.string.session_invite_invalid)
            }
        }

        viewModelScope.launch {
            sessionRepository.lastSessionType.collect { sessionType ->
                sessionType?.let {
                    checkActivityInitialisationState(it)
                }
            }
        }
    }

    private fun checkActivityInitialisationState(sessionType: SessionType) {
        viewModelScope.launch {
            try {
                val result = sessionRepository.checkSessionIsInitialised(sessionType)

                val resultState = when (result) {
                    is Resource.Success -> {
                        if(result.data) ActivityOpenResultState.ResultState.OK else ActivityOpenResultState.ResultState.NOT_INITIALISED
                    }
                    else -> {
                        ActivityOpenResultState.ResultState.NOT_INITIALISED
                    }

                }

                _sessionIsInitialisedEvent.postValue(
                    ActivityOpenResultState(resultState, sessionType)
                )
            }
            catch (err: Error) {
                _sessionIsInitialisedEvent.postValue(
                    ActivityOpenResultState(ActivityOpenResultState.ResultState.ERROR, sessionType, 0))
            }
        }
    }

    private fun terminateSession() {
        viewModelScope.launch {
            sessionRepository.terminateSession()
        }
    }

    public fun userNetworkStateChanged(isConnected: Boolean) {
        viewModelScope.launch {
            websocketManager.onNetworkChanged(isConnected)
        }
    }

    fun clearOutSession() {
        localNotificationsService.clearNotification(matchingSessionInviteChannel.notificationId)
    }

}