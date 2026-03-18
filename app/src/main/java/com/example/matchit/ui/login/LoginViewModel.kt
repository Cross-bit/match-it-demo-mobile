package com.example.matchit.ui.login

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.matchit.R
import com.example.matchit.data.userAuthentication.LoginRepository
import com.example.matchit.data.remote.client.Resource
import com.example.matchit.data.userAuthentication.LoginError

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject
    constructor(
        private val loginRepository: LoginRepository
    ) : ViewModel() {

    private val _loginForm = MutableLiveData<LoginFormState>()
    val loginFormState: LiveData<LoginFormState> = _loginForm

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult


    fun login(username: String, password: String) {
        viewModelScope.launch {

            when (val result = loginRepository.login(username, password)) {
                is Resource.Success -> {
                    _loginResult.value = LoginResult(
                        success = LoggedInUserView(displayName = result.data.displayName)
                    )
                }
                is Resource.Error -> {
                    val errorRes = when (result.error) {
                        LoginError.InvalidPassword ->
                            R.string.login_failed_invalid_password
                        LoginError.AlreadyLoggedIn ->
                            R.string.login_failed_already_logged_in
                        LoginError.NotVerified ->
                            R.string.login_failed_not_verified
                        LoginError.NotExist ->
                            R.string.login_failed_not_exist
                        is LoginError.NetworkError ->
                            R.string.connection_error
                        is LoginError.Validation ->
                            R.string.invalid_credentials_error
                        else ->
                            R.string.login_failed_general
                    }

                    _loginResult.value = LoginResult(error = errorRes)
                }
                is Resource.GeneralError ->
                    _loginResult.value = LoginResult(error = R.string.connection_error)
            }
        }
    }

    fun loginDataChanged(username: String, password: String) {
        if (!isEmailValid(username)) {
            _loginForm.value = LoginFormState(usernameError = R.string.invalid_email_general)
        } else if (!isPasswordValid(password)) {
            _loginForm.value = LoginFormState(passwordError = R.string.invalid_password_length)
        } else {
            _loginForm.value = LoginFormState(isDataValid = true)
        }
    }

    // A placeholder username validation check
    private fun isEmailValid(email: String): Boolean {
        return if (email.contains('@')) {
            Patterns.EMAIL_ADDRESS.matcher(email).matches()
        } else {
            email.isNotBlank()
        }
    }

    // A placeholder password validation check
    private fun isPasswordValid(password: String): Boolean {
        return password.length > 5
    }
}