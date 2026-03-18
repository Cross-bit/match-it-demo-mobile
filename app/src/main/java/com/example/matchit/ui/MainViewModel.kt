package com.example.matchit.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.matchit.R
import com.example.matchit.data.notifications.localNotifications.LocalNotificationsService
import com.example.matchit.data.notifications.pushNotifications.NotificationDispatcher
import com.example.matchit.data.remote.client.Resource
import com.example.matchit.data.session.SessionRepository
import com.example.matchit.data.userAuthentication.LoginRepository
import com.example.matchit.data.websockets.WebsocketManager

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val loginRepository: LoginRepository,
    private val sessionRepository: SessionRepository,
    private val websocketManager: WebsocketManager,
    notificationDispatcher: NotificationDispatcher,
    notificationsService: LocalNotificationsService
) : ViewModel() {

    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

    private val _userLogout = MutableLiveData<Boolean>()
    val userLogout: LiveData<Boolean> = _userLogout

    private val _newFriendRequest = MutableLiveData<String>()
    val newFriendRequest: LiveData<String> = _newFriendRequest

    private val _sessionUUID = MutableLiveData<String>()
    val sessionUUID: LiveData<String> = _sessionUUID

    private val _safeDestroy = MutableLiveData<Unit>()
    val safeDestroy: LiveData<Unit> = _safeDestroy

    private val _newSessionInvitationEvent = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val newSessionInvitationEvent = _newSessionInvitationEvent.asSharedFlow()

    init {

        viewModelScope.launch {
            val invite = notificationDispatcher.currentInvite.value
                ?: return@launch

            val res = sessionRepository.checkSessionIsCreated(invite.sessionUUID)

            if (res is Resource.Success && res.data) {
                sessionRepository.setIsInSession(true)
                _newSessionInvitationEvent.tryEmit(Unit)
            }
        }

        viewModelScope.launch {

            notificationDispatcher.inviteEvent.collectLatest { invite ->

                val res = sessionRepository.checkSessionIsCreated(invite.sessionUUID)

                if (res is Resource.Success && res.data) {
                    _newSessionInvitationEvent.tryEmit(Unit)
                }
            }
        }

        viewModelScope.launch {
            notificationDispatcher.newFriendRequest.collect {
                it?.let {
                    _newFriendRequest.value = it.friendName;
                }
            }
        }

        viewModelScope.launch {
            sessionRepository.lastSessionUUID.collect { sessionUUID ->
                sessionUUID?.let {
                    _sessionUUID.value = it;
                }
            }
        }

        viewModelScope.launch {
            loginRepository.checkUserIsLoggedIn()
        }
    }

    fun logOut() {
        viewModelScope.launch {
            // make sure we close any previous ws connection that belonged to previous client
            //_websocketClient.close()
            websocketManager.disconnect()

            loginRepository.logout()
            _userLogout.postValue(true)
        }
    }

    /**
     * If user was in a session, this method fully cleans up his connection. (In main activity it is called when the app is closed)
     */
    fun cleanUpSessionConnection() {
        viewModelScope.launch {
            _sessionUUID.value?.let {
                sessionRepository.disconnectFromSession(it)
            }
                ?:
            {
                Log.e("error", "Session uuid was null when trying to disconnect client from the session");
            }
            _safeDestroy.postValue(Unit)
        }

    }

}