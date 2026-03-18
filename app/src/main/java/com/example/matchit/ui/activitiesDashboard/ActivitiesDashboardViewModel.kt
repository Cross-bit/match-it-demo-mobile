package com.example.matchit.ui.activitiesDashboard

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.matchit.data.model.session.SessionType
import com.example.matchit.data.remote.client.Resource
import com.example.matchit.data.session.SessionRepository
import com.example.matchit.data.userAccount.UserAccountRepository
import com.example.matchit.data.userAuthentication.LoginRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActivitiesDashboardViewModel @Inject constructor(
    loginRepository: LoginRepository,
    private val accountRepository: UserAccountRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _profilePictureUrl = MutableLiveData<String>()
    val profilePictureUrl: LiveData<String> = _profilePictureUrl

    val userName: LiveData<String> = loginRepository.user
                    .filterNotNull()
                    .map { it.displayName }
                    .asLiveData()

    fun openSessionInvitationCreation(sessionType: SessionType) {
        sessionRepository.setLastSessionType(sessionType);
    }

    fun updateUserProfilePic() {
        viewModelScope.launch {
            when(val profilePic = accountRepository.getUserProfilePictureUrl()){
                is Resource.Success -> {
                    val url = profilePic.data
                    _profilePictureUrl.value = url
                }
                else -> {
                    _profilePictureUrl.value = ""
                    Log.e("profilePicError", "Missing profile picture.")
                }
            }
        }
    }
}