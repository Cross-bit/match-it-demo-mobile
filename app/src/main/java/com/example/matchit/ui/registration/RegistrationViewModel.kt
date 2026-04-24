package com.example.matchit.ui.registration

import android.util.Log
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.matchit.R
import com.example.matchit.data.remote.client.ApiErrors.ValidationCode
import com.example.matchit.data.remote.client.ApiErrors.ValidationErrorDetail
import com.example.matchit.data.remote.client.ApiErrors.ValidationErrors
import com.example.matchit.data.userAuthentication.RegistrationRepository
import com.example.matchit.data.remote.client.Resource
import com.example.matchit.data.remote.model.AuthenticationApi.RegistrationStatus
import com.example.matchit.data.userAuthentication.SignupError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegistrationViewModel @Inject
constructor(private val signupRepository: RegistrationRepository ) : ViewModel()
{

    private val _signupForm = MutableLiveData<SignupFormState>()
    val signupFormState: LiveData<SignupFormState> = _signupForm

    private val _signupResult = MutableLiveData<RegistrationResult>()
    val signupResult: LiveData<RegistrationResult> = _signupResult

    fun signupWithCredentials(username: String, email: String, password: String) {
        viewModelScope.launch {
            when (val result = signupRepository.signup(username, email, password)) {
                is Resource.Success ->
                    onRegistrationSuccess(result.data)
                is Resource.Error ->
                    handleSignupError(result.error)
                is Resource.GeneralError ->
                    _signupResult.value = RegistrationResult(success = R.string.signup_failed)
            }
        }
    }

    fun aggregateErrors(errors: List<ValidationErrorDetail>) : SignupFormState {
        var emailError: Int? = null
        var passwordError: Int? = null
        var usernameError: Int? = null

        // !!!ATTENTION give field names are supposed to match
        // the send DTO of the API (UserRegistrationData obj)

        errors.forEach { error ->
            when (error.field) {
                "password" -> passwordError = error.code.toStringRes()
                "email" -> emailError = error.code.toStringRes()
                "name" -> usernameError = error.code.toStringRes()
            }
        }

        return SignupFormState(
                emailError,
                usernameError,
                passwordError
        )
    }

    fun handleSignupError(error: SignupError) {
        when(error) {
            SignupError.AlreadyExists ->
                _signupForm.value = SignupFormState(emailError = R.string.user_already_exists)
            is SignupError.Network ->
                _signupResult.value = RegistrationResult(error = R.string.connection_error_2)
            is SignupError.Validation ->{
                val newFormState = aggregateErrors(error.errors)
                _signupForm.value = newFormState
            }
        }
    }

    fun signupDataChanged(username: String, mail: String, password: String) {
        if (!isUsernameValid(username)) {
            _signupForm.value = SignupFormState(usernameError = R.string.invalid_username_length)
        } else if (!isEmailValid(mail)) {
            _signupForm.value = SignupFormState(emailError = R.string.invalid_email_general)
        } else if (!isPasswordValid(password)) {
            _signupForm.value = SignupFormState(passwordError = R.string.invalid_password_length)
        } else {
            _signupForm.value = SignupFormState(isDataValid = true)
        }

        //_signupForm.value = SignupFormState(usernameError=usernameError, emailError = emailError, passwordError = passwordError)

    }
    private fun isEmailValid(email: String): Boolean {
        return if (email.contains('@')) {
            Patterns.EMAIL_ADDRESS.matcher(email).matches()
        } else {
            email.isNotBlank()
        }
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.length > 5
    }

    private fun isUsernameValid(password: String): Boolean {
        return password.length >= 3
    }

    private fun onRegistrationSuccess(result: RegistrationStatus) {
        when(result) {
            RegistrationStatus.CREATED ->
                _signupResult.value = RegistrationResult(success = R.string.signup_successful)
            RegistrationStatus.VERIFICATION_MAIL_SEND ->
                _signupResult.value = RegistrationResult(success = R.string.signup_verification_mail_send)
            RegistrationStatus.ALREADY_EXIST ->
                _signupResult.value = RegistrationResult(error = R.string.signup_failed_already_exist)
            else -> _signupResult.value = RegistrationResult(success = R.string.signup_failed)
        }
    }

}