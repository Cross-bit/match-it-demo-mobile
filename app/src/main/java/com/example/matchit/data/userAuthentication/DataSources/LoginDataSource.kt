package com.example.matchit.data.userAuthentication.DataSources


//import com.example.matchit.data.remote.api.AuthenticationApiService

import com.example.matchit.BuildConfig
import com.example.matchit.data.model.authentication.LoggedInUser
import com.example.matchit.data.remote.api.AccountApiService
import com.example.matchit.data.remote.model.AuthenticationApi.UserLoginRequestData
import com.example.matchit.data.remote.api.AuthenticationApiService
import com.example.matchit.data.remote.client.ApiErrors.ApiError
import com.example.matchit.data.remote.client.ApiErrors.ValidationErrorDetail
import com.example.matchit.data.remote.client.AuthenticatedApiCaller
import com.example.matchit.data.remote.client.RequestClientFactory
import com.example.matchit.data.remote.client.Resource
import com.example.matchit.data.remote.client.RetrofitSafeApiCaller
import com.example.matchit.data.remote.model.AuthenticationApi.NewTokenApiResponse
import com.example.matchit.data.remote.model.AuthenticationApi.RefreshTokenApiRequest
import com.example.matchit.data.remote.model.AuthenticationApi.UserLoggedInApiResponse
import com.example.matchit.data.remote.model.SimpleApiResponse
import javax.inject.Inject


/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
class LoginDataSource @Inject constructor(
    private val factory: RequestClientFactory,
    private val apiCaller: RetrofitSafeApiCaller,
    private val reAuthenticationHandler: AuthenticatedApiCaller
)
{
    suspend fun login(email: String, password: String, fcmToken: String): Resource<UserLoggedInApiResponse, ApiError<*>> {
        val retrofit = factory.createUnauthenticatedClient(BuildConfig.ACCOUNTS_API_URL);
        val apiService = retrofit.create(AuthenticationApiService::class.java)

        return apiCaller.safeApiCall<UserLoggedInApiResponse, Any> { apiService.loginUser(UserLoginRequestData(email, password, fcmToken)) }
    }

    suspend fun reauthenticateUser(refreshToken: String) : Resource<NewTokenApiResponse, ApiError<*>> {
        val client = factory.createUnauthenticatedClient(BuildConfig.ACCOUNTS_API_URL);
        val apiService = client.create(AuthenticationApiService::class.java)

        return apiCaller.safeApiCall<NewTokenApiResponse, Any> { apiService.refreshAccessToken(RefreshTokenApiRequest(refreshToken)) }
    }

    suspend fun logout() : Resource<SimpleApiResponse, ApiError<*>> {
        return reAuthenticationHandler.makeAuthenticatedApiCall(BuildConfig.ACCOUNTS_API_URL) {
            it.create(AccountApiService::class.java).logoutUser()
        }
    }
}