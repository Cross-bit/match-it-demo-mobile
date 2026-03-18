package com.example.matchit.data.userAuthentication

import android.util.Log
import com.example.matchit.data.local.db.entities.UserEntity
import com.example.matchit.data.userAuthentication.DataSources.LoginDataSource
import com.example.matchit.data.model.authentication.LoggedInUser
import com.example.matchit.data.remote.client.ApiErrors.ApiError
import com.example.matchit.data.remote.client.ApiErrors.ValidationErrorDetail
import com.example.matchit.data.remote.client.LoginSessionManager
import com.example.matchit.data.remote.client.Resource
import com.example.matchit.data.remote.model.AuthenticationApi.UserLoggedInApiResponse
import com.example.matchit.data.remote.model.AuthenticationApi.toExternal
import com.example.matchit.data.remote.model.SimpleApiResponse
import com.example.matchit.data.userAuthentication.DataSources.LocalSessionCleanup
import com.example.matchit.data.userAuthentication.DataSources.UserDataLocalDataSource
import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

sealed class LoginError {
    data class Success(val user: LoggedInUser) : LoginError()
    object InvalidPassword : LoginError()
    object AlreadyLoggedIn : LoginError()
    object NotVerified : LoginError()
    object NotExist : LoginError()
    data class Validation(val errors: List<ValidationErrorDetail>) : LoginError()
    data class NetworkError(val message: String? = null) : LoginError()
}

/**
 * Class that requests authentication and user information from the remote data source and
 * maintains an in-memory cache of login status and user credentials information.
 */

class LoginRepository @Inject constructor(
    private val dataSource: LoginDataSource,
    private var dbDataSource: UserDataLocalDataSource,
    private val loginSessionManager: LoginSessionManager,
    private val localSessionCleanup: LocalSessionCleanup
) {

    val user: Flow<LoggedInUser?>
        get() = _user

    private val _user = MutableStateFlow<LoggedInUser?>(null)


    suspend fun logout(): Resource<SimpleApiResponse, ApiError<*>> {
        _user.value = null

        // We try to logout user from the remote (best effort)
        val remoteLogoutRes = dataSource.logout()

        // Clean up the local resources (access tokens and other sensible user data)
        localSessionCleanup.clear()

        return remoteLogoutRes
    }


    suspend fun login(email: String, password: String): Resource<LoggedInUser, LoginError> {

        val fcmDeviceToken = Firebase.messaging.token.await()

        // handle login
        return when (val result: Resource<UserLoggedInApiResponse, ApiError<*>> = dataSource.login(email, password, fcmDeviceToken)) {
            is Resource.Success -> {
                val apiUser = result.data

                loginSessionManager.saveAuthToken(apiUser.accessToken);
                loginSessionManager.saveRefreshToken(apiUser.refreshToken);

                dbDataSource.deleteAll();
                dbDataSource.storeUser(UserEntity(apiUser.uuid, apiUser.name, email, apiUser.preferences.profilePictureUrl))

                val domainUser = apiUser.toExternal()
                _user.value = domainUser
                Resource.Success(domainUser)
            }
            is Resource.Error -> {
                val apiError = result.error
                val loginError = when {
                    apiError.name == "VALIDATION_ERROR" -> {
                        val validationErrors =
                            (apiError.errors as? List<ValidationErrorDetail>) ?: emptyList()
                        LoginError.Validation(validationErrors)
                    }

                    apiError.status == 401 -> LoginError.InvalidPassword
                    apiError.status == 403 -> LoginError.NotVerified
                    apiError.status == 409 -> LoginError.AlreadyLoggedIn
                    apiError.status == 404 -> LoginError.NotExist

                    else -> LoginError.NetworkError("HTTP ${apiError.status}")
                }
                Resource.Error(
                    errorMessage = "Error during login",
                    error = loginError
                )
            }
            is Resource.GeneralError -> {
                Resource.Error(
                    errorMessage = "Network / infrastructure error: ${result.message}",
                    error = LoginError.NetworkError(result.message)
                )
            }
        }
    }


    suspend fun checkUserIsLoggedIn(): Boolean
    {
        val refreshToken: String = loginSessionManager.getRefreshToken() ?: return false

        return when (val result = dataSource.reauthenticateUser(refreshToken)) {
            is Resource.Success -> {
                val newAccessToken = result.data.updatedToken
                loginSessionManager.saveAuthToken(newAccessToken)

                return dbDataSource.getCurrentUser()?.let { userEntity ->
                    _user.value = LoggedInUser(userEntity.displayName, userEntity.email, userEntity.uuid)
                    true
                } ?: false
            }
            is Resource.Error -> {
                // todo add log refresh token could expire ...
                Log.d("Api error:", result.errorMessage);
                return false
            }
            is Resource.GeneralError -> {
                // todo add log some general error
                return false
            }
            else -> throw AssertionError()
        }
    }

}