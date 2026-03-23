package com.example.matchit.ui.socialConnections.peopleSearch

import android.util.Log
import androidx.lifecycle.*
import com.example.matchit.R
import com.example.matchit.data.remote.client.ApiErrors.ApiError
import com.example.matchit.data.remote.client.Resource
import com.example.matchit.data.search.SearchPersonRepositoryImpl
import com.example.matchit.data.users.UsersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchPeopleViewModel @Inject constructor(
    private val searchPersonRepository: SearchPersonRepositoryImpl,
    private val usersRepository: UsersRepository
) : ViewModel() {

    private val _searchResult = MutableLiveData<SearchResult>()
    val searchResult: LiveData<SearchResult> = _searchResult

    val friendRequestStatus: LiveData<Int>
        get() = _friendRequestStatus

    private val _friendRequestStatus = MutableLiveData<Int>()

    fun updateSearchPersonByEmail(email: String) {
        viewModelScope.launch {
            when (val result = searchPersonRepository.searchPerson(email)) {
                is Resource.Success -> {
                    val person = result.data.person

                    val avatarUrl = when (val userResult = usersRepository.fetchUserData(person.uuid)) {
                        is Resource.Success -> userResult.data.avatarUrl
                        else -> null
                    }.toString()

                    val personItem = SearchPersonItem(
                        person.uuid,
                        avatarUrl,
                        person.name,
                        person.isFriend,
                        person.hasInvitation
                    )
                    _searchResult.value = SearchResult(personItem, null)
                }

                is Resource.Error -> {
                    val error = result.error
                    if (error is ApiError<*> && error.status == 404) {
                        _searchResult.value = SearchResult(null, R.string.person_not_found_message)
                    } else {
                        _searchResult.value = SearchResult(null, R.string.search_failed)
                    }
                }

                is Resource.GeneralError -> {
                    _friendRequestStatus.value = R.string.connection_error
                    Log.e("error", result.message, result.throwable)
                }
                else -> { }
            }
        }
    }

    fun sendFriendRequest(userId: String) {
        viewModelScope.launch {
            when (searchPersonRepository.sendFriendRequest(userId)) {
                is Resource.Success -> {
                    _friendRequestStatus.value = R.string.successfully_send_request
                }
                is Resource.Error -> {
                    _friendRequestStatus.value = R.string.failed_to_send_request
                }
                is Resource.GeneralError -> {
                    _friendRequestStatus.value = R.string.connection_error
                }
            }
        }
    }
}