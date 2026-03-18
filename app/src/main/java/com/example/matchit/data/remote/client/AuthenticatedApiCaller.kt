package com.example.matchit.data.remote.client

import com.example.matchit.BuildConfig
import com.example.matchit.data.remote.Errors.UserIsLoggedOutError
import com.example.matchit.data.remote.api.AuthenticationApiService
import com.example.matchit.data.remote.client.ApiErrors.ApiError
import com.example.matchit.data.remote.model.AuthenticationApi.NewTokenApiResponse
import com.example.matchit.data.remote.model.AuthenticationApi.RefreshTokenApiRequest
import com.google.gson.Gson
import retrofit2.Response
import retrofit2.Retrofit
import javax.inject.Inject

class AuthenticatedApiCaller @Inject constructor(
    private val loginSessionManager: LoginSessionManager,
    private val apiCaller: RetrofitSafeApiCaller,
    private val requestClientFactory: RequestClientFactory
)  {
    companion object {
        const val ACCESS_TOKEN_EXPIRED = "ACCESS_TOKEN_EXPIRED"
        const val INVALID_REFRESH_TOKEN = "INVALID_REFRESH_TOKEN"
    }

    // for thread safety we need to perform check whether we already tried to reathenticate or not (also it would be nic e)
    private class AlreadyTriedTracker(var value: Int)

    private val _maxNumberOfRetries = 1; // we allow 1 another attempt at most (the attempt after refresh request)

    private suspend fun tryToRefreshAccessToken(): Resource<NewTokenApiResponse, ApiError<*>> {

        val client = requestClientFactory.createUnauthenticatedClient(BuildConfig.ACCOUNTS_API_URL);
        val apiService = client.create(AuthenticationApiService::class.java)

        val refreshToken =
            loginSessionManager.getRefreshToken() ?: throw UserIsLoggedOutError();


        return apiCaller.safeApiCall<NewTokenApiResponse, Any> { apiService.refreshAccessToken(RefreshTokenApiRequest(refreshToken)) }
    }

    private suspend fun<T : Any> recreateOriginalRequest(apiBaseUrl: String, apiToBeCalled: suspend (client: Retrofit) -> Response<T>, attemptsTracker: AlreadyTriedTracker) : Resource<T, ApiError<*>> {

        if (attemptsTracker.value == _maxNumberOfRetries) {
            // we failed even after the re-authentication attempt... we fail completely...
            loginSessionManager.cleanTokens()
            throw UserIsLoggedOutError()
        }

        attemptsTracker.value++ // update "how many times we tried"

        return makeAuthenticatedApiCall(apiBaseUrl, apiToBeCalled = apiToBeCalled)
    }

    private suspend fun<T : Any> handleApiError(apiBaseUrl: String, response: Resource.Error<ApiError<*>>, apiToBeCalled: suspend (client: Retrofit) -> Response<T>, attemptsTracker: AlreadyTriedTracker) : Resource<T, ApiError<*>> {
        return response.error.name.let {

            // if the error was really the expiration error, we can try to re-log-in to the system
            if (it == ACCESS_TOKEN_EXPIRED) {

                when (val refreshResult: Resource<NewTokenApiResponse, ApiError<*>> = tryToRefreshAccessToken())
                {
                    is Resource.Success -> {
                        // we successfully authenticated, we retry the original query

                        // If all reauthentication attempts failed... we return original response...
                        return try {
                            loginSessionManager.saveAuthToken(refreshResult.data.updatedToken)
                             recreateOriginalRequest(apiBaseUrl, apiToBeCalled, attemptsTracker)
                        }
                        catch(e: UserIsLoggedOutError) {
                            response
                        }

                    }
                    is Resource.Error -> {
                        if(refreshResult.error.name == INVALID_REFRESH_TOKEN) { //refreshResult.error != null &&
                            // make sure tokens are really cleared

                            loginSessionManager.cleanTokens()
                            throw UserIsLoggedOutError()
                        }

                        throw AssertionError()
                    }
                    is Resource.GeneralError -> {
                        // some other error during authentication (e.g. connection errors)

                        loginSessionManager.cleanTokens()
                        throw UserIsLoggedOutError(cause = refreshResult.throwable)
                    }
                    else -> throw AssertionError()
                }
            }
            else {
                // There was some other problem then authentication => we return it to the client
                response
            }
        }
    }


    /**
     * Makes authenticated api call with internally stored access token. If authentication fails, tries to refresh the token using refresh token.
     */
    suspend fun <T : Any>makeAuthenticatedApiCall(apiBaseUrl: String, customGsonParser: Gson = Gson(), apiToBeCalled: suspend (client: Retrofit) -> Response<T>): Resource<T, ApiError<*>> {
        return makeAuthenticatedApiCall(apiBaseUrl, customGsonParser, apiToBeCalled, AlreadyTriedTracker(0)) // at the beginning we set the tracker for this call to 0
    }

    /**
     * Private implementation that
     */
    private suspend fun <T : Any>makeAuthenticatedApiCall(apiBaseUrl: String, customGsonParser: Gson = Gson(), apiToBeCalled: suspend (client: Retrofit) -> Response<T>, attemptsTracker: AlreadyTriedTracker): Resource<T, ApiError<*>> {

        // we try to make original request
        val retrofit = requestClientFactory.createAuthenticatedClient(apiBaseUrl, gsonParser = customGsonParser); // NOTE: we always rebuild the retrofit client so it e.g. reloads the token if updated... etc...
        val response: Resource<T, ApiError<*>> = apiCaller.safeApiCall(apiBaseUrl) { apiToBeCalled(retrofit) }

        return if (response is Resource.Error) {
            return handleApiError(apiBaseUrl, response, apiToBeCalled, attemptsTracker);
        } else {
            // the call including authentication went as expected, we just return the response
            response
        }
    }

}