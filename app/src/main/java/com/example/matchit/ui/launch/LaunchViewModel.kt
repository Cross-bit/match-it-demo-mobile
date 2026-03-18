package com.example.matchit.ui.launch

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.matchit.data.userAuthentication.LoginRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class LaunchViewModel @Inject constructor(
    private val loginRepository: LoginRepository) : ViewModel()
{

    private val _authenticationResult = MutableLiveData<Boolean>()
    val authenticationResult: LiveData<Boolean> = _authenticationResult

    fun tryAuthenticateUser()
    {
        viewModelScope.launch {
            _authenticationResult.value = loginRepository.checkUserIsLoggedIn()
        }
    }

}