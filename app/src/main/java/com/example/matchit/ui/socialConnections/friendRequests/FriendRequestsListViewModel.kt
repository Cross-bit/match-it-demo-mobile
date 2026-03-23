package com.example.matchit.ui.socialConnections.friendRequests

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.matchit.R
import com.example.matchit.data.friendships.FriendsRepositoryImpl
import com.example.matchit.data.notifications.pushNotifications.NotificationDispatcher
import com.example.matchit.data.users.UsersRepository
import com.example.matchit.data.remote.client.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendRequestsListViewModel @Inject constructor(
    private val friendsRepository: FriendsRepositoryImpl,
    private val notificationDispatcher: NotificationDispatcher,
    private val usersRepository: UsersRepository
) : ViewModel() {

    private val _allRequestData = MutableLiveData<List<RequestItem>>()
    val allRequestData: LiveData<List<RequestItem>> = _allRequestData

    val admitFriendRequestStatus: LiveData<String>
        get() = _admitFriendRequestStatus
    private val _admitFriendRequestStatus = MutableLiveData<String>()

    fun updateAllFriendRequests() {
        viewModelScope.launch {
            when (val result = friendsRepository.getAllFriendshipCreationRequests()) {
                is Resource.Success -> {
                    val allRequestData: List<RequestItem> = result.data.requests.map { request ->
                        val avatarUrl = when (val userResult = usersRepository.fetchUserData(request.friendData.uuid)) {
                            is Resource.Success -> userResult.data.avatarUrl
                            else -> null
                        }
                        RequestItem(
                            request.requestId,
                            RequestPersonData(
                                avatarUrl,
                                request.friendData.name,
                                request.friendData.email
                            )
                        )
                    }
                    _allRequestData.value = allRequestData
                }
                else -> {
                    Log.d("friendsUpdate", "Something went wrong during friends update.")
                }
            }
        }

        notificationDispatcher.invalidateFriendshipInvite()
    }

    fun admitFriendRequest(requestId: String) {
        viewModelScope.launch {
            when (friendsRepository.admitFriendshipCreationRequest(requestId)) {
                is Resource.Success -> {
                    _admitFriendRequestStatus.value = "Friend request admitted successfully"
                }
                is Resource.Error -> {
                    _admitFriendRequestStatus.value = "Failed to admit friend request"
                }
                else -> {}
            }
        }
    }
}