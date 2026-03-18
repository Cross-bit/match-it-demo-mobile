package com.example.matchit.data.userAuthentication

import com.example.matchit.data.model.authentication.GoogleAuthRequest
import com.example.matchit.data.userAuthentication.DataSources.RegistrationDataSource
import com.example.matchit.data.model.authentication.UserRegistrationData
import com.example.matchit.data.remote.client.ApiErrors.ApiError
import com.example.matchit.data.remote.client.ApiErrors.ValidationErrorDetail
import com.example.matchit.data.remote.client.Resource
import com.example.matchit.data.remote.model.AuthenticationApi.RegistrationStatus
import com.example.matchit.data.remote.model.AuthenticationApi.UserRegistrationApiResponse
import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

sealed class SignupError {
    data class Validation(val errors: List<ValidationErrorDetail>) : SignupError()
    data class Network(val message: String? = null) : SignupError()
    object AlreadyExists : SignupError()
}

class RegistrationRepository @Inject constructor(
    private val dataSource: RegistrationDataSource)
{
    suspend fun signup(username: String, email: String, password: String) : Resource<RegistrationStatus, SignupError> {

        val fcmDeviceToken = Firebase.messaging.token.await()

        // handle registration
        val result: Resource<UserRegistrationApiResponse,  ApiError<List<ValidationErrorDetail>>> = dataSource.signupCredentials(
            UserRegistrationData(username, email, password, fcmDeviceToken)
        )

        return when (result) {
            is Resource.Success -> {
                Resource.Success(result.data.status)
            }

            is Resource.Error -> {
                val apiError = result.error

                val signupError = when {
                    apiError.name == "VALIDATION_ERROR" -> {
                        val validationErrors =
                            (apiError.errors as? List<ValidationErrorDetail>)
                                ?: emptyList()
                        SignupError.Validation(validationErrors)
                    }

                    apiError.status == 409 -> {
                        SignupError.AlreadyExists
                    }

                    else -> {
                        SignupError.Network("HTTP ${apiError.status}")
                    }
                }

                Resource.Error(
                    errorMessage = "Signup failed",
                    error = signupError
                )
            }

            is Resource.GeneralError -> {
                Resource.Error(
                    errorMessage = result.message,
                    error = SignupError.Network(result.message)
                )
            }
        }
    }

    suspend fun signup(googleIdToken: String) : Resource<RegistrationStatus, ApiError<*>> {

        val fcmDeviceToken = Firebase.messaging.token.await()

        // handle registration
        val result: Resource<UserRegistrationApiResponse, ApiError<*>> = dataSource.signupGoogleAuthentication(
            GoogleAuthRequest(googleIdToken)
        )

        return  when (result) {
            is Resource.Success -> Resource.Success(result.data.status)
            is Resource.GeneralError -> Resource.GeneralError(result.message, result.throwable)
            is Resource.Error -> Resource.Error(result.errorMessage, result.error)
            else -> throw Error("User singup failed");
        }
    }




}