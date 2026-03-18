package com.example.matchit.ui.socialConnections

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.matchit.R
import com.example.matchit.data.friendships.FriendsRepositoryImpl
import com.example.matchit.data.remote.client.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendsListViewModel @Inject constructor(
    private val friendsRepository: FriendsRepositoryImpl
) : ViewModel() {

    private val _allFriendsData = MutableLiveData<List<FriendItem>>()
    val allFriendsData: LiveData<List<FriendItem>> = _allFriendsData

    init {
        updateAllFriends()
    }

    private fun updateAllFriends() {
        viewModelScope.launch {
            when(val result = friendsRepository.getAllFriends()) {
                is Resource.Success -> {
                    result.data.friends.let { friends ->
                        val friendsData: List<FriendItem> = friends.map { FriendItem(R.drawable.ic_person, it.name, it.email, it.profilePicUrl) }

                        _allFriendsData.postValue(friendsData)
                    }
                }
                else -> {}
            }
        }
    }
}